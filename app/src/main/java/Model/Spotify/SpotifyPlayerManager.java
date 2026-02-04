package Model.Spotify;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.graduationproject.BuildConfig;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.PlayerState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import Model.POJO.MusicItem;

/**
 * Spotify 播放器管理類（單例模式）——修改之後的結果，不用單例容易發生衝突
 * 負責 Spotify App Remote SDK 的連接與播放控制
 * 所有 ViewModel 共享同一個連接狀態。

 * 連接流程：
 * 1. 調用 connect() 連接 Spotify App
 * 2. showAuthView(true) 讓 SDK 自動處理授權（在 Spotify App 內完成）
 * 3. 連接成功後可以控制播放
 */
public class SpotifyPlayerManager {

    private static final String TAG = "SpotifyPlayerManager";
    private static final long CONNECTION_TIMEOUT_MS = 15000; // 15秒超時

    // 單例實例
    private static volatile SpotifyPlayerManager INSTANCE;

    private SpotifyAppRemote spotifyAppRemote;
    private boolean isConnecting = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // 多回調支持：使用列表存儲多個回調，支持多個 ViewModel 同時監聽
    private final List<SpotifyPlayerCallback> callbacks = new CopyOnWriteArrayList<>();
    private String pendingTrackUri;

    // PlaybackStateListener 列表，用於多頁面監聽播放狀態
    // 使用 CopyOnWriteArrayList 確保線程安全
    private final List<PlaybackStateListener> playbackStateListeners = new CopyOnWriteArrayList<>();

    // 當前播放狀態緩存，用於新 Listener 加入時獲取狀態
    private PlayerState cachedPlayerState;

    // 當前播放列表緩存，用於 MiniPlayer 導航到 PlayerView 時傳遞完整列表
    private List<MusicItem> cachedPlaylist;
    private int cachedPlaylistIndex = 0;

    /**
     * 私有構造函數，防止外部直接創建實例
     */
    private SpotifyPlayerManager() {
    }

