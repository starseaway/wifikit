package com.xinyi.wifikit.scanner;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;

import androidx.annotation.RequiresPermission;
import androidx.annotation.WorkerThread;

import com.xinyi.device.DeviceContext;
import com.xinyi.wifikit.WiFiKit;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Wi-Fi 扫描工具
 *
 * <p> 用于扫描附近 Wi-Fi，支持单次或循环扫描、过滤器匹配、超时处理等功能 </p>
 *
 * @author 新一
 * @date 2025/5/29 10:26
 */
public class WifiScanner {

    /**
     * 默认超时时间：10秒
     */
    private static final long DEFAULT_TIMEOUT_MS = 10000;

    /**
     * 线程处理器，用于处理超时任务和循环扫描任务
     */
    private Handler mHandler;

    /**
     * 扫描超时任务
     */
    private Runnable mTimeoutRunnable;

    /**
     * 是否正在进行循环扫描
     */
    private boolean isLoopScanner = false;

    /**
     * 扫描结果过滤器，可为 null
     */
    private ResultFilter mFilter;

    /**
     * 设置扫描结果过滤器，用于对 {@link ScanResult} 列表进行自定义筛选
     *
     * <p> 调用此方法后，扫描结果中满足 {@code filter.reject(result) == true} 的项将被剔除 </p>
     *
     * @param mFilter 过滤器
     */
    public void setFilter(ResultFilter mFilter) {
        this.mFilter = mFilter;
    }

    /**
     * 执行单次 Wi-Fi 扫描，支持结果过滤与超时处理。
     *
     * @param callback 扫描结果回调接口
     */
    @WorkerThread
    public void scanWifi(ScannerCallback callback) {
        IntentFilter intentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        // 创建 Handler，默认获取当前线程上的Looper对象，建议在WorkerThread中使用
        if (mHandler == null) {
            mHandler = new Handler();
        }

        // 创建广播接收器实例，监听扫描结果
        WifiScannerReceiver receiver = new WifiScannerReceiver(callback, this);

        try {
            // 注册广播接收器
            DeviceContext.getApplication().registerReceiver(receiver, intentFilter);
        } catch (Exception exception) {
            exception.printStackTrace(System.err);
            callback.onFailure("广播注册失败: " + exception.getMessage());
            return;
        }

        // 设置超时任务，超时后取消广播注册并通知回调
        mTimeoutRunnable = () -> {
            try {
                DeviceContext.getApplication().unregisterReceiver(receiver);
            } catch (Exception exception) {
                exception.printStackTrace(System.err);
            }
            callback.onTimeout();
        };
        // 添加超时任务
        addTimeout(mTimeoutRunnable);

        // 启动扫描，如果失败，取消超时任务，注销广播，回调失败
        boolean success = WiFiKit.getWifiManager().startScan();
        if (!success) {
            cancelTimeout();
            try {
                DeviceContext.getApplication().unregisterReceiver(receiver);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            callback.onFailure("startScan 返回 false");
        }
    }

    /**
     * 添加超时任务
     *
     * @param runnable 超时任务
     */
    public void addTimeout(Runnable runnable) {
        addTimeout(runnable, DEFAULT_TIMEOUT_MS);
    }

    /**
     * 添加自定义超时时间任务
     *
     * @param runnable 超时任务
     * @param timeout 超时时间，毫秒
     */
    public void addTimeout(Runnable runnable, long timeout) {
        if (mHandler == null) {
            return;
        }
        mHandler.postDelayed(runnable, timeout);
    }

    /**
     * 取消超时任务
     */
    public void cancelTimeout() {
        if (mHandler == null || mTimeoutRunnable == null) {
            return;
        }
        mHandler.removeCallbacks(mTimeoutRunnable);
    }

    /**
     * 过滤扫描结果列表，移除被过滤器拒绝的 {@link ScanResult}。
     *
     * @param results 扫描结果列表，非空
     */
    public void filterScanResults(List<ScanResult> results) {
        if (results == null || results.isEmpty() || mFilter == null) {
            return;
        }

        // 移除所有被拒绝的元素，即 filter.reject(result) 为 true 的元素
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            // API 24+ 可以使用 removeIf
            results.removeIf(mFilter::reject);
        } else {
            for (int i = results.size() - 1; i >= 0; i--) {
                if (mFilter.reject(results.get(i))) {
                    results.remove(i);
                }
            }
        }
    }

    /**
     * 开始循环扫描，按固定间隔重复触发扫描操作。
     *
     * @param intervalMs 每次扫描之间的间隔时间（毫秒）
     * @param callback 扫描结果回调接口
     */
    @WorkerThread
    public void startLoopScanner(long intervalMs, ScannerCallback callback) {
        if (intervalMs <= 0) {
            throw new IllegalArgumentException("间隔时间必须大于 0");
        }
        if (isLoopScanner) {
            return;
        }
        isLoopScanner = true;
        // 启动首次扫描，并传入循环回调控制后续扫描
        scanWifi(new LoopScannerCallback(intervalMs, callback, this));
    }

