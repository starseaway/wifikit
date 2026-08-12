package com.xinyi.wifibridge.info;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.format.Formatter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import com.xinyi.device.DeviceContext;
import com.xinyi.wifibridge.WiFiBridge;
import com.xinyi.wifibridge.monitor.WifiAdapterState;

import java.util.List;

/**
 * Wi-Fi 信息查询
 *
 * @author 新一
 * @date 2025/5/29 10:01
 */
public class WifiInfoHelper {

    /**
     * 设备是否支持 Wi-Fi
     */
    public static boolean isWifiSupported() {
        return DeviceContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI)
                && DeviceContext.getWifiManager() != null;
    }

    /**
     * Wi-Fi 是否已开启
     */
    public static boolean isEnabled() {
        WifiManager wifiManager = getWifiManager();
        if (wifiManager == null) {
            return false;
        }
        try {
            return wifiManager.isWifiEnabled();
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 是否已连接到 Wi-Fi
     *
     * <p> Wi-Fi 未开启或未连接时返回 false </p>
     */
    public static boolean isConnected() {
        if (!isEnabled()) {
            return false;
        }
        WifiInfo info = getConnectionInfo();
        return info != null && info.getNetworkId() != -1;
    }

    /**
     * 当前 Wi-Fi 模块开关状态
     */
    @NonNull
    public static WifiAdapterState getAdapterState() {
        WifiManager wifiManager = getWifiManager();
        if (wifiManager == null) {
            return WifiAdapterState.UNKNOWN;
        }
        try {
            return WifiAdapterState.fromWifiManager(wifiManager.getWifiState());
        } catch (Exception ignored) {
            return WifiAdapterState.UNKNOWN;
        }
    }

    /**
     * 读取当前 Wi-Fi 快照
     *
     * @return Wi-Fi 快照，未连接返回 null
     */
    @Nullable
    public static WifiSnapshot getWifiSnapshot() {
        WifiInfo info = getConnectionInfo();
        if (info == null || info.getNetworkId() == -1) {
            return null;
        }
        return WifiSnapshot.from(info);
    }

    /**
     * 获取当前连接的 SSID（Wi-Fi 名称）
     *
     * @return SSID，未连接返回 null
     */
    public static String getCurrentSsid() {
        WifiInfo info = getConnectionInfo();
        if (info != null && info.getSupplicantState() == SupplicantState.COMPLETED) {
            return info.getSSID().replace("\"", "");
        }
        return null;
    }

    /**
     * 判断当前连接是否为隐藏 Wi-Fi（隐藏 SSID）
     *
     * @return true 表示是隐藏 Wi-Fi，false 表示公开 Wi-Fi 或未连接
     */
    public static boolean isHiddenSSID() {
        WifiInfo info = getConnectionInfo();
        if (info != null) {
            return info.getHiddenSSID();
        }
        return false;
    }

    /**
     * 判断某个指定 SSID 是否在当前扫描结果中出现。多个 SSID 可以使用逗号隔个
     */
    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE})
    public static boolean isSsidVisible(String... targetSsid) {
        return isSsidVisible(String.join(",", targetSsid));
    }

    /**
     * 判断某个指定 SSID 是否在当前扫描结果中出现。
     *
     * @param targetSsid 要检测的 SSID 名称
     * @return true 表示该 SSID 存在于当前扫描结果中
     */
    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE})
    public static boolean isSsidVisible(String targetSsid) {
        List<ScanResult> results = WiFiBridge.getWifiManager().getScanResults();
        if (results != null) {
            for (ScanResult result : results) {
                if (targetSsid.equals(result.SSID)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取当前连接的 Wi-Fi 网络 ID（可用于判断配置项）
     *
     * @return 网络 ID，失败时返回 -1
     */
    public static int getNetworkId() {
        WifiInfo info = getConnectionInfo();
        if (info != null) {
            return info.getNetworkId();
        }
        return -1;
    }

    /**
     * 获取当前连接的 BSSID（接入点 MAC 地址）
     *
     * @return BSSID，未连接返回 null
     */
    public static String getCurrentBssid() {
        WifiInfo info = getConnectionInfo();
        if (info != null) {
            return info.getBSSID();
        }
        return null;
    }

    /**
     * 获取当前连接的信号强度
     *
     * @return 信号强度（单位 dBm），未连接返回 Integer.MIN_VALUE
     */
    public static int getCurrentSignalLevel() {
        WifiInfo info = getConnectionInfo();
        if (info != null) {
            return info.getRssi();
        }
        return Integer.MIN_VALUE;
    }

    /**
     * 获取当前连接的 IP 地址（格式化为字符串）
     *
     * @return IP 地址，未连接返回 null
     */
    public static String getCurrentIpAddress() {
        WifiInfo info = getConnectionInfo();
        if (info != null) {
            int ip = info.getIpAddress();
            return Formatter.formatIpAddress(ip);
        }
        return null;
    }

    /**
     * 获取当前连接的 MAC 地址（Android 6 后通常为固定伪地址）
     *
     * @return MAC 地址字符串
     */
    @SuppressLint("HardwareIds")
    public static String getMacAddress() {
        WifiInfo info = getConnectionInfo();
        if (info != null) {
            return info.getMacAddress();
        }
        return null;
    }

    /**
     * 获取当前连接的频率
     *
     * @return 频率，单位 MHz（如 2412 表示 2.4GHz），未连接返回 -1
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static int getFrequency() {
        WifiInfo info = getConnectionInfo();
        if (info != null) {
            return info.getFrequency();
        }
        return -1;
    }

    /**
     * 兼容 Android 5.0 以下系统的 Wi-Fi 频率获取方法
     *
     * <p>该方法通过当前连接的 BSSID 与 Wi-Fi 扫描结果进行匹配，获取对应频率。</p>
     *
     * @return 当前连接 Wi-Fi 的频率，单位 MHz。获取失败返回 -1。
     */
    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE})
    public static int getFrequencyLegacy() {
        WifiInfo info = getConnectionInfo();
        if (info == null) {
            return -1;
        }

        String currentBssid = info.getBSSID();
        if (currentBssid == null) {
            return -1;
        }

        // 获取当前的扫描结果列表
        List<ScanResult> scanResults = WiFiBridge.getWifiManager().getScanResults();
        if (scanResults == null || scanResults.isEmpty()) {
            return -1;
        }

        // 遍历查找与当前连接的 BSSID 匹配的项
        for (ScanResult result : scanResults) {
            if (currentBssid.equals(result.BSSID)) {
                return result.frequency; // frequency 字段在 API 1 就有
            }
        }

        return -1;
    }

    /**
     * 判断是否为 5GHz 网络
     *
     * @return true 表示为 5GHz，false 为 2.4GHz 或未知
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static boolean is5GHz() {
        int frequency = getFrequency();
        return is5GHz(frequency);
    }

    /**
     * 判断是否为 5GHz 网络
     *
     * @return true 表示为 5GHz，false 为 2.4GHz 或未知
     */
    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE})
    public static boolean is5GHzLegacy() {
        int frequency = getFrequencyLegacy();
        return is5GHz(frequency);
    }

    /**
     * 根据频率判断是否为 5GHz 网络
     */
    public static boolean is5GHz(int frequency) {
        return frequency >= 4900 && frequency <= 5900;
    }

    /**
     * 获取当前 Wi-Fi 链接速度（Mbps）
     *
     * @return 速度（单位 Mbps），未连接返回 -1
     */
    public static int getLinkSpeed() {
        WifiInfo info = getConnectionInfo();
        if (info != null) {
            return info.getLinkSpeed();
        }
        return -1;
    }

    /**
     * 判断设备是否已连接到 Wi-Fi 网络
     *
     * @return 如果设备已连接到 Wi-Fi 网络，则返回 true；否则返回 false。
     * @see #isConnected()
     */
    public static boolean isWifiConnected() {
        return isConnected();
    }

    /**
     * 获取网关地址（通常为路由器 IP）
     *
     * @return 网关 IP，未连接返回 null
     */
    public static String getGatewayAddress() {
        DhcpInfo dhcpInfo = WiFiBridge.getWifiManager().getDhcpInfo();
        if (dhcpInfo != null) {
            return Formatter.formatIpAddress(dhcpInfo.gateway);
        }
        return null;
    }

    /**
     * 获取当前网络的 DNS 服务器地址
     *
     * @return DNS 地址字符串，失败时返回 null
     */
    public static String getDnsAddress() {
        DhcpInfo dhcpInfo = WiFiBridge.getWifiManager().getDhcpInfo();
        if (dhcpInfo != null) {
            return Formatter.formatIpAddress(dhcpInfo.dns1);
        }
        return null;
    }

    /**
     * 获取当前网络的子网掩码
     *
     * @return 子网掩码字符串，失败时返回 null
     */
    public static String getSubnetMask() {
        DhcpInfo dhcpInfo = WiFiBridge.getWifiManager().getDhcpInfo();
        if (dhcpInfo != null) {
            return Formatter.formatIpAddress(dhcpInfo.netmask);
        }
        return null;
    }

    /**
     * 获取 DHCP 服务器地址
     *
     * @return DHCP 地址，失败返回 null
     */
    public static String getDhcpServerAddress() {
        DhcpInfo dhcpInfo = WiFiBridge.getWifiManager().getDhcpInfo();
        if (dhcpInfo != null) {
            return Formatter.formatIpAddress(dhcpInfo.serverAddress);
        }
        return null;
    }

    /**
     * 获取当前 DHCP 信息描述字符串（一般我调试用）
     *
     * @return DHCP 信息字符串
     */
    public static String getDhcpInfoSummary() {
        DhcpInfo info = WiFiBridge.getWifiManager().getDhcpInfo();
        if (info == null) {
            return "";
        }

        return "ip=" + Formatter.formatIpAddress(info.ipAddress) +
                ", gateway=" + Formatter.formatIpAddress(info.gateway) +
                ", netmask=" + Formatter.formatIpAddress(info.netmask) +
                ", dns1=" + Formatter.formatIpAddress(info.dns1) +
                ", dns2=" + Formatter.formatIpAddress(info.dns2) +
                ", server=" + Formatter.formatIpAddress(info.serverAddress) +
                ", leaseDuration=" + info.leaseDuration;
    }

    /**
     * 获取 Wi-Fi 管理器
     *
     * @return Wi-Fi 管理器，未连接返回 null
     */
    @Nullable
    private static WifiManager getWifiManager() {
        return DeviceContext.getWifiManager();
    }

    /**
     * 获取当前连接的 Wi-Fi 信息
     *
     * @return Wi-Fi 信息，未连接返回 null
     */
    @Nullable
    private static WifiInfo getConnectionInfo() {
        WifiManager wifiManager = getWifiManager();
        if (wifiManager == null) {
            return null;
        }
        try {
            return wifiManager.getConnectionInfo();
        } catch (Exception ignored) {
            return null;
        }
    }
}