    /**
     * 獲取 SpotifyPlayerManager 單例實例
     * 使用雙重檢查鎖定確保線程安全
     * @return SpotifyPlayerManager 實例
     */
    public static SpotifyPlayerManager getInstance() {
        if (INSTANCE == null) {
            synchronized (SpotifyPlayerManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SpotifyPlayerManager();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 添加回調監聽器
     * 支持多個 ViewModel 同時監聽連接和播放事件
     * @param callback 回調監聯器
     */
    public void addCallback(SpotifyPlayerCallback callback) {
        if (callback != null && !callbacks.contains(callback)) {
            callbacks.add(callback);
            Log.d(TAG, "添加 Callback，當前數量: " + callbacks.size());
        }
    }

    /**
     * 移除回調監聽器
     * @param callback 回調監聽器
     */
    public void removeCallback(SpotifyPlayerCallback callback) {
        if (callback != null) {
            callbacks.remove(callback);
            Log.d(TAG, "移除 Callback，當前數量: " + callbacks.size());
        }
    }

    /**
     * @deprecated 使用 {@link #addCallback(SpotifyPlayerCallback)} 代替
     * 為了向後兼容保留此方法，內部會調用 addCallback
     */
    @Deprecated
    public void setCallback(SpotifyPlayerCallback callback) {
        // 向後兼容：清除舊回調，添加新回調
        // 注意：這種方式不推薦，建議直接使用 addCallback
        addCallback(callback);
    }

    /**
     * 檢查 Spotify App 是否已安裝
     */
    public boolean isSpotifyInstalled(Context context) {
        return SpotifyAppRemote.isSpotifyInstalled(context);
    }

    /**
     * 連接到 Spotify App
     * 使用 showAuthView(true) 讓 SDK 自動處理授權
     * 與測試代碼保持完全一致的連接流程
     *
     * @param context Activity Context (必須是 Activity Context)
     */
    public void connect(Context context) {
        if (isConnecting) {
            Log.d(TAG, "正在連接中，忽略重複請求");
            return;
        }

        // 單例模式：如果已連接，直接回調成功，不重新連接
        if (spotifyAppRemote != null && spotifyAppRemote.isConnected()) {
            Log.d(TAG, "已連接，使用現有連接");
            handler.post(this::notifyConnected);
            return;
        }

        // 檢查 Spotify 是否安裝
        if (!isSpotifyInstalled(context)) {
            Log.e(TAG, "Spotify App 未安裝");
            notifyConnectionFailed("請先安裝 Spotify App");
            return;
        }

        isConnecting = true;
        Log.d(TAG, "=== 開始連接 Spotify App Remote ===");
        Log.d(TAG, "CLIENT_ID: " + BuildConfig.SPOTIFY_CLIENT_ID);
        Log.d(TAG, "REDIRECT_URI: " + BuildConfig.SPOTIFY_REDIRECT_URI);

        notifyConnecting();

        // 設置連接超時（與測試代碼一致：15秒）
        handler.postDelayed(() -> {
            if (isConnecting) {
                Log.e(TAG, "連接超時 (" + CONNECTION_TIMEOUT_MS + "ms)");
                isConnecting = false;
                notifyConnectionFailed("連接超時，請確認 Spotify App 已登錄");
            }
        }, CONNECTION_TIMEOUT_MS);

        // 創建連接參數（與測試代碼完全一致）
        Log.d(TAG, "=== 創建 ConnectionParams ===");
        ConnectionParams connectionParams = new ConnectionParams.Builder(BuildConfig.SPOTIFY_CLIENT_ID)
                .setRedirectUri(BuildConfig.SPOTIFY_REDIRECT_URI)
                .showAuthView(true)
                .build();

        Log.d(TAG, "=== 開始調用 SpotifyAppRemote.connect() ===");

        try {
            SpotifyAppRemote.connect(context, connectionParams, new Connector.ConnectionListener() {
                @Override
                public void onConnected(SpotifyAppRemote appRemote) {
                    Log.d(TAG, "=== onConnected 被調用 ===");
                    handler.removeCallbacksAndMessages(null);
                    isConnecting = false;
                    spotifyAppRemote = appRemote;

                    Log.d(TAG, "成功連接到 Spotify App Remote");

                    // 訂閱播放狀態
                    subscribeToPlayerState();

                    // 獲取當前播放狀態（與測試代碼一致）
                    getPlayerState();

                    handler.post(() -> {
                        notifyConnected();

                        // 播放待播放曲目
                        if (pendingTrackUri != null) {
                            playTrack(pendingTrackUri);
                            pendingTrackUri = null;
                        }
                    });
                }

                @Override
                public void onFailure(Throwable throwable) {
                    Log.e(TAG, "=== onFailure 被調用 ===");
                    Log.e(TAG, "錯誤類型: " + throwable.getClass().getName());
                    Log.e(TAG, "錯誤信息: " + throwable.getMessage());

                    if (throwable.getCause() != null) {
                        Log.e(TAG, "Cause: " + throwable.getCause().getMessage());
                    }
                    throwable.printStackTrace();

                    handler.removeCallbacksAndMessages(null);
                    isConnecting = false;

                    String errorMessage = parseErrorMessage(throwable);

                    handler.post(() -> {
                        notifyConnectionFailed(errorMessage);
                    });
                }
            });

            Log.d(TAG, "SpotifyAppRemote.connect() 調用完成，等待回調...");

        } catch (Exception e) {
            Log.e(TAG, "connect() 拋出異常: " + e.getMessage());
            e.printStackTrace();
            handler.removeCallbacksAndMessages(null);
            isConnecting = false;

            notifyConnectionFailed("連接異常: " + e.getMessage());
        }
    }

    /**
     * 先打開 Spotify App，然後嘗試連接
     * 用於解決某些設備上直接連接失敗的問題
     */
    public void openSpotifyAndConnect(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.spotify.music");
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

            Log.d(TAG, "已打開 Spotify App，3秒後嘗試連接...");

            // 延遲 3 秒後連接
            handler.postDelayed(() -> connect(context), 3000);
        } else {
            notifyConnectionFailed("無法打開 Spotify App");
        }
    }

    /**
     * 斷開 Spotify 連接
     */
    public void disconnect() {
        handler.removeCallbacksAndMessages(null);
        isConnecting = false;

        if (spotifyAppRemote != null) {
            Log.d(TAG, "斷開 Spotify 連接");
            SpotifyAppRemote.disconnect(spotifyAppRemote);
            spotifyAppRemote = null;
        }
    }

    /**
     * 播放指定曲目
     */
    public void playTrack(String trackUri) {
        if (trackUri == null || trackUri.isEmpty()) {
            Log.e(TAG, "Track URI 為空");
            notifyError("無效的曲目");
            return;
        }

        String spotifyUri = formatTrackUri(trackUri);

        if (!isConnected()) {
            Log.d(TAG, "尚未連接，保存待播放曲目: " + spotifyUri);
            pendingTrackUri = spotifyUri;
            return;
        }

        Log.d(TAG, "播放曲目: " + spotifyUri);
        spotifyAppRemote.getPlayerApi().play(spotifyUri)
                .setResultCallback(empty -> {
                    Log.d(TAG, "開始播放");
                    notifyPlaybackStarted(spotifyUri);
                })
                .setErrorCallback(throwable -> {
                    Log.e(TAG, "播放失敗", throwable);
                    String message = throwable.getMessage();
                    // 檢測 Spotify Free 用戶無法點播特定歌曲的錯誤
                    if (message != null && message.contains("CANT_PLAY_ON_DEMAND")) {
                        Log.w(TAG, "Spotify Free 用戶無法點播特定歌曲");
                        notifyTrackPlayRestricted();
                    } else {
                        notifyError("播放失敗: " + message);
                    }
                });
    }

    /**
     * 暫停播放
     */
    public void pause() {
        if (!isConnected()) {
            Log.e(TAG, "尚未連接到 Spotify");
            return;
        }

        spotifyAppRemote.getPlayerApi().pause()
                .setResultCallback(empty -> {
                    Log.d(TAG, "已暫停");
                    notifyPlaybackPaused();
                })
                .setErrorCallback(throwable -> {
                    Log.e(TAG, "暫停失敗", throwable);
                    notifyError("暫停失敗: " + throwable.getMessage());
                });
    }

    /**
     * 恢復播放
     */
    public void resume() {
        if (!isConnected()) {
            Log.e(TAG, "尚未連接到 Spotify");
            return;
        }

        spotifyAppRemote.getPlayerApi().resume()
                .setResultCallback(empty -> {
                    Log.d(TAG, "已恢復播放");
                    notifyPlaybackResumed();
                })
                .setErrorCallback(throwable -> {
                    Log.e(TAG, "恢復播放失敗", throwable);
                    notifyError("恢復播放失敗: " + throwable.getMessage());
                });
    }

    /**
     * 跳轉到指定播放位置
     * @param positionMs 目標位置（毫秒）
     */
    public void seekTo(long positionMs) {
        if (!isConnected()) {
            Log.e(TAG, "尚未連接到 Spotify");
            return;
        }

        Log.d(TAG, "跳轉到位置: " + positionMs + "ms");
        spotifyAppRemote.getPlayerApi().seekTo(positionMs)
                .setResultCallback(empty -> {
                    Log.d(TAG, "跳轉成功");
                    notifySeekCompleted(positionMs);
                })
                .setErrorCallback(throwable -> {
                    Log.e(TAG, "跳轉失敗", throwable);
                    String message = throwable.getMessage();
                    // 檢測 Spotify Free 用戶限制
                    if (message != null) {
                        notifySeekRestricted();
                    } else {
                        notifyError("跳轉失敗: " + message);
                    }
                });
    }

    /**
     * 獲取當前播放狀態
     */
    public void getPlayerState() {
        if (!isConnected()) {
            return;
        }

        spotifyAppRemote.getPlayerApi().getPlayerState()
                .setResultCallback(this::notifyPlayerStateChanged)
                .setErrorCallback(throwable -> {
                    Log.e(TAG, "獲取播放狀態失敗", throwable);
                });
    }

    /**
     * 訂閱播放狀態更新
     */
    private void subscribeToPlayerState() {
        if (spotifyAppRemote == null) return;

        spotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(playerState -> {
                    // 緩存當前播放狀態
                    cachedPlayerState = playerState;

                    // 通知所有回調
                    notifyPlayerStateChanged(playerState);

                    // 通知所有 PlaybackStateListener
                    notifyPlaybackStateListeners(playerState);
                })
                .setErrorCallback(throwable -> {
                    Log.e(TAG, "訂閱播放狀態失敗", throwable);
                });
    }

    /**
     * 通知所有 PlaybackStateListener
     * @param playerState 當前播放狀態
     */
    private void notifyPlaybackStateListeners(PlayerState playerState) {
        if (playerState == null) return;

        boolean isPlaying = !playerState.isPaused;
        long positionMs = playerState.playbackPosition;
        long durationMs = playerState.track != null ? playerState.track.duration : 0;
        String trackUri = playerState.track != null ? playerState.track.uri : null;

        for (PlaybackStateListener listener : playbackStateListeners) {
            handler.post(() -> {
                listener.onPlaybackStateChanged(isPlaying, positionMs, durationMs);
                if (trackUri != null) {
                    listener.onTrackChanged(trackUri);
                }
            });
        }
    }

    /**
     * 添加播放狀態監聽器
     * 添加後會立即收到當前狀態回調（如果有緩存狀態）
     * @param listener 播放狀態監聽器
     */
    public void addPlaybackStateListener(PlaybackStateListener listener) {
        if (listener == null) return;

        if (!playbackStateListeners.contains(listener)) {
            playbackStateListeners.add(listener);
            Log.d(TAG, "添加 PlaybackStateListener，當前數量: " + playbackStateListeners.size());

            // 立即推送緩存的狀態（如果有）
            if (cachedPlayerState != null) {
                handler.post(() -> {
                    boolean isPlaying = !cachedPlayerState.isPaused;
                    long positionMs = cachedPlayerState.playbackPosition;
                    long durationMs = cachedPlayerState.track != null ? cachedPlayerState.track.duration : 0;
                    String trackUri = cachedPlayerState.track != null ? cachedPlayerState.track.uri : null;

                    listener.onPlaybackStateChanged(isPlaying, positionMs, durationMs);
                    if (trackUri != null) {
                        listener.onTrackChanged(trackUri);
                    }
                });
            }
        }
    }

    /**
     * 移除播放狀態監聽器
     * @param listener 播放狀態監聽器
     */
    public void removePlaybackStateListener(PlaybackStateListener listener) {
        if (listener == null) return;

        playbackStateListeners.remove(listener);
        Log.d(TAG, "移除 PlaybackStateListener，當前數量: " + playbackStateListeners.size());
    }

    /**
     * 獲取緩存的播放狀態
     * @return 緩存的 PlayerState，可能為 null
     */
    public PlayerState getCachedPlayerState() {
        return cachedPlayerState;
    }

    /**
     * 緩存當前播放列表
     * 當 PlayerView 設置播放列表時調用，用於後續從 MiniPlayer 導航時獲取完整列表
     *
     * @param playlist 播放列表
     * @param currentIndex 當前播放索引
     */
    public void cachePlaylist(List<MusicItem> playlist, int currentIndex) {
        if (playlist != null && !playlist.isEmpty()) {
            // 創建副本避免外部修改影響緩存
            this.cachedPlaylist = new ArrayList<>(playlist);
            this.cachedPlaylistIndex = currentIndex;
        }
    }

    /**
     * 更新緩存的播放索引
     * 當用戶切換上一首/下一首時調用
     *
     * @param currentIndex 當前播放索引
     */
    public void updateCachedPlaylistIndex(int currentIndex) {
        this.cachedPlaylistIndex = currentIndex;
    }

    /**
     * 獲取緩存的播放列表
     * @return 緩存的播放列表，可能為 null
     */
    public List<MusicItem> getCachedPlaylist() {
        return cachedPlaylist;
    }

    /**
     * 獲取緩存的播放列表當前索引
     * @return 當前播放索引
     */
    public int getCachedPlaylistIndex() {
        return cachedPlaylistIndex;
    }

    /**
     * 檢查是否有緩存的播放列表
     * @return 是否有有效的緩存播放列表
     */
    public boolean hasCachedPlaylist() {
        return cachedPlaylist != null && !cachedPlaylist.isEmpty();
    }

    /**
     * 檢查是否已連接
     */
    public boolean isConnected() {
        return spotifyAppRemote != null && spotifyAppRemote.isConnected();
    }

    /**
     * 檢查是否正在連接
     */
    public boolean isConnecting() {
        return isConnecting;
    }

    /**
     * 格式化 Track URI
     */
    private String formatTrackUri(String trackIdOrUri) {
        if (trackIdOrUri.startsWith("spotify:track:")) {
            return trackIdOrUri;
        }
        return "spotify:track:" + trackIdOrUri;
    }

    /**
     * 解析錯誤訊息
     */
    private String parseErrorMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null) {
            return "連接 Spotify 失敗";
        }

        if (message.contains("SpotifyNotInstalledException") ||
            message.contains("CouldNotFindSpotifyApp")) {
            return "請先安裝 Spotify App";
        } else if (message.contains("UserNotAuthorizedException")) {
            return "請先在 Spotify App 中登錄";
        } else if (message.contains("AUTHENTICATION_SERVICE_UNAVAILABLE")) {
            return "Spotify 授權服務不可用，請打開 Spotify App 後再試";
        } else if (message.contains("NotLoggedInException")) {
            return "請先登錄 Spotify App";
        } else if (message.contains("Explicit user authorization") ||
                   message.contains("user authorization is required")) {
            // 這個錯誤表示 Spotify Developer Dashboard 配置不正確
            // 需要添加應用的 Package Name 和 SHA1 Fingerprint
            return "需要在 Spotify Developer Dashboard 添加應用指紋 (SHA1)";
        } else if (message.contains("offline") || message.contains("Offline")) {
            return "Spotify 離線模式，請確認網絡連接";
        }

        return "連接 Spotify 失敗: " + message;
    }

