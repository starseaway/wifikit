package com.xinyi.wifikit;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

import com.xinyi.device.DeviceContext;

/**
 * WiFiKit 核心入口类
 *
 * <p> 用于初始化 WiFiKit，并提供全局系统服务访问能力 </p>
 *
 * @author 新一
 * @date 2025/5/29 14:25
 */
public class WiFiKit {

    /**
     * 系统的 Wi-Fi 管理器
     */
    private static WifiManager mWifiManager;

    /**
     * 初始化全局上下文
     *
     * @param mainContext 上下文
     */
    public static void init(Context mainContext) {
        DeviceContext.init(mainContext);
    }

    /**
     * 获取全局上下文
     *
     * @return 全局上下文
     */
    public static Application getApplication() {
        return DeviceContext.getApplication();
    }

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