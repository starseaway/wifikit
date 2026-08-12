package com.xinyi.wifibridge.monitor;

import android.net.wifi.WifiManager;

/**
 * Wi-Fi 开关状态
 *
 * @author 新一
 * @date 2026/8/12 11:52
 */
public enum WifiState {

    /**
     * 已关闭
     */
    OFF,

    /**
     * 正在开启
     */
    TURNING_ON,

    /**
     * 已开启
     */
    ON,

    /**
     * 正在关闭
     */
    TURNING_OFF,

    /**
     * 未知状态
     */
    UNKNOWN;

    /**
     * 将 {@link WifiManager} 状态常量映射为枚举
     *
     * @param state Wi-Fi 状态常量
     */
    public static WifiState from(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_DISABLED:
                return OFF;
            case WifiManager.WIFI_STATE_ENABLING:
                return TURNING_ON;
            case WifiManager.WIFI_STATE_ENABLED:
                return ON;
            case WifiManager.WIFI_STATE_DISABLING:
                return TURNING_OFF;
            case WifiManager.WIFI_STATE_UNKNOWN:
            default:
                return UNKNOWN;
        }
    }
}
