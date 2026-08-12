package com.xinyi.wifibridge.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.xinyi.device.DeviceContext;
import com.xinyi.wifibridge.info.WifiSnapshot;
import com.xinyi.wifibridge.info.WifiInfoHelper;

/**
 * Wi-Fi 连接状态监听器
 *
 * <p> Android 5.0+ 使用 {@link ConnectivityManager.NetworkCallback} 监听 </p>
 * <p> Android 5.0 以下回退到 {@link ConnectivityManager#CONNECTIVITY_ACTION} 广播 </p>
 *
 * @author 新一
 * @date 2026/8/12 11:50
 */
public final class WifiConnectionMonitor {

    /**
     * 对外连接回调
     */
    private final WifiConnectionListener mListener;

    /**
     * 主线程 Handler
     */
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * Android 5.0 以下广播接收器；未注册时为 null
     */
    private BroadcastReceiver mReceiver;

    /**
     * Android 5.0+ 网络回调；未注册时为 null
     */
    private ConnectivityManager.NetworkCallback mNetworkCallback;

    /**
     * 最近一次已连接信息（用于能力刷新）
     */
    private WifiSnapshot mLastConnectedInfo;

    /**
     * @param listener 连接回调
     */
    public WifiConnectionMonitor(@NonNull WifiConnectionListener listener) {
        this.mListener = listener;
    }

    /**
     * 注册监听
     */
    public void register() {
        if (isRegistered()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            registerNetworkCallback();
        } else {
            registerLegacyReceiver();
        }
    }

    /**
     * 取消注册
     */
    public void unregister() {
        if (!isRegistered()) {
            return;
        }
        mHandler.removeCallbacksAndMessages(null);

        if (mReceiver != null) {
            DeviceContext.getApplication().unregisterReceiver(mReceiver);
            mReceiver = null;
        }

        if (mNetworkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                DeviceContext.getConnectivityManager().unregisterNetworkCallback(mNetworkCallback);
            } catch (Exception exception) {
                exception.printStackTrace(System.err);
            }
            mNetworkCallback = null;
        }

        mLastConnectedInfo = null;
    }

    /**
     * @return 是否已注册
     */
    public boolean isRegistered() {
        return mReceiver != null || mNetworkCallback != null;
    }

    /**
     * Android 5.0+：按 TRANSPORT_WIFI 监听连接
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void registerNetworkCallback() {
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        mNetworkCallback = new ConnectivityManager.NetworkCallback() {

            @Override
            public void onAvailable(@NonNull Network network) {
                // 稍作延迟，等待系统把 WifiInfo 填充完整后再回调
                mHandler.post(() -> notifyConnectionChanged(true, resolveWifiInfo(network)));
            }

            @Override
            public void onLost(@NonNull Network network) {
                mHandler.post(() -> notifyConnectionChanged(false, null));
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities capabilities) {
                // Android 10+ 可从 TransportInfo 刷新连接快照
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    return;
                }
                WifiInfo wifiInfo = extractWifiInfo(capabilities);
                if (wifiInfo == null) {
                    return;
                }
                WifiSnapshot info = WifiSnapshot.from(wifiInfo);
                if (info.getSsid() != null) {
                    mLastConnectedInfo = info;
                }
            }
        };
        DeviceContext.getConnectivityManager().registerNetworkCallback(request, mNetworkCallback);
    }

    /**
     * Android 5.0 以下：回退到连接状态广播
     */
    @SuppressWarnings("deprecation")
    private void registerLegacyReceiver() {
        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || !ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                    return;
                }
                if (WifiInfoHelper.isWifiConnected()) {
                    notifyConnectionChanged(true, resolveWifiInfo(null));
                } else if (mLastConnectedInfo != null) {
                    notifyConnectionChanged(false, null);
                }
            }
        };
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        ContextCompat.registerReceiver(
                DeviceContext.getApplication(),
                mReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    /**
     * 解析当前 Wi-Fi 信息
     *
     * @param network 网络，可为 null
     */
    @Nullable
    private WifiInfo resolveWifiInfo(@Nullable Network network) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && network != null) {
            NetworkCapabilities capabilities = DeviceContext.getConnectivityManager().getNetworkCapabilities(network);
            WifiInfo wifiInfo = extractWifiInfo(capabilities);
            if (wifiInfo != null) {
                return wifiInfo;
            }
        }
        return DeviceContext.getWifiManager().getConnectionInfo();
    }

    /**
     * 从 NetworkCapabilities 中提取 WifiInfo（Android 10+）
     *
     * @param capabilities 网络能力
     */
    @Nullable
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private WifiInfo extractWifiInfo(@Nullable NetworkCapabilities capabilities) {
        if (capabilities == null) {
            return null;
        }
        if (!(capabilities.getTransportInfo() instanceof WifiInfo)) {
            return null;
        }
        return (WifiInfo) capabilities.getTransportInfo();
    }

    /**
     * 通知连接状态变化
     *
     * @param connected 是否已连接
     * @param wifiInfo 系统 Wi-Fi 信息
     */
    private void notifyConnectionChanged(boolean connected, @Nullable WifiInfo wifiInfo) {
        WifiSnapshot info = null;
        if (connected) {
            if (wifiInfo == null) {
                return;
            }
            info = WifiSnapshot.from(wifiInfo);
            // 若系统尚未给出有效 SSID，再确认一次是否真的已连接
            if (info.getSsid() == null && !WifiInfoHelper.isWifiConnected()) {
                return;
            }
            mLastConnectedInfo = info;
        } else {
            mLastConnectedInfo = null;
        }
        mListener.onWifiConnectionChanged(connected, info);
    }
}