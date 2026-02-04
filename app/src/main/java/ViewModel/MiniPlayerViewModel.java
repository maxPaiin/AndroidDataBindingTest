package ViewModel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.graduationproject.R;

import Model.POJO.MusicItem;
import Model.Repository.FavoriteRepository;
import Model.Spotify.SpotifyPlayerManager;

/**
 * MiniPlayer ViewModel
 * 負責管理迷你播放器的 UI 狀態
 * 遵循 RULES.md 規範：
 * - MVVM First: UI 邏輯與業務邏輯分離
 * - Reactive over Imperative: 使用 LiveData 驅動 UI
 * - 單一職責原則: 只負責 MiniPlayer 狀態管理
 */
public class MiniPlayerViewModel extends AndroidViewModel
        implements SpotifyPlayerManager.PlaybackStateListener {

    private static final String TAG = "MiniPlayerViewModel";

    // 常量：倒計時更新間隔（毫秒）
    private static final long COUNTDOWN_UPDATE_INTERVAL_MS = 1000;

    // LiveData
    private final MutableLiveData<Boolean> _isVisible = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<MusicItem> _currentTrack = new MutableLiveData<>();
    private final MutableLiveData<String> _remainingTimeText = new MutableLiveData<>("0:00");
    private final MutableLiveData<Boolean> _isFavorite = new MutableLiveData<>(false);
    private final MutableLiveData<String> _toastMessage = new MutableLiveData<>();

    public LiveData<Boolean> isVisible = _isVisible;
    public LiveData<Boolean> isPlaying = _isPlaying;
    public LiveData<MusicItem> currentTrack = _currentTrack;
    public LiveData<String> remainingTimeText = _remainingTimeText;
    public LiveData<Boolean> isFavorite = _isFavorite;
    public LiveData<String> toastMessage = _toastMessage;

    // 內部狀態
    private SpotifyPlayerManager playerManager;
    private final FavoriteRepository favoriteRepository;
    private final Handler countdownHandler = new Handler(Looper.getMainLooper());
    private Runnable countdownRunnable;

    // 當前播放位置和時長
    private long currentPositionMs = 0;
    private long currentDurationMs = 0;

    public MiniPlayerViewModel(@NonNull Application application) {
        super(application);
        favoriteRepository = new FavoriteRepository(application);
    }

    /**
     * 綁定到 SpotifyPlayerManager
     * 在 Activity onStart 時調用
     * 如果當前有音樂正在播放，會自動顯示 MiniPlayer
     *
     * @param manager SpotifyPlayerManager 實例
     */
    public void bindToPlayerManager(SpotifyPlayerManager manager) {
        if (manager == null) return;

        this.playerManager = manager;
        manager.addPlaybackStateListener(this);

        // 檢查是否有音樂正在播放，如果有則自動顯示 MiniPlayer
        // 這解決了從 PlayerView 返回時 MiniPlayer 不顯示的問題
        checkAndShowIfPlaying();
    }

    /**
     * 檢查當前是否有音樂播放，如果有則顯示 MiniPlayer
     * 用於頁面返回時自動恢復 MiniPlayer 顯示狀態
     */
    private void checkAndShowIfPlaying() {
        if (playerManager == null) return;

        com.spotify.protocol.types.PlayerState cachedState = playerManager.getCachedPlayerState();
        if (cachedState != null && cachedState.track != null) {
            // 有緩存的播放狀態且有曲目信息，顯示 MiniPlayer
            _isVisible.postValue(true);

            // 同步當前播放狀態
            boolean isPlaying = !cachedState.isPaused;
            _isPlaying.postValue(isPlaying);

            // 更新位置和時長
            this.currentPositionMs = cachedState.playbackPosition;
            this.currentDurationMs = cachedState.track.duration;
            updateRemainingTime();

            // 更新曲目信息
            String trackUri = cachedState.track.uri;
            if (trackUri != null) {
                onTrackChanged(trackUri);
            }

            // 如果正在播放，啟動倒計時
            if (isPlaying) {
                startCountdown();
            }
        }
    }

    /**
     * 解除綁定 SpotifyPlayerManager
     * 在 Activity onStop 時調用
     */
    public void unbindFromPlayerManager() {
        if (playerManager != null) {
            playerManager.removePlaybackStateListener(this);
        }
        stopCountdown();
    }

    // ========== PlaybackStateListener 實現 ==========

    @Override
    public void onPlaybackStateChanged(boolean isPlaying, long positionMs, long durationMs) {
        _isPlaying.postValue(isPlaying);

        // 更新位置和時長
        this.currentPositionMs = positionMs;
        this.currentDurationMs = durationMs;

        // 更新剩餘時間
        updateRemainingTime();

        // 控制倒計時
        if (isPlaying) {
            startCountdown();
        } else {
            stopCountdown();
        }

        // 注意：不再自動顯示 MiniPlayer
        // MiniPlayer 只在用戶透過 App 明確播放時顯示（由 show() 方法控制）
    }

    @Override
    public void onTrackChanged(String trackUri) {
        // 從 trackUri 提取 trackId
        String trackId = extractTrackId(trackUri);

        // 通過 Spotify API 獲取歌曲信息比較複雜，
        // 這裡我們利用 PlayerState 的緩存來獲取基本信息
        if (playerManager != null && playerManager.getCachedPlayerState() != null) {
            com.spotify.protocol.types.PlayerState state = playerManager.getCachedPlayerState();
            if (state.track != null) {
                // 創建 MusicItem
                MusicItem item = new MusicItem();
                item.setSongName(state.track.name);
                item.setArtistName(state.track.artist.name);
                item.setSpotifyTrackId(trackId);
                item.setDurationMs(state.track.duration);

                // 獲取專輯封面 URL
                // Spotify SDK 的 ImageUri.raw 是 "spotify:image:xxx" 格式，不是 HTTP URL
                // 需要轉換為 Spotify CDN URL 格式
                if (state.track.imageUri != null && state.track.imageUri.raw != null) {
                    String imageUri = state.track.imageUri.raw;
                    // 轉換 spotify:image:xxx 為 https://i.scdn.co/image/xxx
                    if (imageUri.startsWith("spotify:image:")) {
                        String imageId = imageUri.substring("spotify:image:".length());
                        item.setAlbumImageUrl("https://i.scdn.co/image/" + imageId);
                    } else {
                        item.setAlbumImageUrl(imageUri);
                    }
                }

                _currentTrack.postValue(item);

                // 檢查收藏狀態
                checkFavoriteStatus(trackId);
            }
        }
    }

    // ========== 公開方法 ==========

    /**
     * 切換播放/暫停狀態
     */
    public void togglePlayPause() {
        if (playerManager == null || !playerManager.isConnected()) {
            _toastMessage.postValue(getApplication().getString(R.string.toast_spotify_not_connected));
            return;
        }

        Boolean playing = _isPlaying.getValue();
        if (playing != null && playing) {
            playerManager.pause();
        } else {
            playerManager.resume();
        }
    }

    /**
     * 切換收藏狀態
     */
    public void toggleFavorite() {
        MusicItem track = _currentTrack.getValue();
        if (track == null || track.getSpotifyTrackId() == null) {
            return;
        }

        Boolean currentStatus = _isFavorite.getValue();
        if (currentStatus != null && currentStatus) {
            // 已收藏 -> 移除
            favoriteRepository.removeFavorite(track.getSpotifyTrackId(), () -> {
                _isFavorite.postValue(false);
                _toastMessage.postValue(getApplication().getString(R.string.toast_removed_from_favorites));
            });
        } else {
            // 未收藏 -> 添加
            favoriteRepository.addFavorite(track, () -> {
                _isFavorite.postValue(true);
                _toastMessage.postValue(getApplication().getString(R.string.toast_added_to_favorites));
            });
        }
    }

    /**
     * 顯示 MiniPlayer
     * 只在用戶透過 App 明確播放音樂時調用
     */
    public void show() {
        _isVisible.postValue(true);
    }

    /**
     * 隱藏 MiniPlayer
     */
    public void hide() {
        _isVisible.postValue(false);
    }

    /**
     * 清除 Toast 訊息
     */
    public void clearToastMessage() {
        _toastMessage.setValue(null);
    }

    /**
     * 獲取當前歌曲的 Spotify Track ID
     * @return Track ID，可能為 null
     */
    public String getCurrentTrackId() {
        MusicItem track = _currentTrack.getValue();
        return track != null ? track.getSpotifyTrackId() : null;
    }

    /**
     * 檢查收藏狀態
     */
    private void checkFavoriteStatus(String trackId) {
        if (trackId == null) {
            _isFavorite.postValue(false);
            return;
        }
        favoriteRepository.checkIsFavorite(trackId, result -> {
            _isFavorite.postValue(result);
        });
    }

    /**
     * 更新剩餘時間顯示
     */
    private void updateRemainingTime() {
        long remainingMs = currentDurationMs - currentPositionMs;
        if (remainingMs < 0) {
            remainingMs = 0;
        }
        _remainingTimeText.postValue(formatTime(remainingMs));
    }

    /**
     * 開始倒計時
     * 每秒更新剩餘時間
     */
    private void startCountdown() {
        stopCountdown();

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                // 每秒增加 1000ms
                currentPositionMs += COUNTDOWN_UPDATE_INTERVAL_MS;
                if (currentPositionMs > currentDurationMs) {
                    currentPositionMs = currentDurationMs;
                }

                updateRemainingTime();

                // 如果還在播放且沒到結尾，繼續倒計時
                Boolean playing = _isPlaying.getValue();
                if (playing != null && playing && currentPositionMs < currentDurationMs) {
                    countdownHandler.postDelayed(this, COUNTDOWN_UPDATE_INTERVAL_MS);
                }
            }
        };

        countdownHandler.postDelayed(countdownRunnable, COUNTDOWN_UPDATE_INTERVAL_MS);
    }

    /**
     * 停止倒計時
     */
    private void stopCountdown() {
        if (countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
            countdownRunnable = null;
        }
    }

    /**
     * 格式化時間為 mm:ss 格式
     * @param milliseconds 毫秒數
     * @return 格式化字串
     */
    private String formatTime(long milliseconds) {
        if (milliseconds <= 0) {
            return "0:00";
        }
        long totalSeconds = milliseconds / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * 從 Spotify URI 提取 Track ID
     * @param uri spotify:track:xxx 格式
     * @return Track ID
     */
    private String extractTrackId(String uri) {
        if (uri == null) return null;
        if (uri.startsWith("spotify:track:")) {
            return uri.substring("spotify:track:".length());
        }
        return uri;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // 清理資源
        stopCountdown();
        if (playerManager != null) {
            playerManager.removePlaybackStateListener(this);
        }
        favoriteRepository.shutdown();
    }
}