    /**
     * 停止循环扫描。
     * <p>
     * 建议在生命周期结束时主动调用，以避免线程泄漏。
     * </p>
     */
    public void stopLoopScan() {
        if (!isLoopScanner) {
            return;
        }
        isLoopScanner = false;
        cancelTimeout();
    }

    /**
     * 判断当前是否正在进行循环扫描。
     *
     * @return true 表示正在循环扫描，false 表示未扫描或已停止
     */
    public boolean isLoopScanner() {
        return isLoopScanner;
    }

    /**
     * Wi-Fi 扫描结果的广播接收器
     */
    private static class WifiScannerReceiver extends BroadcastReceiver {

        /**
         * 结果回调接口
         */
        private final ScannerCallback mCallback;

        /**
         * 外部 WifiScanner 实例，用于访问 Handler 和超时任务
         */
        private final WeakReference<WifiScanner> mScanner;

        /**
         * 构造函数
         *
         * @param callback 扫描结果回调接口
         * @param scanner  外部 WifiScanner 实例
         */
        public WifiScannerReceiver(ScannerCallback callback, WifiScanner scanner) {
            this.mCallback = callback;
            this.mScanner = new WeakReference<>(scanner);
        }

        /**
         * 接收到扫描结果广播后的回调，取消注册广播，移除超时任务，过滤结果，通知回调。
         *
         * @param context 上下文
         * @param intent  广播意图
         */
        @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE})
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                DeviceContext.getApplication().unregisterReceiver(this);
            } catch (Exception exception) {
                exception.printStackTrace(System.err);
            }

            // 获取扫描实例
            WifiScanner scanner = mScanner.get();
            if (scanner == null) {
                return;
            }

            // 取消超时任务
            scanner.cancelTimeout();

            // 获取扫描结果
            List<ScanResult> results = WiFiKit.getWifiManager().getScanResults();
            // 移除所有被过滤掉的元素
            scanner.filterScanResults(results);
            // 通知回调
            mCallback.onResult(results);
        }
    }

    /**
     * 循环扫描时使用的回调实现，控制重复扫描的逻辑。
     */
    private static class LoopScannerCallback implements ScannerCallback {

        /**
         * 两次扫描间隔时间
         */
        private final long mIntervalMs;

        /**
         * 扫描回调，扫描结果和状态最终会转发给它
         */
        private final ScannerCallback mScannerCallback;

        /**
         * 外部 WifiScanner 实例，用于访问 Handler 和超时任务
         */
        private final WeakReference<WifiScanner> mScanner;

        /**
         * 构造函数
         *
         * @param intervalMs 两次扫描间隔时间
         * @param scannerCallback 扫描回调
         * @param scanner  外部 WifiScanner 实例
         */
        public LoopScannerCallback(long intervalMs, ScannerCallback scannerCallback, WifiScanner scanner) {
            this.mIntervalMs = intervalMs;
            this.mScannerCallback = scannerCallback;
            this.mScanner = new WeakReference<>(scanner);
        }

        /**
         * 扫描成功时回调，转发结果并根据状态决定是否继续循环扫描。
         *
         * @param results 扫描结果列表
         */
        @Override
        public void onResult(List<ScanResult> results) {
            // 通知回调
            mScannerCallback.onResult(results);
            // 获取扫描实例
            WifiScanner scanner = mScanner.get();
            if (scanner == null) {
                return;
            }
            if (scanner.isLoopScanner() && scanner.mHandler != null) {
                // 继续循环扫描
                scanner.mHandler.postDelayed(() -> scanner.scanWifi(this), mIntervalMs);
            }
        }

        /**
         * 扫描超时回调，停止循环扫描并通知用户回调。
         */
        @Override
        public void onTimeout() {
            setLoopScanner(false);
            mScannerCallback.onTimeout();
        }

        /**
         * 扫描失败回调，停止循环扫描并通知用户回调。
         *
         * @param reason 失败原因
         */
        @Override
        public void onFailure(String reason) {
            setLoopScanner(false);
            mScannerCallback.onFailure(reason);
        }

        /**
         * 设置循环扫描状态
         *
         * @param loopScanner 循环扫描状态
         */
        public void setLoopScanner(boolean loopScanner) {
            // 获取扫描实例
            WifiScanner scanner = mScanner.get();
            if (scanner == null) {
                return;
            }
            scanner.isLoopScanner = loopScanner;
        }
    }
}