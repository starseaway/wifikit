package com.xinyi.wifikit.connect;

import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.xinyi.wifikit.WiFiKit;

/**
 * Android 10+ 的网络回调实现
 *
 * @author 新一
 * @date 2025/5/30 8:48
 */
@RequiresApi(api = Build.VERSION_CODES.Q)
public class WifiNetworkCallback extends ConnectivityManager.NetworkCallback {

    /**
     * 连接结果回调
     */
    private final ConnectCallback mCallback;

    /**
     * 主线程 Handler
     */
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public WifiNetworkCallback(ConnectCallback callback) {
        this.mCallback = callback;
    }

    @Override
    public void onAvailable(@NonNull Network network) {
        // 绑定网络到当前进程的默认网络，此后该进程发出的所有网络请求（包括 HttpURLConnection、OkHttp、Socket 等）都会走绑定的这张网络。
        WiFiKit.getConnectivityManager().bindProcessToNetwork(network);

        // 连接成功
        mHandler.post(mCallback::onSuccess);
    }

    @Override
    public void onUnavailable() {
        // 连接失败
        mHandler.post(() -> mCallback.onFailure("Wi-Fi 网络不可用"));
    }
}