package com.xinyi.wifibridge.monitor;

import androidx.annotation.Nullable;

import com.xinyi.wifibridge.info.WifiConnectionInfo;

/**
 * Wi-Fi 连接状态监听回调
 *
 * @author 新一
 * @date 2026/8/12 11:58
 */
public interface WifiConnectionListener {

    /**
     * 连接状态发生变化
     *
     * @param connected 当前是否已连接 Wi-Fi
     * @param info 当前连接信息；未连接时为 null
     */
    void onWifiConnectionChanged(boolean connected, @Nullable WifiConnectionInfo info);
}