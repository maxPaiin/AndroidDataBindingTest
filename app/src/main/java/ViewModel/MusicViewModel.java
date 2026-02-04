package ViewModel;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.graduationproject.R;
import com.spotify.protocol.types.PlayerState;

import java.util.ArrayList;
import java.util.List;

import Model.POJO.EmotionInput;
import Model.POJO.MusicItem;
import Model.Repository.FavoriteRepository;
import Model.Repository.MusicRepository;
import Model.Spotify.SpotifyPlayerManager;
import Util.TokenManager;

/**
 * 音樂 ViewModel
 * 處理音樂推薦的業務邏輯和 Spotify 播放控制
 * 遵循 MVVM 原則，作為 View 和 Model 之間的橋樑
 */
public class MusicViewModel extends AndroidViewModel {

    // 音樂列表狀態
    private final MutableLiveData<List<MusicItem>> _musicList = new MutableLiveData<>();
    public LiveData<List<MusicItem>> musicList = _musicList;

    // 加載狀態
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    // 錯誤訊息
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> errorMessage = _errorMessage;

    // 狀態訊息
    private final MutableLiveData<String> _statusMessage = new MutableLiveData<>();
    public LiveData<String> statusMessage = _statusMessage;

    // Spotify 連接狀態
    private final MutableLiveData<Boolean> _isSpotifyConnected = new MutableLiveData<>(false);
    public LiveData<Boolean> isSpotifyConnected = _isSpotifyConnected;

    // Spotify 正在連接狀態
    private final MutableLiveData<Boolean> _isSpotifyConnecting = new MutableLiveData<>(false);
    public LiveData<Boolean> isSpotifyConnecting = _isSpotifyConnecting;

    // 當前播放曲目 ID
    private final MutableLiveData<String> _currentPlayingTrackId = new MutableLiveData<>();
    public LiveData<String> currentPlayingTrackId = _currentPlayingTrackId;

    // 播放狀態
    private final MutableLiveData<Boolean> _isPlaying = new MutableLiveData<>(false);
    public LiveData<Boolean> isPlaying = _isPlaying;

    // Toast 訊息（用於收藏操作反饋）
    private final MutableLiveData<String> _toastMessage = new MutableLiveData<>();
    public LiveData<String> toastMessage = _toastMessage;

    // ===== 新增功能1、3 相關 LiveData =====

    // btn_list_bad 冷卻剩餘時間（秒）
    private final MutableLiveData<Integer> _cooldownSeconds = new MutableLiveData<>(0);
    public LiveData<Integer> cooldownSeconds = _cooldownSeconds;

    // btn_list_bad 是否可用
    private final MutableLiveData<Boolean> _isRefreshEnabled = new MutableLiveData<>(true);
    public LiveData<Boolean> isRefreshEnabled = _isRefreshEnabled;

    // 是否顯示直接輸入模式（TextInputLayout）
    private final MutableLiveData<Boolean> _isDirectInputMode = new MutableLiveData<>(false);
    public LiveData<Boolean> isDirectInputMode = _isDirectInputMode;

    // 是否顯示詢問對話框
    private final MutableLiveData<Boolean> _showDirectInputDialog = new MutableLiveData<>(false);
    public LiveData<Boolean> showDirectInputDialog = _showDirectInputDialog;

    // 是否已獲取歌單（用於控制 UI 狀態切換：情緒輸入階段 -> 歌單顯示階段）
    private final MutableLiveData<Boolean> _hasPlaylist = new MutableLiveData<>(false);
    public LiveData<Boolean> hasPlaylist = _hasPlaylist;

    // btn_list_bad 點擊計數器
    private int badClickCount = 0;

    // 上次情緒輸入值（用於刷新）
    private int lastHappy = 0, lastSad = 0, lastAngry = 0, lastDisgust = 0, lastFear = 0;

    // 用於防止重複顯示 Toast
    private boolean hasShownConnectedToast = false;

