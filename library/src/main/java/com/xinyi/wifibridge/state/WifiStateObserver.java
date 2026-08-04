package com.xinyi.wifibridge.state;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.xinyi.device.DeviceContext;
import com.xinyi.wifibridge.info.WifiConnectionInfo;
import com.xinyi.wifibridge.info.WifiInfoHelper;

/**
 * Wi-Fi 状态监听器
 *
 * <p> 用于监听 Wi-Fi 打开/关闭、连接状态变化 </p>
 *
 * <p> Android 5.0+ 使用 {@link ConnectivityManager.NetworkCallback} 监听 Wi-Fi 连接 </p>
 * <p> Android 5.0 以下回退到 {@link ConnectivityManager#CONNECTIVITY_ACTION} 广播 </p>
 *
 * @author 新一
 * @date 2025/5/29 14:11
 */
public class WifiStateObserver {

    /**
     * Wi-Fi 状态监听回调
     */
    private WifiStateCallback mStateCallback;

    /**
     * 最近一次已连接的 Wi-Fi 信息快照（断开时带回给调用方）
     */
    private WifiConnectionInfo mLastConnectedInfo;

    /**
     * 主线程 Handler
     */
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * Wi-Fi 开关状态广播接收器
     */
    private final BroadcastReceiver mWifiStateReceiver;

    /**
     * Android 5.0 以下的连接状态广播接收器
     */
    private BroadcastReceiver mLegacyConnectivityReceiver;

    /**
     * Android 5.0+ 的网络回调
     */
    private ConnectivityManager.NetworkCallback mNetworkCallback;

    /**
     * 是否已注册监听
     */
    private boolean mRegistered;

    /**
     * 构造函数
     */
    public WifiStateObserver() {
        this.mWifiStateReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (!WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                    return;
                }

                // Wi-Fi 状态发生变化
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                if (mStateCallback != null) {
                    mStateCallback.onWifiStateChanged(state);
                }
            }
        };
    }

    /**
     * 注册 Wi-Fi 状态监听
     *
     * @param callback 回调接口
     */
    public void register(WifiStateCallback callback) {
        if (mRegistered) {
            unregister();
        }
        this.mStateCallback = callback;
        this.mRegistered = true;

        // 注册 Wi-Fi 开关状态监听
        IntentFilter wifiStateFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiverCompat(mWifiStateReceiver, wifiStateFilter);

        // 注册 Wi-Fi 连接状态监听
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            registerNetworkCallback();
        } else {
            registerLegacyConnectivityReceiver();
        }
    }

    /**
     * Android 5.0+ 使用 NetworkCallback 监听 Wi-Fi 连接状态
     *
     * <p> 这里按 TRANSPORT_WIFI 过滤，而不是直接复用 {@code NetworkStatusMonitor}：
     * 后者面向通用上网网络（INTERNET），会漏掉仅局域网的 Wi-Fi。 </p>
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
                mHandler.post(() -> notifyConnected(resolveWifiInfo(network)));
            }

            @Override
            public void onLost(@NonNull Network network) {
                mHandler.post(() -> notifyDisconnected());
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities capabilities) {
                // Android 10+ 可从 TransportInfo 拿到更准确的 WifiInfo，用于刷新断开前快照
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiInfo wifiInfo = extractWifiInfo(capabilities);
                    if (wifiInfo != null) {
                        WifiConnectionInfo info = WifiConnectionInfo.from(wifiInfo);
                        if (info.getSsid() != null) {
                            mLastConnectedInfo = info;
                        }
                    }
                }
            }
        };
        DeviceContext.getConnectivityManager().registerNetworkCallback(request, mNetworkCallback);
    }

    /**
     * Android 5.0 以下回退到连接状态广播
     */
    private void registerLegacyConnectivityReceiver() {
        mLegacyConnectivityReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (!ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                    return;
                }

                // 判断是否已连上 Wi-Fi
                if (WifiInfoHelper.isWifiConnected()) {
                    notifyConnected(resolveWifiInfo(null));
                } else if (mLastConnectedInfo != null) {
                    notifyDisconnected();
                }
            }
        };
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiverCompat(mLegacyConnectivityReceiver, filter);
    }

    /**
     * 兼容 Android 13+ 的广播注册方式
     *
     * @param receiver 广播接收器
     * @param filter   过滤器
     */
    private void registerReceiverCompat(BroadcastReceiver receiver, IntentFilter filter) {
        Context context = DeviceContext.getApplication();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        }
    }

    /**
     * 解析当前 Wi-Fi 信息
     *
     * <p> Android 10+ 优先从 NetworkCapabilities 的 TransportInfo 读取；否则回退 WifiManager </p>
     *
     * @param network 网络，可为 null
     */
    @SuppressWarnings("deprecation")
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
     * 通知已连接到 Wi-Fi
     *
     * @param wifiInfo 当前连接的 Wi-Fi 信息
     */
    private void notifyConnected(@Nullable WifiInfo wifiInfo) {
        if (wifiInfo == null) {
            return;
        }
        WifiConnectionInfo info = WifiConnectionInfo.from(wifiInfo);
        // 若系统尚未给出有效 SSID，再确认一次是否真的已连接
        if (info.getSsid() == null && !WifiInfoHelper.isWifiConnected()) {
            return;
        }
        mLastConnectedInfo = info;
        if (mStateCallback != null) {
            mStateCallback.onWifiConnected(info);
        }
    }

    /**
     * 通知 Wi-Fi 已断开
     */
    private void notifyDisconnected() {
        WifiConnectionInfo previous = mLastConnectedInfo;
        mLastConnectedInfo = null;
        if (mStateCallback != null) {
            mStateCallback.onWifiDisconnected(previous);
        }
    }

    /**
     * 注销 Wi-Fi 状态监听
     */
    public void unregister() {
        Context context = DeviceContext.getApplication();
        try {
            context.unregisterReceiver(mWifiStateReceiver);
        } catch (IllegalArgumentException exception) {
            exception.printStackTrace(System.err);
        }

        if (mLegacyConnectivityReceiver != null) {
            try {
                context.unregisterReceiver(mLegacyConnectivityReceiver);
            } catch (IllegalArgumentException exception) {
                exception.printStackTrace(System.err);
            }
            mLegacyConnectivityReceiver = null;
        }

        if (mNetworkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                DeviceContext.getConnectivityManager().unregisterNetworkCallback(mNetworkCallback);
            } catch (Exception exception) {
                exception.printStackTrace(System.err);
            }
            mNetworkCallback = null;
        }

        mHandler.removeCallbacksAndMessages(null);
        mStateCallback = null;
        mLastConnectedInfo = null;
        mRegistered = false;
    }

    /**
     * Wi-Fi 状态监听回调
     */
    public interface WifiStateCallback {

        /**
         * Wi-Fi 模块状态变化（开启/关闭）
         *
         * @param state 见 {@link WifiManager#WIFI_STATE_ENABLED} 等常量
         */
        void onWifiStateChanged(int state);

        /**
         * 成功连接到 Wi-Fi
         *
         * @param info 当前连接的 Wi-Fi 信息，获取失败时可能为 null
         */
        void onWifiConnected(@Nullable WifiConnectionInfo info);

        /**
         * Wi-Fi 断开连接
         *
         * @param info 断开前的 Wi-Fi 信息，未知时可能为 null
         */
        void onWifiDisconnected(@Nullable WifiConnectionInfo info);
    }
}