package com.xinyi.wifibridge.monitor;

import android.net.wifi.WifiManager;

/**
 * Wi-Fi 模块开关状态
 *
 * @author 新一
 * @date 2026/8/12 11:52
 */
public enum WifiAdapterState {

    /**
     * 已关闭
     */
    DISABLED,

    /**
     * 正在关闭
     */
    DISABLING,

    /**
     * 已开启
     */
    ENABLED,

    /**
     * 正在开启
     */
    ENABLING,

    /**
     * 未知或不可用
     */
    UNKNOWN;

    /**
     * 将 {@link WifiManager} 状态常量映射为枚举
     *
     * @param state Wi-Fi 状态常量
     */
    public static WifiAdapterState fromWifiManager(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_DISABLED:
                return DISABLED;
            case WifiManager.WIFI_STATE_DISABLING:
                return DISABLING;
            case WifiManager.WIFI_STATE_ENABLED:
                return ENABLED;
            case WifiManager.WIFI_STATE_ENABLING:
                return ENABLING;
            default:
                return UNKNOWN;
        }
    }
}