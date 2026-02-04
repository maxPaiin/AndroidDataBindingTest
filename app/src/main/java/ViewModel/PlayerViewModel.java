package ViewModel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.graduationproject.R;
import com.spotify.protocol.types.PlayerState;

import java.util.List;

import Model.POJO.MusicItem;
import Model.Repository.FavoriteRepository;
import Model.Spotify.SpotifyPlayerManager;
import Util.CountdownTimer;

/**
 * 播放器 ViewModel
 * 負責管理播放狀態、歌單導航和倒計時邏輯
 */
public class PlayerViewModel extends AndroidViewModel {

    // LiveData - 當前歌曲
    private final MutableLiveData<MusicItem> _currentTrack = new MutableLiveData<>();
    public LiveData<MusicItem> currentTrack = _currentTrack;

    // LiveData - 剩餘時間（格式化）
    private final MutableLiveData<String> _remainingTime = new MutableLiveData<>("0:00");
    public LiveData<String> remainingTime = _remainingTime;

    // LiveData - 播放狀態
    private final MutableLiveData<Boolean> _isPlaying = new MutableLiveData<>(false);
    public LiveData<Boolean> isPlaying = _isPlaying;

    // LiveData - 播放/暫停按鈕文字
    private final MutableLiveData<String> _playPauseText = new MutableLiveData<>("播放");
    public LiveData<String> playPauseText = _playPauseText;

    // LiveData - 連接狀態訊息
    private final MutableLiveData<String> _statusMessage = new MutableLiveData<>();
    public LiveData<String> statusMessage = _statusMessage;

    // LiveData - 控制按鈕是否可用
    private final MutableLiveData<Boolean> _controlsEnabled = new MutableLiveData<>(false);
    public LiveData<Boolean> controlsEnabled = _controlsEnabled;

    // LiveData - 收藏狀態
    private final MutableLiveData<Boolean> _isFavorite = new MutableLiveData<>(false);
    public LiveData<Boolean> isFavorite = _isFavorite;

    // LiveData - Toast 訊息
    private final MutableLiveData<String> _toastMessage = new MutableLiveData<>();
    public LiveData<String> toastMessage = _toastMessage;

    // LiveData - 當前播放位置（毫秒）
    private final MutableLiveData<Long> _currentPosition = new MutableLiveData<>(0L);
    public LiveData<Long> currentPosition = _currentPosition;

    // LiveData - 歌曲總時長（毫秒）
    private final MutableLiveData<Long> _totalDuration = new MutableLiveData<>(0L);
    public LiveData<Long> totalDuration = _totalDuration;

    // LiveData - 用戶是否正在拖動 SeekBar
    private final MutableLiveData<Boolean> _isUserSeeking = new MutableLiveData<>(false);
    public LiveData<Boolean> isUserSeeking = _isUserSeeking;

    // LiveData - 當前時間格式化字串
    private final MutableLiveData<String> _currentTimeFormatted = new MutableLiveData<>("0:00");
    public LiveData<String> currentTimeFormatted = _currentTimeFormatted;

    // LiveData - 總時長格式化字串
    private final MutableLiveData<String> _totalTimeFormatted = new MutableLiveData<>("0:00");
    public LiveData<String> totalTimeFormatted = _totalTimeFormatted;

    // LiveData - Seek 功能受限訊息（Spotify Free 用戶）
    private final MutableLiveData<String> _seekRestrictedMessage = new MutableLiveData<>();
    public LiveData<String> seekRestrictedMessage = _seekRestrictedMessage;

    // LiveData - 是否可以切換上一首/下一首（播放列表需有多首歌曲）
    private final MutableLiveData<Boolean> _canNavigate = new MutableLiveData<>(false);
    public LiveData<Boolean> canNavigate = _canNavigate;

    // 歌單數據
    private List<MusicItem> playlist;
    private int currentIndex = 0;

    // 倒計時
    private final CountdownTimer countdownTimer;

    // Spotify 播放器管理
    private final SpotifyPlayerManager playerManager;

    // 收藏 Repository
    private final FavoriteRepository favoriteRepository;

    // 進度更新用 Handler
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    // 常量：進度更新間隔（毫秒）
    private static final long PROGRESS_UPDATE_INTERVAL_MS = 1000;

    // Seek 操作的待處理數據
    private long pendingSeekPosition = -1;
    private Long pendingSeekDuration = null;

    // 標記是否需要在連接後播放新曲目
    // true = 播放新曲目（從頭開始），false = 保持當前播放狀態
    private boolean shouldPlayNewTrackOnConnect = false;

