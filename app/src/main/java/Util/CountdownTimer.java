package Util;

import android.os.Handler;
import android.os.Looper;

/**
 * 歌曲倒計時工具類
 * 用於播放器頁面的時間倒計時顯示
 * 功能：
 * - 接收總時長（毫秒）開始倒計時
 * - 每秒更新剩餘時間
 * - 支持暫停/恢復
 * - 到達 00:00 時觸發完成回調
 */
public class CountdownTimer {

    private final Handler handler;
    private OnCountdownListener listener;

    private long totalDurationMs;      // 總時長（毫秒）
    private long remainingMs;          // 剩餘時間（毫秒）
    private boolean isRunning;         // 是否正在運行
    private boolean isPaused;          // 是否暫停

    private static final long TICK_INTERVAL = 1000; // 每秒更新一次

    public CountdownTimer() {
        this.handler = new Handler(Looper.getMainLooper());
        this.isRunning = false;
        this.isPaused = false;
    }

    /**
     * 設置倒計時監聽器
     */
    public void setListener(OnCountdownListener listener) {
        this.listener = listener;
    }

    /**
     * 開始倒計時
     * @param durationMs 總時長（毫秒）
     */
    public void start(long durationMs) {
        stop(); // 先停止之前的倒計時

        this.totalDurationMs = durationMs;
        this.remainingMs = durationMs;
        this.isRunning = true;
        this.isPaused = false;

        // 立即發送初始時間
        notifyTick();

        // 開始倒計時
        handler.postDelayed(tickRunnable, TICK_INTERVAL);
    }

    /**
     * 暫停倒計時
     */
    public void pause() {
        if (isRunning && !isPaused) {
            isPaused = true;
            handler.removeCallbacks(tickRunnable);
        }
    }

    /**
     * 恢復倒計時
     */
    public void resume() {
        if (isRunning && isPaused) {
            isPaused = false;
            handler.postDelayed(tickRunnable, TICK_INTERVAL);
        }
    }

    /**
     * 停止倒計時
     */
    public void stop() {
        isRunning = false;
        isPaused = false;
        handler.removeCallbacks(tickRunnable);
    }

    /**
     * 檢查是否正在運行
     */
    public boolean isRunning() {
        return isRunning && !isPaused;
    }

    /**
     * 檢查是否暫停
     */
    public boolean isPaused() {
        return isPaused;
    }

    /**
     * 獲取剩餘時間（毫秒）
     */
    public long getRemainingMs() {
        return remainingMs;
    }

    /**
     * 獲取總時長（毫秒）
     */
    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    /**
     * 格式化時間為 "MM:ss" 格式
     * @param ms 毫秒
     * @return 格式化的時間字串
     */
    public static String formatTime(long ms) {
        if (ms < 0) {
            ms = 0;
        }
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * 每秒執行的任務
     */
    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning || isPaused) {
                return;
            }

            remainingMs -= TICK_INTERVAL;

            if (remainingMs <= 0) {
                remainingMs = 0;
                notifyTick();
                notifyFinish();
                stop();
            } else {
                notifyTick();
                handler.postDelayed(this, TICK_INTERVAL);
            }
        }
    };

    /**
     * 通知監聽器時間更新
     */
    private void notifyTick() {
        if (listener != null) {
            listener.onTick(remainingMs, formatTime(remainingMs));
        }
    }

    /**
     * 通知監聯器倒計時完成
     */
    private void notifyFinish() {
        if (listener != null) {
            listener.onFinish();
        }
    }

    /**
     * 倒計時監聽器接口
     */
    public interface OnCountdownListener {
        /**
         * 每秒回調
         * @param remainingMs 剩餘毫秒數
         * @param formattedTime 格式化的時間 "MM:ss"
         */
        void onTick(long remainingMs, String formattedTime);

        /**
         * 倒計時完成（到達 00:00）
         */
        void onFinish();
    }
}
