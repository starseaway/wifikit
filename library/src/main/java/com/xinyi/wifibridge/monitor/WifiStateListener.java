package com.xinyi.wifibridge.monitor;

import androidx.annotation.NonNull;

/**
 * Wi-Fi 模块开关状态监听回调
 *
 * @author 新一
 * @date 2026/8/12 11:46
 */
public interface WifiStateListener {

    /**
     * Wi-Fi 模块开关状态变化
     *
     * @param state 最新状态
     */
    void onWifiStateChanged(@NonNull WifiAdapterState state);
}