    public PlayerViewModel(@NonNull Application application) {
        super(application);

        favoriteRepository = new FavoriteRepository(application);

        countdownTimer = new CountdownTimer();
        countdownTimer.setListener(new CountdownTimer.OnCountdownListener() {
            @Override
            public void onTick(long remainingMs, String formattedTime) {
                _remainingTime.postValue(formattedTime);
            }

            @Override
            public void onFinish() {
                // 倒計時結束，自動播放下一首
                playNext();
            }
        });

        playerManager = SpotifyPlayerManager.getInstance();
        playerManager.setCallback(new SpotifyPlayerManager.SpotifyPlayerCallback() {
            @Override
            public void onConnecting() {
                _statusMessage.postValue(getApplication().getString(R.string.status_connecting_spotify));
            }

            @Override
            public void onConnected() {
                _statusMessage.postValue(getApplication().getString(R.string.status_connected));
                _controlsEnabled.postValue(true);

                // 根據 shouldPlayNewTrackOnConnect 標記決定播放行為
                if (shouldPlayNewTrackOnConnect) {
                    // 不同歌曲：從頭播放新曲目
                    playCurrentTrack();
                    shouldPlayNewTrackOnConnect = false;
                } else {
                    // 同一首歌：同步當前播放狀態，啟動進度更新
                    syncWithCurrentPlaybackState();
                    Boolean playing = _isPlaying.getValue();
                    if (playing != null && playing) {
                        startProgressUpdates();
                    }
                }
            }

            @Override
            public void onConnectionFailed(String errorMessage) {
                _statusMessage.postValue(errorMessage);
                _controlsEnabled.postValue(false);
            }

            @Override
            public void onPlaybackStarted(String trackUri) {
                _isPlaying.postValue(true);
                _playPauseText.postValue(getApplication().getString(R.string.player_pause));
                // 啟動倒計時
                MusicItem current = _currentTrack.getValue();
                if (current != null && current.getDurationMs() > 0) {
                    countdownTimer.start(current.getDurationMs());
                }
            }

            @Override
            public void onPlaybackPaused() {
                _isPlaying.postValue(false);
                _playPauseText.postValue(getApplication().getString(R.string.player_play));
                countdownTimer.pause();
            }

            @Override
            public void onPlaybackResumed() {
                _isPlaying.postValue(true);
                _playPauseText.postValue(getApplication().getString(R.string.player_pause));
                countdownTimer.resume();
            }

            @Override
            public void onPlayerStateChanged(PlayerState playerState) {
                if (playerState != null) {
                    boolean isPaused = playerState.isPaused;
                    _isPlaying.postValue(!isPaused);
                    _playPauseText.postValue(isPaused ? getApplication().getString(R.string.player_play) : getApplication().getString(R.string.player_pause));

                    // 更新播放進度（只在非用戶拖動時更新）
                    Boolean isSeeking = _isUserSeeking.getValue();
                    if (isSeeking == null || !isSeeking) {
                        long position = playerState.playbackPosition;
                        _currentPosition.postValue(position);
                        _currentTimeFormatted.postValue(formatTime(position));
                    }

                    // 更新總時長
                    if (playerState.track != null) {
                        long duration = playerState.track.duration;
                        _totalDuration.postValue(duration);
                        _totalTimeFormatted.postValue(formatTime(duration));
                    }
                }
            }

            @Override
            public void onSeekCompleted(long positionMs) {
                // 跳轉完成，更新位置
                _currentPosition.postValue(positionMs);
                _currentTimeFormatted.postValue(formatTime(positionMs));
                // 同步倒計時器
                syncCountdownAfterSeek();
            }

            @Override
            public void onSeekRestricted() {
                // Spotify Free 用戶無法使用 快進/快退 功能
                _seekRestrictedMessage.postValue(getApplication().getString(R.string.error_seek_restricted));
                // 重置 pending 狀態
                pendingSeekPosition = -1;
                pendingSeekDuration = null;
            }

            @Override
            public void onTrackPlayRestricted() {
                // Spotify Free 用戶無法點播特定歌曲
                _toastMessage.postValue(getApplication().getString(R.string.toast_spotify_free_cannot_play_on_demand));
            }

            @Override
            public void onError(String errorMessage) {
                _statusMessage.postValue(errorMessage);
            }
        });
    }