    // 冷卻計時 Handler
    private Handler cooldownHandler;
    private Runnable cooldownRunnable;

    private final MusicRepository musicRepository;
    private final FavoriteRepository favoriteRepository;
    private final TokenManager tokenManager;
    private final Handler mainHandler;
    private final SpotifyPlayerManager spotifyPlayerManager;

    public MusicViewModel(@NonNull Application application) {
        super(application);
        this.musicRepository = new MusicRepository();
        this.favoriteRepository = new FavoriteRepository(application);
        this.tokenManager = new TokenManager(application);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.cooldownHandler = new Handler(Looper.getMainLooper());
        this.spotifyPlayerManager = SpotifyPlayerManager.getInstance();

        setupSpotifyPlayerCallback();
    }

    /**
     * 設置 Spotify 播放器回調
     */
    private void setupSpotifyPlayerCallback() {
        spotifyPlayerManager.setCallback(new SpotifyPlayerManager.SpotifyPlayerCallback() {
            @Override
            public void onConnecting() {
                mainHandler.post(() -> {
                    _isSpotifyConnecting.setValue(true);
                    _statusMessage.setValue(getApplication().getString(R.string.status_connecting_spotify));
                });
            }

            @Override
            public void onConnected() {
                mainHandler.post(() -> {
                    _isSpotifyConnecting.setValue(false);
                    // 只在首次連接時顯示 Toast，避免頁面切換時重複顯示
                    boolean wasConnected = Boolean.TRUE.equals(_isSpotifyConnected.getValue());
                    _isSpotifyConnected.setValue(true);
                    if (!wasConnected && !hasShownConnectedToast) {
                        _statusMessage.setValue(getApplication().getString(R.string.status_connected_spotify));
                        hasShownConnectedToast = true;
                    }
                });
            }

            @Override
            public void onConnectionFailed(String errorMessage) {
                mainHandler.post(() -> {
                    _isSpotifyConnecting.setValue(false);
                    _isSpotifyConnected.setValue(false);
                    _errorMessage.setValue(errorMessage);
                });
            }

            @Override
            public void onPlaybackStarted(String trackUri) {
                mainHandler.post(() -> {
                    String trackId = extractTrackId(trackUri);
                    _currentPlayingTrackId.setValue(trackId);
                    _isPlaying.setValue(true);
                    updateMusicItemPlayingState(trackId, true);
                });
            }

            @Override
            public void onPlaybackPaused() {
                mainHandler.post(() -> {
                    _isPlaying.setValue(false);
                    String currentTrackId = _currentPlayingTrackId.getValue();
                    if (currentTrackId != null) {
                        updateMusicItemPlayingState(currentTrackId, false);
                    }
                });
            }

            @Override
            public void onPlaybackResumed() {
                mainHandler.post(() -> {
                    _isPlaying.setValue(true);
                    String currentTrackId = _currentPlayingTrackId.getValue();
                    if (currentTrackId != null) {
                        updateMusicItemPlayingState(currentTrackId, true);
                    }
                });
            }

            @Override
            public void onPlayerStateChanged(PlayerState playerState) {
                mainHandler.post(() -> {
                    if (playerState != null && playerState.track != null) {
                        String trackUri = playerState.track.uri;
                        String trackId = extractTrackId(trackUri);
                        boolean isPaused = playerState.isPaused;

                        _currentPlayingTrackId.setValue(trackId);
                        _isPlaying.setValue(!isPaused);
                        updateMusicItemPlayingState(trackId, !isPaused);
                    }
                });
            }

            @Override
            public void onSeekCompleted(long positionMs) {

            }

            @Override
            public void onSeekRestricted() {

            }

            @Override
            public void onTrackPlayRestricted() {
                mainHandler.post(() -> _errorMessage.setValue(getApplication().getString(R.string.error_spotify_free_cannot_play)));
            }

            @Override
            public void onError(String errorMessage) {
                mainHandler.post(() -> _errorMessage.setValue(errorMessage));
            }
        });
    }

