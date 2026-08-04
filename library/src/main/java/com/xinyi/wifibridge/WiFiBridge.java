package com.xinyi.wifibridge;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

import com.xinyi.device.DeviceContext;

/**
 * WiFiBridge 核心入口类
 *
 * <p> 用于初始化 WiFiBridge，并提供全局系统服务访问能力 </p>
 *
 * @author 新一
 * @date 2025/5/29 14:25
 */
public class WiFiBridge {

    /**
     * 系统的 Wi-Fi 管理器
     */
    private static WifiManager mWifiManager;

    /**
     * 获取 Wi-Fi 管理器
     */
    public static WifiManager getWifiManager() {
        if (mWifiManager == null) {
            mWifiManager = DeviceContext.getSystemService(Context.WIFI_SERVICE);
        }
        return mWifiManager;
    }

    /**
     * 获取网络连接管理器
     */
    public static ConnectivityManager getConnectivityManager() {
        return DeviceContext.getConnectivityManager();
    }
}