    /**
     * 設置歌單並決定播放行為
     * 如果目標歌曲與當前播放的歌曲相同，則保持播放進度
     * 如果是不同歌曲，則標記需要從頭播放
     *
     * @param items 歌單列表
     * @param startIndex 開始播放的索引
     */
    public void setPlaylist(List<MusicItem> items, int startIndex) {
        this.playlist = items;
        this.currentIndex = startIndex;

        if (playlist != null && !playlist.isEmpty()) {
            // 確保索引有效
            if (currentIndex < 0 || currentIndex >= playlist.size()) {
                currentIndex = 0;
            }

            // 緩存播放列表到 SpotifyPlayerManager，用於 MiniPlayer 導航時獲取
            playerManager.cachePlaylist(playlist, currentIndex);

            // 更新導航可用狀態（播放列表需有多首歌曲才能切換）
            _canNavigate.postValue(playlist.size() > 1);

            MusicItem targetTrack = playlist.get(currentIndex);
            String targetTrackUri = targetTrack.getSpotifyUri();

            // 檢查目標歌曲是否與當前正在播放的歌曲相同
            String currentlyPlayingUri = getCurrentlyPlayingTrackUri();
            boolean isSameTrack = targetTrackUri != null && targetTrackUri.equals(currentlyPlayingUri);

            if (isSameTrack) {
                // 同一首歌：保持當前播放狀態，同步 UI
                shouldPlayNewTrackOnConnect = false;
                syncWithCurrentPlaybackState();
            } else {
                // 不同歌曲：標記需要從頭播放
                shouldPlayNewTrackOnConnect = true;
            }

            updateCurrentTrack();
        }
    }

    /**
     * 獲取當前正在播放的歌曲 URI
     * 從 SpotifyPlayerManager 的緩存狀態中讀取
     *
     * @return 當前播放的歌曲 URI，如果沒有則返回 null
     */
    private String getCurrentlyPlayingTrackUri() {
        PlayerState cachedState = playerManager.getCachedPlayerState();
        if (cachedState != null && cachedState.track != null) {
            return cachedState.track.uri;
        }
        return null;
    }

    /**
     * 同步 UI 與當前 Spotify 播放狀態
     * 用於進入 PlayerView 時，目標歌曲與正在播放的歌曲相同的情況
     */
    private void syncWithCurrentPlaybackState() {
        PlayerState cachedState = playerManager.getCachedPlayerState();
        if (cachedState != null) {
            // 同步播放狀態
            boolean isPlaying = !cachedState.isPaused;
            _isPlaying.postValue(isPlaying);
            _playPauseText.postValue(isPlaying ? getApplication().getString(R.string.player_pause) : getApplication().getString(R.string.player_play));

            // 同步播放進度
            _currentPosition.postValue(cachedState.playbackPosition);
            _currentTimeFormatted.postValue(formatTime(cachedState.playbackPosition));

            // 同步總時長
            if (cachedState.track != null) {
                long duration = cachedState.track.duration;
                _totalDuration.postValue(duration);
                _totalTimeFormatted.postValue(formatTime(duration));
            }
        }
    }

    /**
     * 連接 Spotify 並開始播放
     */
    public void connectAndPlay() {
        playerManager.connect(getApplication());
    }

    /**
     * 播放當前曲目
     */
    public void playCurrentTrack() {
        MusicItem current = _currentTrack.getValue();
        if (current == null) return;

        String uri = current.getSpotifyUri();
        if (uri != null) {
            playerManager.playTrack(uri);
        }
    }

    /**
     * 暫停播放
     */
    public void pauseTrack() {
        playerManager.pause();
    }

    /**
     * 恢復播放
     */
    public void resumeTrack() {
        playerManager.resume();
    }

    /**
     * 切換播放/暫停狀態
     */
    public void togglePlayPause() {
        Boolean playing = _isPlaying.getValue();
        if (playing != null && playing) {
            pauseTrack();
        } else {
            if (playerManager.isConnected()) {
                resumeTrack();
            } else {
                connectAndPlay();
            }
        }
    }

    /**
     * 播放下一首
     * 循環播放：最後一首 → 第一首
     * 如果播放列表只有一首歌，則不執行任何操作
     */
    public void playNext() {
        if (playlist == null || playlist.isEmpty()) return;

        // 如果播放列表只有一首歌，顯示提示並返回
        if (playlist.size() == 1) {
            _toastMessage.postValue(getApplication().getString(R.string.toast_playlist_only_one_song));
            return;
        }

        currentIndex = (currentIndex + 1) % playlist.size();
        MusicItem nextTrack = playlist.get(currentIndex);

        // 更新緩存的播放索引
        playerManager.updateCachedPlaylistIndex(currentIndex);

        // 更新 UI
        _currentTrack.postValue(nextTrack);
        updateTrackTimeDisplay(nextTrack);

        // 直接播放，避免競態條件
        playTrackDirectly(nextTrack);
    }

    /**
     * 播放上一首
     * 循環播放：第一首 → 最後一首
     * 如果播放列表只有一首歌，則不執行任何操作
     */
    public void playPrevious() {
        if (playlist == null || playlist.isEmpty()) return;

        // 如果播放列表只有一首歌，顯示提示並返回
        if (playlist.size() == 1) {
            _toastMessage.postValue(getApplication().getString(R.string.toast_playlist_only_one_song));
            return;
        }

        currentIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
        MusicItem prevTrack = playlist.get(currentIndex);

        // 更新緩存的播放索引
        playerManager.updateCachedPlaylistIndex(currentIndex);

        // 更新 UI
        _currentTrack.postValue(prevTrack);
        updateTrackTimeDisplay(prevTrack);

        // 直接播放，避免競態條件
        playTrackDirectly(prevTrack);
    }