    /**
     * 連接到 Spotify App Remote
     */
    public void connectSpotify(Context context) {
        spotifyPlayerManager.connect(context);
    }

    /**
     * 先打開 Spotify App，然後連接
     * 用於解決 AUTHENTICATION_SERVICE_UNAVAILABLE 錯誤
     */
    public void openSpotifyAndConnect(Context context) {
        spotifyPlayerManager.openSpotifyAndConnect(context);
    }

    /**
     * 斷開 Spotify 連接
     */
    public void disconnectSpotify() {
        spotifyPlayerManager.disconnect();
        _isSpotifyConnected.setValue(false);
        _isSpotifyConnecting.setValue(false);
    }

    /**
     * 檢查 Spotify 是否已安裝
     */
    public boolean isSpotifyInstalled(Context context) {
        return spotifyPlayerManager.isSpotifyInstalled(context);
    }

    /**
     * 播放指定曲目
     */
    public void playTrack(MusicItem item) {
        if (item == null || item.getSpotifyTrackId() == null) {
            _errorMessage.setValue(getApplication().getString(R.string.error_cannot_play_music));
            return;
        }

        String trackUri = item.getSpotifyUri();
        spotifyPlayerManager.playTrack(trackUri);
    }

    /**
     * 暫停播放
     */
    public void pausePlayback() {
        spotifyPlayerManager.pause();
    }

    /**
     * 恢復播放
     */
    public void resumePlayback() {
        spotifyPlayerManager.resume();
    }

    /**
     * 獲取 SpotifyPlayerManager 實例
     */
    public SpotifyPlayerManager getSpotifyPlayerManager() {
        return spotifyPlayerManager;
    }

    /**
     * 從 URI 中提取 Track ID
     */
    private String extractTrackId(String uri) {
        if (uri == null) return null;
        if (uri.startsWith("spotify:track:")) {
            return uri.substring("spotify:track:".length());
        }
        return uri;
    }

    /**
     * 更新音樂項目的播放狀態
     */
    private void updateMusicItemPlayingState(String trackId, boolean isPlaying) {
        List<MusicItem> currentList = _musicList.getValue();
        if (currentList == null) return;

        List<MusicItem> updatedList = new ArrayList<>();
        for (MusicItem item : currentList) {
            item.setPlaying(false);
            if (item.getSpotifyTrackId() != null && item.getSpotifyTrackId().equals(trackId)) {
                item.setPlaying(isPlaying);
            }
            updatedList.add(item);
        }
        _musicList.setValue(updatedList);
    }

    /**
     * 根據情緒獲取音樂推薦
     */
    public void fetchMusicRecommendations(int happy, int sad, int angry, int disgust, int fear) {
        if (!validateInput(happy, sad, angry, disgust, fear)) {
            _errorMessage.setValue(getApplication().getString(R.string.error_invalid_emotion_input));
            return;
        }

        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            _errorMessage.setValue(getApplication().getString(R.string.error_please_login_spotify));
            return;
        }

        _isLoading.setValue(true);
        _statusMessage.setValue(getApplication().getString(R.string.status_analyzing_emotions));
        _errorMessage.setValue(null);

        EmotionInput emotionInput = new EmotionInput(happy, sad, angry, disgust, fear);