    // 回調通知輔助方法
    private void notifyConnecting() {
        for (SpotifyPlayerCallback cb : callbacks) {
            cb.onConnecting();
        }
    }

    private void notifyConnected() {
        for (SpotifyPlayerCallback cb : callbacks) {
            cb.onConnected();
        }
    }

    private void notifyConnectionFailed(String errorMessage) {
        for (SpotifyPlayerCallback cb : callbacks) {
            cb.onConnectionFailed(errorMessage);
        }
    }

    private void notifyPlaybackStarted(String trackUri) {
        for (SpotifyPlayerCallback cb : callbacks) {
            cb.onPlaybackStarted(trackUri);
        }
    }

    private void notifyPlaybackPaused() {
        for (SpotifyPlayerCallback cb : callbacks) {
            cb.onPlaybackPaused();
        }
    }

    private void notifyPlaybackResumed() {
        for (SpotifyPlayerCallback cb : callbacks) {
            cb.onPlaybackResumed();
        }
    }

    private void notifyPlayerStateChanged(PlayerState playerState) {
        for (SpotifyPlayerCallback cb : callbacks) {
            cb.onPlayerStateChanged(playerState);
        }
    }

    private void notifySeekCompleted(long positionMs) {
        for (SpotifyPlayerCallback cb : callbacks) {
            cb.onSeekCompleted(positionMs);
        }
    }

