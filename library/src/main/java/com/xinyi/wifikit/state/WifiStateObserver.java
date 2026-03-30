package com.xinyi.wifikit.state;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.ConnectivityManager;

import com.xinyi.device.DeviceContext;

/**
 * Wi-Fi 状态监听器
 *
 * <p> 用于监听 Wi-Fi 打开/关闭、连接状态变化 </p>
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
     * Wi-Fi 状态监听的广播接收器
     */
    private final BroadcastReceiver mReceiver;

    public WifiStateObserver() {
        this.mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                    // Wi-Fi 状态发生变化
                    int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                    if (mStateCallback != null) {
                        mStateCallback.onWifiStateChanged(state);
                    }
                } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                    // Wi-Fi 连接状态
                    NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                    if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI) {
                        if (mStateCallback != null) {
                            if (info.isConnected()) {
                                mStateCallback.onWifiConnected();
                            } else {
                                mStateCallback.onWifiDisconnected();
                            }
                        }
                    }
                }
            }
        };
    }

    /**
     * 开始监听 Wi-Fi 状态
     *
     * @param callback 回调接口
     */
    public void start(WifiStateCallback callback) {
        this.mStateCallback = callback;
        IntentFilter filter = new IntentFilter();
        // Wi-Fi 状态变化
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        // Wi-Fi 连接状态变化
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        // 注册广播接收器
        DeviceContext.getApplication().registerReceiver(mReceiver, filter);
    }

    /**
     * 停止监听
     */
    public void stop() {
        try {
            DeviceContext.getApplication().unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException exception) {
            exception.printStackTrace();
        }
        mStateCallback = null;
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
         */
        void onWifiConnected();

        /**
         * Wi-Fi 断开连接
         */
        void onWifiDisconnected();
    }
}