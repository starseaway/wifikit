package com.xinyi.wifikit.connect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import com.xinyi.wifikit.WiFiKit;

import java.util.List;

/**
 * Wi-Fi 连接器
 * 
 * <p> 用于对指定 Wi-Fi 网络的连接功能。支持 Android 4.4（API 19）至 Android 14（API 35） </p>
 *
 * <p> Android 10+ 使用 {@link WifiNetworkSpecifier} + {@link NetworkRequest} 连接 </p>
 * <p> Android 9 及以下使用 {@link WifiConfiguration} 接口 </p>
 *
 * @author 新一
 * @date 2025/5/29 13:20
 */
public class WifiConnector {

    /**
     * 主线程处理器
     */
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * 网络回调
     */
    private WifiNetworkCallback mNetworkCallback;

    /**
     * 尝试连接到指定 Wi-Fi 网络（自动根据系统版本选择实现）
     *
     * @param ssid Wi-Fi 名称
     * @param password Wi-Fi 密码
     * @param callback 连接结果回调
     */
    @SuppressLint("MissingPermission")
    public void connect(@NonNull String ssid, @NonNull String password, @NonNull ConnectCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectAndroid10Plus(ssid, password, callback);
        } else {
            connectLegacy(ssid, password, callback);
        }
    }

    /**
     * Android 10 及以上版本使用 NetworkSpecifier 连接 Wi-Fi
     *
     * @param ssid 要连接的 Wi-Fi SSID（不带引号）
     * @param password Wi-Fi 密码（WPA2）
     * @param callback 连接结果回调
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void connectAndroid10Plus(@NonNull String ssid, @NonNull String password, @NonNull ConnectCallback callback) {
        if (mNetworkCallback != null) {
            // 先注销之前的连接，防止冲突
            disconnectAndroid10Plus();
        }
        // 构建 Wi-Fi 网络描述符，指定目标 SSID 和密码（使用 WPA2）
        WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build();

        // 构建网络请求，仅请求 Wi-Fi 类型，附带指定的 NetworkSpecifier
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build();

        // 发起网络连接请求（异步）
        mNetworkCallback = new WifiNetworkCallback(callback);
        WiFiKit.getConnectivityManager().requestNetwork(request, mNetworkCallback);
    }

    /**
     * Android 9 及以下版本使用 WifiConfiguration 接口连接 Wi-Fi
     */
    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE})
    public void connectLegacy(@NonNull String ssid, @NonNull String password, @NonNull ConnectCallback callback) {
        // 清除已存在的相同 SSID 配置，避免冲突
        List<WifiConfiguration> configs = WiFiKit.getWifiManager().getConfiguredNetworks();
        if (configs != null) {
            for (WifiConfiguration config : configs) {
                if (config.SSID != null && config.SSID.equals("\"" + ssid + "\"")) {
                    WiFiKit.getWifiManager().removeNetwork(config.networkId);
                }
            }
        }

        // 创建新的 Wi-Fi 配置
        WifiConfiguration configuration = new WifiConfiguration();
        configuration.SSID = String.format("\"%s\"", ssid);
        configuration.preSharedKey = String.format("\"%s\"", password);
        configuration.status = WifiConfiguration.Status.ENABLED;
        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

        int netId = WiFiKit.getWifiManager().addNetwork(configuration);
        if (netId == -1) {
            postFailure(callback, "添加 Wi-Fi 配置失败");
            return;
        }

        // 断开当前连接
        boolean disconnected = WiFiKit.getWifiManager().disconnect();
        // 启用新的配置
        boolean enabled = WiFiKit.getWifiManager().enableNetwork(netId, true);
        // 重新连接
        boolean reconnected = WiFiKit.getWifiManager().reconnect();

        if (disconnected && enabled && reconnected) {
            postSuccess(callback);
            // Android 9 及以下连接成功后，系统网络默认会切换到这个 Wi-Fi，数据流量默认走它。
            // 参考指南：https://developer.android.com/reference/android/net/wifi/WifiManager#enableNetwork(int,%20boolean)
        } else {
            postFailure(callback, "Legacy 连接失败");
        }
    }

    /**
     * 主线程通知连接成功
     */
    private void postSuccess(@NonNull ConnectCallback callback) {
        mHandler.post(callback::onSuccess);
    }

    /**
     * 主线程通知连接失败
     */
    private void postFailure(@NonNull ConnectCallback callback, @NonNull String reason) {
        mHandler.post(() -> callback.onFailure(reason));
    }

    /**
     * 断开当前连接的 Wi-Fi
     */
    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            disconnectAndroid10Plus();
        } else {
            disconnectLegacy();
        }
    }

    /**
     * Android 10+ 断开连接，注销之前的 NetworkCallback
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void disconnectAndroid10Plus() {
        if (mNetworkCallback != null) {
            try {
                WiFiKit.getConnectivityManager().unregisterNetworkCallback(mNetworkCallback);
            } catch (Exception exception) {
                // 可能已经注销或异常，不用特别处理
                exception.printStackTrace(System.err);
            }
            mNetworkCallback = null;
        }
    }

    /**
     * Android 9 及以下断开当前 Wi-Fi 连接
     */
    @RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)
    private void disconnectLegacy() {
        boolean success = WiFiKit.getWifiManager().disconnect();
        if (!success) {
            Log.e(WifiConnector.class.getSimpleName(), "断开 Wi-Fi 连接失败");
        }
    }
}