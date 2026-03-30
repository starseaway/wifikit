package com.xinyi.wifikit.connect;

/**
 * Wi-Fi 连接回调接口
 *
 * @author 新一
 * @date 2025/5/29 13:25
 */
public interface ConnectCallback {

    /**
     * Wi-Fi 连接成功
     */
    void onSuccess();

    /**
     * Wi-Fi 连接失败
     *
     * @param reason 失败原因描述
     */
    void onFailure(String reason);
}