package View;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.graduationproject.R;
import com.example.graduationproject.databinding.ActivityPlayerViewBinding;
import com.google.android.material.snackbar.Snackbar;

import Model.POJO.MusicItem;
import Model.POJO.PlaylistData;
import ViewModel.PlayerViewModel;

/**
 * 播放器頁面
 * 顯示歌曲信息、播放控制和倒計時
 */
public class PlayerView extends AppCompatActivity {

    public static final String EXTRA_PLAYLIST_DATA = "playlist_data";

    private ActivityPlayerViewBinding binding;
    private PlayerViewModel playerViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // 設置 Data Binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_player_view);
        playerViewModel = new ViewModelProvider(this).get(PlayerViewModel.class);
        binding.setPvm(playerViewModel);
        binding.setLifecycleOwner(this);

        // 從 Intent 接收歌單數據
        receivePlaylistData();

        // 設置觀察者
        setupObservers();

        // 設置按鈕點擊事件
        setupClickListeners();

        // 設置 SeekBar 監聽器
        setupSeekBar();

        // 開始連接並播放
        playerViewModel.connectAndPlay();
    }

    /**
     * 從 Intent 接收歌單數據
     */
    private void receivePlaylistData() {
        PlaylistData playlistData = getIntent().getParcelableExtra(EXTRA_PLAYLIST_DATA);

        if (playlistData != null && !playlistData.isEmpty()) {
            playerViewModel.setPlaylist(playlistData.getItems(), playlistData.getCurrentIndex());
        } else {
            // 如果沒有歌單數據，顯示錯誤並返回
            Toast.makeText(this, R.string.toast_no_playlist_data, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * 設置 LiveData 觀察者
     */
    private void setupObservers() {
        // 觀察當前歌曲變化，更新封面
        playerViewModel.currentTrack.observe(this, musicItem -> {
            if (musicItem != null) {
                // 更新歌曲名稱
                binding.tvTrackName.setText(musicItem.getSongName());
                // 更新藝術家名稱
                binding.tvArtistName.setText(musicItem.getArtistName());
                // 使用 Glide 加載封面（優先使用高清圖）
                loadAlbumCover(musicItem);
                // 檢查收藏狀態
                playerViewModel.checkFavoriteStatus(musicItem.getSpotifyTrackId());
            }
        });

        // 觀察剩餘時間
//        playerViewModel.remainingTime.observe(this, time -> {
//            binding.tvSongTime.setText(time);
//        });

        // 觀察播放/暫停按鈕文字
//        playerViewModel.playPauseText.observe(this, text -> {
//            binding.btnPlayPause.setText(text);
//        });

        // 觀察控制按鈕可用狀態
        playerViewModel.controlsEnabled.observe(this, enabled -> {
            binding.btnPlayPause.setEnabled(enabled);
            binding.btnPrevious.setEnabled(enabled);
            binding.btnNext.setEnabled(enabled);
        });

        // 觀察狀態訊息
        playerViewModel.statusMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        // 觀察收藏狀態
        playerViewModel.isFavorite.observe(this, isFavorite -> {
            if (isFavorite != null && isFavorite) {
                binding.btnLike.setText(R.string.player_heart_filled); // 實心愛心
            } else {
                binding.btnLike.setText(R.string.player_heart_empty); // 空心愛心
            }
        });

        // 觀察 Toast 訊息（收藏操作反饋）
        playerViewModel.toastMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                playerViewModel.clearToastMessage();
            }
        });

        // 觀察播放進度（更新 SeekBar）
        playerViewModel.currentPosition.observe(this, position -> {
            // 只在非拖動狀態下更新 SeekBar，避免 UI 抖動
            Boolean isSeeking = playerViewModel.isUserSeeking.getValue();
            if (isSeeking == null || !isSeeking) {
                updateSeekBarProgress(position);
            }
        });

        // 觀察總時長（設置 SeekBar 最大值）
        playerViewModel.totalDuration.observe(this, duration -> {
            if (duration != null && duration > 0) {
                // 使用毫秒作為 max 值以獲得更精確的控制
                binding.seekBarProgress.setMax(duration.intValue());
            }
        });

        // 觀察播放狀態（控制進度更新定時器）
        playerViewModel.isPlaying.observe(this, isPlaying -> {
            if (isPlaying != null && isPlaying) {
                playerViewModel.startProgressUpdates();
            }
        });

        // 觀察 Seek 功能受限訊息（Spotify Free 用戶）
        playerViewModel.seekRestrictedMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                        .setDuration(5000)  // 5 秒
                        .show();
                playerViewModel.clearSeekRestrictedMessage();
            }
        });
    }

    /**
     * 設置按鈕點擊事件
     */
    private void setupClickListeners() {
        // 播放/暫停按鈕
        binding.btnPlayPause.setOnClickListener(v -> {
            playerViewModel.togglePlayPause();
        });

        // 上一首按鈕
        binding.btnPrevious.setOnClickListener(v -> {
            playerViewModel.playPrevious();
        });

        // 下一首按鈕
        binding.btnNext.setOnClickListener(v -> {
            playerViewModel.playNext();
        });

        // 收藏按鈕
        binding.btnLike.setOnClickListener(v -> {
            playerViewModel.toggleFavorite();
        });
    }

    /**
     * 設置 SeekBar 拖動監聽器
     * 處理用戶拖動進度條的交互邏輯
     */
    private void setupSeekBar() {
        binding.seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 只處理用戶主動拖動的變化
                if (fromUser) {
                    // 更新時間顯示預覽
                    playerViewModel.updateSeekPosition(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 用戶開始拖動，暫停自動更新
                playerViewModel.setUserSeeking(true);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 用戶放開，執行跳轉並恢復自動更新
                playerViewModel.seekTo(seekBar.getProgress());
                playerViewModel.setUserSeeking(false);
            }
        });
    }

    /**
     * 更新 SeekBar 進度
     * @param positionMs 當前播放位置（毫秒）
     */
    private void updateSeekBarProgress(Long positionMs) {
        if (positionMs != null) {
            binding.seekBarProgress.setProgress(positionMs.intValue());
        }
    }

    /**
     * 使用 Glide 加載專輯封面
     * 優先使用高清圖，如果沒有則 fallback 到縮略圖
     */
    private void loadAlbumCover(MusicItem musicItem) {
        // 優先使用高清圖
        String imageUrl = musicItem.getLargeImageUrl();
        // 如果沒有高清圖，fallback 到縮略圖
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageUrl = musicItem.getAlbumImageUrl();
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(binding.ivAlbumCover);
        } else {
            binding.ivAlbumCover.setImageResource(R.drawable.ic_launcher_background);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 停止進度更新以節省資源
        playerViewModel.stopProgressUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ViewModel 會在 onCleared 中自動清理資源
    }
}
