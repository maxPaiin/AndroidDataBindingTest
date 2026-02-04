package View;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.example.graduationproject.R;
import com.example.graduationproject.databinding.ActivityUserMainBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import Model.POJO.MusicItem;
import Model.POJO.PlaylistData;
import Model.Spotify.SpotifyPlayerManager;
import View.adapter.MusicListAdapter;
import View.adapter.OnMusicItemClickListener;
import ViewModel.MiniPlayerViewModel;
import ViewModel.MusicViewModel;
import ViewModel.UserMainViewModel;

/**
 * 用戶主頁面 Activity（UI邏輯最麻煩的一個Activity）
 * 處理音樂推薦和 Spotify 播放控制
 * Spotify App Remote 連接流程：
 * 1. 在 onCreate/onStart 調 connectSpotify()
 * 2. SpotifyPlayerManager 用 showAuthView(true) 自動處理授權
 * 3. 授權在 Spotify App 內完成，不需要額外的 intent-filter（這個必須強制，否則會出現啟動的Activity不一致的問題，這是SDK的鍋）
 */
public class UserMainActivity extends AppCompatActivity {
    private static final String TAG = "UserMainActivity";

    private UserMainViewModel userMainViewModel;
    private MusicViewModel musicViewModel;
    private MiniPlayerViewModel miniPlayerViewModel;
    private ActivityUserMainBinding binding;
    private MusicListAdapter musicListAdapter;