        musicRepository.getMusicRecommendations(emotionInput, accessToken,
                new MusicRepository.MusicRepositoryCallback() {
                    @Override
                    public void onSuccess(List<MusicItem> musicItems) {
                        mainHandler.post(() -> {
                            _isLoading.setValue(false);
                            _musicList.setValue(musicItems);
                            _statusMessage.setValue(getApplication().getString(R.string.status_found_music, musicItems.size()));
                            _hasPlaylist.setValue(true);
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        mainHandler.post(() -> {
                            _isLoading.setValue(false);
                            _errorMessage.setValue(errorMessage);
                            // 檢測是否為 API 限流錯誤
                            if (errorMessage != null && (errorMessage.contains("limit") ||
                                errorMessage.contains("quota") || errorMessage.contains("RATE_LIMIT"))) {
                                _statusMessage.setValue(getApplication().getString(R.string.status_request_limit_reached));
                            } else {
                                _statusMessage.setValue(getApplication().getString(R.string.status_failed_to_get_recommendations));
                            }
                        });
                    }
                });
    }

    private boolean validateInput(int happy, int sad, int angry, int disgust, int fear) {
        return isValidEmotionValue(happy) &&
               isValidEmotionValue(sad) &&
               isValidEmotionValue(angry) &&
               isValidEmotionValue(disgust) &&
               isValidEmotionValue(fear);
    }

    private boolean isValidEmotionValue(int value) {
        return value >= 0 && value <= 100;
    }

    public void clearError() {
        _errorMessage.setValue(null);
    }

    public void clearMusicList() {
        _musicList.setValue(null);
    }

    /**
     * 切換收藏狀態
     */
    public void toggleFavorite(MusicItem item) {
        if (item == null || item.getSpotifyTrackId() == null) {
            return;
        }

        favoriteRepository.checkIsFavorite(item.getSpotifyTrackId(), isFav -> {
            if (isFav) {
                favoriteRepository.removeFavorite(item.getSpotifyTrackId(), () -> {
                    _toastMessage.postValue(getApplication().getString(R.string.toast_removed_from_favorites));
                });
            } else {
                favoriteRepository.addFavorite(item, () -> {
                    _toastMessage.postValue(getApplication().getString(R.string.toast_added_to_favorites));
                });
            }
        });
    }

    public void clearToastMessage() {
        _toastMessage.setValue(null);
    }

    /**
     * 處理「不喜歡」按鈕點擊
     * 包含冷卻時間檢查和點擊計數邏輯
     * @return true 表示已執行刷新，false 表示在冷卻中無法刷新
     */
    public boolean handleBadListClick() {
        // 檢查是否在冷卻中
        if (!Boolean.TRUE.equals(_isRefreshEnabled.getValue())) {
            return false;
        }

        badClickCount++;
        if (badClickCount >= 3) {
            _showDirectInputDialog.setValue(true);
            badClickCount = 0;
        } else {
            refreshRecommendations();
        }
        return true;
    }

    /**
     * @deprecated 使用 {@link #handleBadListClick()} 代替
     */
    @Deprecated
    public void onBadClick() {
        handleBadListClick();
    }

    public void refreshRecommendations() {
        if (Boolean.TRUE.equals(_isRefreshEnabled.getValue())) {
            fetchMusicRecommendations(lastHappy, lastSad, lastAngry, lastDisgust, lastFear);
            startCooldown();
        }
    }

    private void startCooldown() {
        _isRefreshEnabled.setValue(false);
        _cooldownSeconds.setValue(10);

        cooldownRunnable = new Runnable() {
            @Override
            public void run() {
                Integer current = _cooldownSeconds.getValue();
                if (current != null && current > 0) {
                    _cooldownSeconds.setValue(current - 1);
                    cooldownHandler.postDelayed(this, 1000);
                } else {
                    _isRefreshEnabled.setValue(true);
                }
            }
        };
        cooldownHandler.postDelayed(cooldownRunnable, 1000);
    }

    /**
     * 保存情緒輸入值（供刷新使用）
     * @param happy 開心指數 (0-100)
     * @param sad 悲傷指數 (0-100)
     * @param angry 憤怒指數 (0-100)
     * @param disgust 厭惡指數 (0-100)
     * @param fear 恐懼指數 (0-100)
     */
    public void saveEmotionInput(int happy, int sad, int angry, int disgust, int fear) {
        this.lastHappy = happy;
        this.lastSad = sad;
        this.lastAngry = angry;
        this.lastDisgust = disgust;
        this.lastFear = fear;
    }

    /**
     * @deprecated 使用 {@link #saveEmotionInput(int, int, int, int, int)} 代替
     */
    @Deprecated
    public void setLastInput(int happy, int sad, int angry, int disgust, int fear) {
        saveEmotionInput(happy, sad, angry, disgust, fear);
    }

    /**
     * 根據用戶直接輸入的情緒文字獲取音樂推薦
     * @param emotionText 用戶輸入的情緒文字
     */
    public void fetchMusicByDirectInput(String emotionText) {
        if (emotionText == null || emotionText.trim().isEmpty()) {
            _errorMessage.setValue(getApplication().getString(R.string.error_please_input_feeling));
            return;
        }

        String accessToken = tokenManager.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            _errorMessage.setValue(getApplication().getString(R.string.error_please_login_spotify));
            return;
        }

        _isLoading.setValue(true);
        _statusMessage.setValue(getApplication().getString(R.string.status_analyzing_emotions_zh));
        _errorMessage.setValue(null);

        musicRepository.getMusicByDirectText(emotionText.trim(), accessToken,
                new MusicRepository.MusicRepositoryCallback() {
                    @Override
                    public void onSuccess(List<MusicItem> musicItems) {
                        mainHandler.post(() -> {
                            _isLoading.setValue(false);
                            _musicList.setValue(musicItems);
                            _statusMessage.setValue(getApplication().getString(R.string.status_found_music_zh, musicItems.size()));
                            _hasPlaylist.setValue(true);
                            // 成功獲取後自動退出直接輸入模式
                            _isDirectInputMode.setValue(false);
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        mainHandler.post(() -> {
                            _isLoading.setValue(false);
                            _errorMessage.setValue(errorMessage);
                            // 檢測是否為 API 限流錯誤
                            if (errorMessage != null && (errorMessage.contains("limit") ||
                                errorMessage.contains("quota") || errorMessage.contains("RATE_LIMIT"))) {
                                _statusMessage.setValue(getApplication().getString(R.string.status_request_limit_reached));
                            } else {
                                _statusMessage.setValue(getApplication().getString(R.string.status_failed_to_get_recommendations_zh));
                            }
                        });
                    }
                });
    }

    /**
     * 進入直接輸入模式
     * 顯示文字輸入框，隱藏情緒數值輸入
     */
    public void enterDirectInputMode() {
        _isDirectInputMode.setValue(true);
        _showDirectInputDialog.setValue(false);
    }

    /**
     * 退出直接輸入模式
     * 由用戶點擊遮罩層觸發，隱藏輸入框和遮罩層
     * 不重置 badListClickCount，用戶再次點擊 btn_list_bad 仍可進入直接輸入模式
     */
    public void exitDirectInputMode() {
        _isDirectInputMode.setValue(false);
    }

    /**
     * 取消直接輸入對話框
     * 不進入直接輸入模式，僅關閉對話框
     */
    public void cancelDirectInputDialog() {
        _showDirectInputDialog.setValue(false);
    }

    /**
     * @deprecated 使用 {@link #enterDirectInputMode()} 代替
     */
    @Deprecated
    public void enableDirectInputMode(boolean enable) {
        if (enable) {
            enterDirectInputMode();
        } else {
            _isDirectInputMode.setValue(false);
        }
    }

    /**
     * @deprecated 使用 {@link #cancelDirectInputDialog()} 代替
     */
    @Deprecated
    public void dismissDirectInputDialog() {
        cancelDirectInputDialog();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cooldownHandler.removeCallbacksAndMessages(null);
        // 單例模式下不在此處斷開連接，由 Application 生命週期管理
        // spotifyPlayerManager.disconnect();
        favoriteRepository.shutdown();
    }
}
