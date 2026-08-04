package com.xinyi.wifibridge.info;

import android.net.wifi.WifiInfo;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.Formatter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Wi-Fi 连接信息快照
 *
 * <p> 用于描述某一时刻的 Wi-Fi 连接状态，方便在状态回调中直接使用 </p>
 *
 * @author 新一
 * @date 2026/8/4 9:20
 */
public class WifiConnectionInfo {

    /**
     * Wi-Fi 名称
     */
    private final String ssid;

    /**
     * 接入点 MAC 地址
     */
    private final String bssid;

    /**
     * 网络配置 ID
     */
    private final int networkId;

    /**
     * 信号强度（单位 dBm）
     */
    private final int rssi;

    /**
     * 链接速度（单位 Mbps）
     */
    private final int linkSpeed;

    /**
     * 频率（单位 MHz），不可用时为 -1
     */
    private final int frequency;

    /**
     * IP 地址
     */
    private final String ipAddress;

    /**
     * 是否为隐藏 SSID
     */
    private final boolean hiddenSsid;

    private WifiConnectionInfo(String ssid, String bssid, int networkId, int rssi,
                               int linkSpeed, int frequency, String ipAddress, boolean hiddenSsid) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.networkId = networkId;
        this.rssi = rssi;
        this.linkSpeed = linkSpeed;
        this.frequency = frequency;
        this.ipAddress = ipAddress;
        this.hiddenSsid = hiddenSsid;
    }

    /**
     * 从系统 {@link WifiInfo} 构建连接信息快照
     *
     * @param wifiInfo 系统 Wi-Fi 信息，为空时返回 null
     */
    @NonNull
    public static WifiConnectionInfo from(@NonNull WifiInfo wifiInfo) {
        String ssid = wifiInfo.getSSID();
        if (ssid != null) {
            ssid = ssid.replace("\"", "");
            if ("<unknown ssid>".equals(ssid) || TextUtils.isEmpty(ssid)) {
                ssid = null;
            }
        }

        String bssid = wifiInfo.getBSSID();
        if ("00:00:00:00:00:00".equals(bssid) || TextUtils.isEmpty(bssid)) {
            bssid = null;
        }

        int frequency = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            frequency = wifiInfo.getFrequency();
        }

        String ipAddress = null;
        int ip = wifiInfo.getIpAddress();
        if (ip != 0) {
            ipAddress = Formatter.formatIpAddress(ip);
        }

        return new WifiConnectionInfo(
                ssid,
                bssid,
                wifiInfo.getNetworkId(),
                wifiInfo.getRssi(),
                wifiInfo.getLinkSpeed(),
                frequency,
                ipAddress,
                wifiInfo.getHiddenSSID()
        );
    }

    @Nullable
    public String getSsid() {
        return ssid;
    }

    @Nullable
    public String getBssid() {
        return bssid;
    }

    public int getNetworkId() {
        return networkId;
    }

    public int getRssi() {
        return rssi;
    }

    public int getLinkSpeed() {
        return linkSpeed;
    }

    public int getFrequency() {
        return frequency;
    }

    @Nullable
    public String getIpAddress() {
        return ipAddress;
    }

    public boolean isHiddenSsid() {
        return hiddenSsid;
    }

    @NonNull
    @Override
    public String toString() {
        return "WifiConnectionInfo{" +
                "ssid='" + ssid + '\'' +
                ", bssid='" + bssid + '\'' +
                ", networkId=" + networkId +
                ", rssi=" + rssi +
                ", linkSpeed=" + linkSpeed +
                ", frequency=" + frequency +
                ", ipAddress='" + ipAddress + '\'' +
                ", hiddenSsid=" + hiddenSsid +
                '}';
    }
}