    private void notifySeekRestricted() {
        for (SpotifyPlayerCallback cb : callbacks) {
            cb.onSeekRestricted();
        }
    }

    private void notifyTrackPlayRestricted() {
        for (SpotifyPlayerCallback cb : callbacks) {
            cb.onTrackPlayRestricted();
        }
    }

    private void notifyError(String errorMessage) {
        for (SpotifyPlayerCallback cb : callbacks) {
            cb.onError(errorMessage);
        }
    }

    /**
     * 播放狀態監聽器接口
     * 用於 MiniPlayer 等組件監聽播放狀態變化
     */
    public interface PlaybackStateListener {
        /**
         * 播放狀態改變
         * @param isPlaying 是否正在播放
         * @param positionMs 當前播放位置（毫秒）
         * @param durationMs 歌曲總時長（毫秒）
         */
        void onPlaybackStateChanged(boolean isPlaying, long positionMs, long durationMs);

        /**
         * 曲目改變
         * @param trackUri 新曲目的 Spotify URI
         */
        void onTrackChanged(String trackUri);
    }

    /**
     * 回調接口
     */
    public interface SpotifyPlayerCallback {
        /** 正在連接 */
        void onConnecting();

        /** 連接成功 */
        void onConnected();

        /** 連接失敗 */
        void onConnectionFailed(String errorMessage);

        /** 開始播放 */
        void onPlaybackStarted(String trackUri);

        /** 暫停播放 */
        void onPlaybackPaused();

        /** 恢復播放 */
        void onPlaybackResumed();

        /** 播放狀態改變 */
        void onPlayerStateChanged(PlayerState playerState);

        /** 跳轉完成 */
        void onSeekCompleted(long positionMs);

        /** Seek 功能受限（Spotify Free 用戶） */
        void onSeekRestricted();

        /** 歌曲播放受限（Spotify Free 用戶無法點播特定歌曲） */
        void onTrackPlayRestricted();

        /** 發生錯誤 */
        void onError(String errorMessage);
    }
}
