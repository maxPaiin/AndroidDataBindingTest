package View;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.example.graduationproject.R;
import com.example.graduationproject.databinding.ActivityUserSettingViewBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;
import java.util.List;

import Model.POJO.MusicItem;
import Model.POJO.PlaylistData;
import Model.Spotify.SpotifyPlayerManager;
import ViewModel.MiniPlayerViewModel;
import ViewModel.MusicViewModel;
import ViewModel.UserMainViewModel;

/**
 * 用戶設置頁面
 * 顯示用戶設置選項，包括收藏夾、關於 App、退出登錄
 * 底部顯示 MiniPlayer
 */
public class UserSettingView extends AppCompatActivity {

    private static final String TAG = "UserSettingView";

    private ActivityUserSettingViewBinding binding;
    private UserMainViewModel userViewModel;
    private MusicViewModel musicViewModel;
    private MiniPlayerViewModel miniPlayerViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_setting_view);

        userViewModel = new ViewModelProvider(this).get(UserMainViewModel.class);
        musicViewModel = new ViewModelProvider(this).get(MusicViewModel.class);
        miniPlayerViewModel = new ViewModelProvider(this).get(MiniPlayerViewModel.class);

        binding.setUserViewModel(userViewModel);
        binding.setMiniPlayerViewModel(miniPlayerViewModel);
        binding.setLifecycleOwner(this);

        // 設置按鈕點擊事件
        setupClickListeners();

        // 設置 MiniPlayer 點擊事件
        setupMiniPlayerClickListeners();

        // 觀察 ViewModel 數據變化
        observeViewModels();

        // 連接 Spotify（如果尚未連接）
        connectSpotifyIfNeeded();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 重新連接 Spotify（如果未連接）
        SpotifyPlayerManager playerManager = musicViewModel.getSpotifyPlayerManager();
        if (!playerManager.isConnected() && !playerManager.isConnecting()) {
            Log.d(TAG, "onStart: 重新連接 Spotify");
            musicViewModel.connectSpotify(this);
        }

        // 綁定 MiniPlayer 到 SpotifyPlayerManager
        miniPlayerViewModel.bindToPlayerManager(playerManager);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 解除 MiniPlayer 綁定
        miniPlayerViewModel.unbindFromPlayerManager();
        // 單例模式下不在頁面 onStop 時斷開連接，保持連接狀態
    }

    /**
     * 連接 Spotify（如果尚未連接）
     */
    private void connectSpotifyIfNeeded() {
        SpotifyPlayerManager playerManager = musicViewModel.getSpotifyPlayerManager();
        if (!playerManager.isConnected() && !playerManager.isConnecting()) {
            Log.d(TAG, "連接 Spotify...");
            musicViewModel.connectSpotify(this);
        }
    }

    /**
     * 設置按鈕點擊事件
     */
    private void setupClickListeners() {
        // 收藏夾按鈕 - 跳轉到收藏列表
        binding.btnUserPage.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyListView.class);
            startActivity(intent);
        });

        // 關於此 App 按鈕
        binding.btnUserAboutAPP.setOnClickListener(v -> {
            Toast.makeText(this, R.string.about_app_message, Toast.LENGTH_SHORT).show();
        });

        // 退出登錄按鈕
        binding.btnUserLogout.setOnClickListener(v -> {
            // 清除所有登錄信息
            userViewModel.logout();
            // 跳轉回登錄頁面並清除任務棧
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
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
            // 沒有緩存時，使用當前單曲（兜底方案）
            MusicItem currentTrack = miniPlayerViewModel.currentTrack.getValue();
            if (currentTrack == null) {
                Toast.makeText(this, R.string.toast_no_songs_playing, Toast.LENGTH_SHORT).show();
                return;
            }
            List<MusicItem> singleTrackList = Collections.singletonList(currentTrack);
            playlistData = new PlaylistData(singleTrackList, 0);
        }

        // 啟動 PlayerView
        Intent intent = new Intent(this, PlayerView.class);
        intent.putExtra(PlayerView.EXTRA_PLAYLIST_DATA, playlistData);
        startActivity(intent);
    }

    /**
     * 觀察 ViewModel 數據變化
     */
    private void observeViewModels() {
        // 觀察 MiniPlayer Toast 訊息
        miniPlayerViewModel.toastMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                miniPlayerViewModel.clearToastMessage();
            }
        });
    }
}
