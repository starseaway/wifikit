package com.xinyi.wifibridge.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.xinyi.device.DeviceContext;

/**
 * Wi-Fi 模块开关状态监听器
 *
 * @author 新一
 * @date 2026/8/12 11:40
 */
public final class WifiStateMonitor {

    /**
     * 对外状态回调
     */
    private final WifiStateListener mListener;

    /**
     * 系统广播接收器；未注册时为 null
     */
    private BroadcastReceiver mReceiver;

    /**
     * 构造函数
     *
     * @param listener 状态回调
     */
    public WifiStateMonitor(@NonNull WifiStateListener listener) {
        this.mListener = listener;
    }

    /**
     * 注册监听
     */
    public void register() {
        if (mReceiver != null) {
            return;
        }
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || !WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                    return;
                }
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                mListener.onWifiStateChanged(WifiState.from(state));
            }
        };
        IntentFilter filter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        ContextCompat.registerReceiver(
                DeviceContext.getApplication(),
                mReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    /**
     * 取消注册
     */
    public void unregister() {
        if (mReceiver == null) {
            return;
        }
        DeviceContext.getApplication().unregisterReceiver(mReceiver);
        mReceiver = null;
    }

    /**
     * @return 是否已注册
     */
    public boolean isRegistered() {
        return mReceiver != null;
    }
}