    /**
     * 直接播放指定曲目（不依賴 LiveData）
     * @param track 要播放的曲目
     */
    private void playTrackDirectly(MusicItem track) {
        if (track == null) return;

        String uri = track.getSpotifyUri();
        if (uri != null) {
            playerManager.playTrack(uri);
        }
    }

    /**
     * 更新曲目時間顯示
     * @param track 曲目
     */
    private void updateTrackTimeDisplay(MusicItem track) {
        if (track != null && track.getDurationMs() > 0) {
            _remainingTime.postValue(CountdownTimer.formatTime(track.getDurationMs()));
        } else {
            _remainingTime.postValue("0:00");
        }
    }

    /**
     * 更新當前曲目 LiveData
     */
    private void updateCurrentTrack() {
        if (playlist != null && currentIndex >= 0 && currentIndex < playlist.size()) {
            MusicItem item = playlist.get(currentIndex);
            _currentTrack.postValue(item);
            updateTrackTimeDisplay(item);
        }
    }

    /**
     * 獲取 SpotifyPlayerManager
     * 用於 View 層進行連接等操作
     */
    public SpotifyPlayerManager getPlayerManager() {
        return playerManager;
    }

    /**
     * 獲取當前歌曲索引
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * 獲取歌單大小
     */
    public int getPlaylistSize() {
        return playlist != null ? playlist.size() : 0;
    }

    /**
     * 檢查當前歌曲的收藏狀態
     */
    public void checkFavoriteStatus(String trackId) {
        if (trackId == null) {
            _isFavorite.postValue(false);
            return;
        }
        favoriteRepository.checkIsFavorite(trackId, result -> {
            _isFavorite.postValue(result);
        });
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
     * 清除 Toast 訊息
     */
    public void clearToastMessage() {
        _toastMessage.setValue(null);
    }

    /**
     * 清除 Seek 功能受限訊息
     */
    public void clearSeekRestrictedMessage() {
        _seekRestrictedMessage.setValue(null);
    }

    /**
     * 跳轉到指定播放位置
     * @param positionMs 目標位置（毫秒）
     */
    public void seekTo(long positionMs) {
        // 檢查連接狀態
        if (!playerManager.isConnected()) {
//            _toastMessage.postValue("Spotify 未連接，請稍候再試");
            return;
        }

        // 保存目標位置和時長，用於回調中同步倒計時
        pendingSeekPosition = positionMs;
        pendingSeekDuration = _totalDuration.getValue();

        playerManager.seekTo(positionMs);
    }

    /**
     * Seek 完成後同步倒計時器（由回調調用）
     */
    private void syncCountdownAfterSeek() {
        if (pendingSeekPosition >= 0 && pendingSeekDuration != null && pendingSeekDuration > 0) {
            long remaining = pendingSeekDuration - pendingSeekPosition;
            countdownTimer.stop();
            countdownTimer.start(remaining);

            // 如果當前是暫停狀態，暫停倒計時
            Boolean playing = _isPlaying.getValue();
            if (playing == null || !playing) {
                countdownTimer.pause();
            }
        }
        // 重置
        pendingSeekPosition = -1;
        pendingSeekDuration = null;
    }

    /**
     * 設置用戶是否正在拖動 SeekBar
     * 拖動時暫停自動更新進度，避免 UI 抖動
     * @param isSeeking 是否正在拖動
     */
    public void setUserSeeking(boolean isSeeking) {
        // 使用 postValue 確保線程安全
        _isUserSeeking.postValue(isSeeking);
    }

    /**
     * 更新當前播放位置（用戶拖動時的預覽）
     * @param positionMs 預覽位置（毫秒）
     */
    public void updateSeekPosition(long positionMs) {
        _currentPosition.setValue(positionMs);
        _currentTimeFormatted.setValue(formatTime(positionMs));
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
     * 開始定時更新進度
     * 用於主動輪詢 Spotify 播放狀態
     */
    public void startProgressUpdates() {
        stopProgressUpdates();
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                // 只在播放中且非拖動時請求更新
                Boolean playing = _isPlaying.getValue();
                Boolean seeking = _isUserSeeking.getValue();
                if (playing != null && playing && (seeking == null || !seeking)) {
                    playerManager.getPlayerState();
                }
                progressHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS);
            }
        };
        progressHandler.post(progressRunnable);
    }

    /**
     * 停止定時更新進度
     */
    public void stopProgressUpdates() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopProgressUpdates();
        countdownTimer.stop();
        // 單例模式下不在此處斷開連接，由 Application 生命週期管理
        // playerManager.disconnect();
        favoriteRepository.shutdown();
    }
}