    // 用於防止重複顯示 Spotify 連接 Toast
    private boolean hasShownSpotifyConnectedToast = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_main);

        // 初始化 ViewModels
        userMainViewModel = new ViewModelProvider(this).get(UserMainViewModel.class);
        musicViewModel = new ViewModelProvider(this).get(MusicViewModel.class);
        miniPlayerViewModel = new ViewModelProvider(this).get(MiniPlayerViewModel.class);

        // 綁定 ViewModels
        binding.setUserViewModel(userMainViewModel);
        binding.setMusicViewModel(musicViewModel);
        binding.setMiniPlayerViewModel(miniPlayerViewModel);
        binding.setLifecycleOwner(this);

        // 初始化 Adapter
        setupMusicListAdapter();

        // 設置按鈕點擊事件
        setupSendButton();
        setupUserPageButton();
        setupFeedbackButtons();
        setupDirectInputSendButton();
        setupOverlayMaskClickListener();

        // 設置 MiniPlayer 點擊事件
        setupMiniPlayerClickListeners();

        // 觀察數據變化
        observeViewModels();

        // 連 Spotify App Remote
        connectSpotify();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 重新連接 Spotify（如果未連接）
        if (!musicViewModel.getSpotifyPlayerManager().isConnected() &&
            !musicViewModel.getSpotifyPlayerManager().isConnecting()) {
            Log.d(TAG, "onStart: 重新連接 Spotify");
            connectSpotify();
        }

        // 綁定 MiniPlayer 到 SpotifyPlayerManager
        miniPlayerViewModel.bindToPlayerManager(musicViewModel.getSpotifyPlayerManager());
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 解除 MiniPlayer 綁定
        miniPlayerViewModel.unbindFromPlayerManager();
        // 單例模式下不在頁面 onStop 時斷開連接，保持連接狀態
    }

    /**
     * 連接 Spotify Android SDK
     * 和當時測試的寫的是一樣的，這個東西很容易出問題
     */
    private void connectSpotify() {
        Log.d(TAG, "開始連接 Spotify...");
        musicViewModel.connectSpotify(this);
    }

    /**
     * 設置音樂列表 Adapter
     */
    private void setupMusicListAdapter() {
        musicListAdapter = new MusicListAdapter(this);
        binding.lvMusicList.setAdapter(musicListAdapter);

        // 設置點擊事件監聽器
        musicListAdapter.setOnMusicItemClickListener(new OnMusicItemClickListener() {
            @Override
            public void onPlayClick(MusicItem item, int position) {
                //檢查是否已連接 Spotify
                if (!musicViewModel.getSpotifyPlayerManager().isConnected()) {
                    Toast.makeText(UserMainActivity.this, R.string.toast_connecting_spotify, Toast.LENGTH_SHORT).show();
                    connectSpotify();
                }
                // 一開始就加載整個歌曲列表，而不是單個歌曲
                List<MusicItem> musicItems = musicListAdapter.getMusicItems();
                if(musicItems != null && !musicItems.isEmpty())
                    SpotifyPlayerManager.getInstance().cachePlaylist(musicItems, position);
                // 播放
                musicViewModel.playTrack(item);
                miniPlayerViewModel.show();

            }

            @Override
            public void onPauseClick(MusicItem item, int position) {
                // 暫停播放
                musicViewModel.pausePlayback();
            }

            @Override
            public void onItemClick(MusicItem item, int position) {
                // 跳轉到播放器頁面
                navigateToPlayerView(position);
            }

            @Override
            public void onLikeClick(MusicItem item, int position) {
                // 切換收藏狀態
                musicViewModel.toggleFavorite(item);
            }
        });
    }

    /**
     * 設置 MiniPlayer 點擊事件
     */
    private void setupMiniPlayerClickListeners() {
        // 點擊 MiniPlayer 區域跳轉到 PlayerView
        binding.miniPlayer.miniPlayerContainer.setOnClickListener(v -> {
            navigateToPlayerViewFromMiniPlayer();
        });

        // 播放/暫停按鈕
        binding.miniPlayer.btnPlayPause.setOnClickListener(v -> {
            miniPlayerViewModel.togglePlayPause();
        });

        // 收藏按鈕
        binding.miniPlayer.btnFavorite.setOnClickListener(v -> {
            miniPlayerViewModel.toggleFavorite();
        });
    }

    /**
     * 從 MiniPlayer 跳轉到 PlayerView
     * 優先使用緩存的完整播放列表，如果沒有則使用當前單曲
     */
    private void navigateToPlayerViewFromMiniPlayer() {
        SpotifyPlayerManager playerManager = SpotifyPlayerManager.getInstance();
        PlaylistData playlistData;

        // 優先使用緩存的完整播放列表
        if (playerManager.hasCachedPlaylist()) {
            List<MusicItem> cachedList = playerManager.getCachedPlaylist();
            int cachedIndex = playerManager.getCachedPlaylistIndex();
            playlistData = new PlaylistData(cachedList, cachedIndex);
        } else {
            // 沒有緩存時，使用當前單曲，如果有的話（QA時才加）
            MusicItem currentTrack = miniPlayerViewModel.currentTrack.getValue();
            if (currentTrack == null) {
                Toast.makeText(this, R.string.toast_cannot_play_music, Toast.LENGTH_SHORT).show();
                return;
            }
            List<MusicItem> singleTrackList = java.util.Collections.singletonList(currentTrack);
            playlistData = new PlaylistData(singleTrackList, 0);
        }

        // 啟動 PlayerView
        Intent intent = new Intent(this, PlayerView.class);
        intent.putExtra(PlayerView.EXTRA_PLAYLIST_DATA, playlistData);
        startActivity(intent);
    }

    /**
     * 跳轉到播放器頁面
     * @param position 當前點擊的歌曲索引
     */
    private void navigateToPlayerView(int position) {
        List<MusicItem> musicItems = musicListAdapter.getMusicItems();
        if (musicItems == null || musicItems.isEmpty()) {
            Toast.makeText(this, R.string.toast_no_playlist_data, Toast.LENGTH_SHORT).show();
            return;
        }

        // 封裝歌單數據
        PlaylistData playlistData = new PlaylistData(musicItems, position);

        // 啟動 PlayerView
        Intent intent = new Intent(this, PlayerView.class);
        intent.putExtra(PlayerView.EXTRA_PLAYLIST_DATA, playlistData);
        startActivity(intent);
    }

    /**
     * 設置發送按鈕點擊事件
     * 從 UserMainViewModel 的 Slider LiveData 獲取情緒值
     */
    private void setupSendButton() {
        binding.btnSend.setOnClickListener(v -> {
            // 從 UserMainViewModel 獲取 Slider 的 int 值
            int happy = userMainViewModel.getHappyInt();
            int sad = userMainViewModel.getSadInt();
            int angry = userMainViewModel.getAngryInt();
            int disgust = userMainViewModel.getDisgustInt();
            int fear = userMainViewModel.getFearInt();

            // 保存情緒輸入以供刷新使用
            musicViewModel.saveEmotionInput(happy, sad, angry, disgust, fear);
            musicViewModel.fetchMusicRecommendations(happy, sad, angry, disgust, fear);
        });
    }

    /**
     * 設置反饋按鈕點擊事件
     */
    private void setupFeedbackButtons() {
        // btn_list_bad: 刷新音樂列表，有冷卻時間和點擊計數
        binding.btnListBad.setOnClickListener(v -> {
            boolean canRefresh = musicViewModel.handleBadListClick();
            if (!canRefresh) {
                // 冷卻中，顯示剩餘秒數
                Integer remainingSeconds = musicViewModel.cooldownSeconds.getValue();
                if (remainingSeconds != null && remainingSeconds > 0) {
                    Toast.makeText(this,
                            getString(R.string.toast_please_wait_seconds, remainingSeconds),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        // btn_list_good: 顯示感謝訊息
        binding.btnListGood.setOnClickListener(v -> {
            Toast.makeText(this, R.string.toast_thank_you, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 設置直接輸入發送按鈕點擊事件
     */
    private void setupDirectInputSendButton() {
        // TextInputLayout 的 end icon 點擊事件
        binding.tilDirectInput.setEndIconOnClickListener(v -> {
            String emotionText = binding.edDirectInput.getText() != null ?
                    binding.edDirectInput.getText().toString().trim() : "";

            if (emotionText.isEmpty()) {
                Toast.makeText(this, R.string.toast_input_your_feeling, Toast.LENGTH_SHORT).show();
                return;
            }

            // 發送文字到 API
            musicViewModel.fetchMusicByDirectInput(emotionText);
        });
    }

    /**
     * 設置遮罩層點擊事件
     * 點擊遮罩層（輸入框以外區域）退出直接輸入模式
     * 注意：不清除 btn_list_bad 計數，用戶可再次觸發直接輸入
     */
    private void setupOverlayMaskClickListener() {
        binding.overlayMask.setOnClickListener(v -> {
            musicViewModel.exitDirectInputMode();
        });
    }

    /**
     * 顯示直接輸入確認對話框
     */
    private void showDirectInputDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_direct_input)
                .setMessage(R.string.input_word)
                .setPositiveButton(R.string.dialog_btn_yes, (dialog, which) -> {
                    musicViewModel.enterDirectInputMode();
                    // 清空輸入框
                    binding.edDirectInput.setText("");
                })
                .setNegativeButton(R.string.dialog_btn_no, (dialog, which) -> {
                    musicViewModel.cancelDirectInputDialog();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * 設置用戶設置頁面按鈕點擊事件
     */
    private void setupUserPageButton() {
        binding.btnUserPage.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserSettingView.class);
            startActivity(intent);
        });
    }

    /**
     * 觀察 ViewModel 數據變化
     */
    private void observeViewModels() {
        // 觀察音樂列表變化
        musicViewModel.musicList.observe(this, musicItems -> {
            if (musicItems != null) {
                musicListAdapter.updateData(musicItems);
            }
        });

        // 觀察錯誤訊息（包含 Spotify 連接錯誤處理）
        musicViewModel.errorMessage.observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Log.e(TAG, "errorMessage: " + errorMessage);

                // 過濾掉 Spotify Free 用戶的 seek 限制錯誤（不需要每次都顯示）
                if (errorMessage.contains("Cannot seek in song") || errorMessage.contains("CANT_PLAY_ON_DEVICE")) {
                    Log.d(TAG, "Spotify Free 用戶 seek 限制，忽略此錯誤");
                    musicViewModel.clearError();
                    return;
                }

                // 檢查是否為 Spotify 授權服務不可用的錯誤
                if (errorMessage.contains("授權服務不可用") || errorMessage.contains("AUTHENTICATION_SERVICE_UNAVAILABLE")) {
                    // 使用與測試代碼相同的解決方案：先打開 Spotify App，然後連接
                    Log.d(TAG, "嘗試先打開 Spotify App 再連接...");
                    Toast.makeText(this, R.string.toast_opening_spotify_auth, Toast.LENGTH_SHORT).show();
                    musicViewModel.openSpotifyAndConnect(this);
                } else {
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                }

                musicViewModel.clearError();
            }
        });

        // 觀察當前播放曲目變化
        musicViewModel.currentPlayingTrackId.observe(this, trackId -> {
            Boolean isPlaying = musicViewModel.isPlaying.getValue();
            musicListAdapter.updatePlaybackState(trackId, isPlaying != null && isPlaying);
        });

        // 觀察播放狀態變化
        musicViewModel.isPlaying.observe(this, isPlaying -> {
            String trackId = musicViewModel.currentPlayingTrackId.getValue();
            musicListAdapter.updatePlaybackState(trackId, isPlaying != null && isPlaying);
        });

        // 觀察 Spotify 連接狀態（避免重複顯示 Toast）
        musicViewModel.isSpotifyConnected.observe(this, isConnected -> {
            if (isConnected != null && isConnected) {
                Log.d(TAG, "Spotify 連接成功");
                // 只在首次連接時顯示 Toast，避免頁面切換時重複顯示
                if (!hasShownSpotifyConnectedToast) {
                    Toast.makeText(this, R.string.toast_connected_spotify, Toast.LENGTH_SHORT).show();
                    hasShownSpotifyConnectedToast = true;
                }
            }
        });

        // 觀察 Spotify 連接中狀態
        musicViewModel.isSpotifyConnecting.observe(this, isConnecting -> {
            if (isConnecting != null && isConnecting) {
                Log.d(TAG, "正在連接 Spotify...");
            }
        });

        // 觀察收藏操作 Toast 訊息
        musicViewModel.toastMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                musicViewModel.clearToastMessage();
            }
        });

        // 觀察是否需要顯示直接輸入對話框
        musicViewModel.showDirectInputDialog.observe(this, shouldShow -> {
            if (shouldShow != null && shouldShow) {
                showDirectInputDialog();
            }
        });

        // 觀察直接輸入模式變化（成功獲取音樂後自動退出）
        musicViewModel.isDirectInputMode.observe(this, isDirectMode -> {
            if (isDirectMode != null && !isDirectMode) {
                // 清空輸入框
                binding.edDirectInput.setText("");
            }
        });

        // 觀察 MiniPlayer Toast 訊息
        miniPlayerViewModel.toastMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(binding.coordinatorLayout, message, Snackbar.LENGTH_SHORT)
                        .setAnchorView(binding.miniPlayer.getRoot())
                        .show();
                miniPlayerViewModel.clearToastMessage();
            }
        });
    }
}
