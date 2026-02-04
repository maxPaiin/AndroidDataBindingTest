package View;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.example.graduationproject.R;
import com.example.graduationproject.databinding.ActivityMyListViewBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;
import java.util.List;

import Model.POJO.MusicItem;
import Model.POJO.PlaylistData;
import Model.Spotify.SpotifyPlayerManager;
import View.adapter.MusicListAdapter;
import View.adapter.OnMusicItemClickListener;
import ViewModel.MiniPlayerViewModel;
import ViewModel.MusicViewModel;
import ViewModel.MyListViewModel;
import ViewModel.UserMainViewModel;

/**
 * 收藏列表頁面
 * 顯示用戶收藏的歌曲，支持日期篩選
 */
public class MyListView extends AppCompatActivity {

    private static final String TAG = "MyListView";

    private ActivityMyListViewBinding binding;
    private MyListViewModel myListViewModel;
    private UserMainViewModel userViewModel;
    private MusicViewModel musicViewModel;
    private MiniPlayerViewModel miniPlayerViewModel;
    private MusicListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // 設置 Data Binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_my_list_view);

        // 初始化 ViewModels
        myListViewModel = new ViewModelProvider(this).get(MyListViewModel.class);
        userViewModel = new ViewModelProvider(this).get(UserMainViewModel.class);
        musicViewModel = new ViewModelProvider(this).get(MusicViewModel.class);
        miniPlayerViewModel = new ViewModelProvider(this).get(MiniPlayerViewModel.class);

        // 綁定到 XML
        binding.setMvm(myListViewModel);
        binding.setUserViewModel(userViewModel);
        binding.setMiniPlayerViewModel(miniPlayerViewModel);
        binding.setLifecycleOwner(this);

        // 設置 ListView Adapter
        setupMusicListAdapter();

        // 設置日期選擇按鈕
        setupDatePicker();

        // 設置用戶頭像點擊跳轉
        setupUserImageClick();

        // 設置 MiniPlayer 點擊事件
        setupMiniPlayerClickListeners();

        // 觀察數據變化
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
     * 設置用戶頭像點擊跳轉到 UserSettingView
     */
    private void setupUserImageClick() {
        binding.ivUserImage.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserSettingView.class);
            startActivity(intent);
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
            List<MusicItem> singleTrackList = java.util.Collections.singletonList(currentTrack);
            playlistData = new PlaylistData(singleTrackList, 0);
        }

        // 啟動 PlayerView
        Intent intent = new Intent(this, PlayerView.class);
        intent.putExtra(PlayerView.EXTRA_PLAYLIST_DATA, playlistData);
        startActivity(intent);
    }

    /**
     * 設置音樂列表 Adapter
     */
    private void setupMusicListAdapter() {
        adapter = new MusicListAdapter(this);
        binding.lvMusicList.setAdapter(adapter);

        adapter.setOnMusicItemClickListener(new OnMusicItemClickListener() {
            @Override
            public void onPlayClick(MusicItem item, int position) {
                // 跳轉到播放頁面
                navigateToPlayerView(position);
            }

            @Override
            public void onPauseClick(MusicItem item, int position) {
                // 在收藏列表中，暫停按鈕也跳轉到播放頁面
                navigateToPlayerView(position);
            }

            @Override
            public void onItemClick(MusicItem item, int position) {
                // 點擊整個 item 跳轉到播放頁面
                navigateToPlayerView(position);
            }

            @Override
            public void onLikeClick(MusicItem item, int position) {
                // 從收藏列表移除
                myListViewModel.removeFavorite(item);
            }
        });
    }

    /**
     * 設置日期選擇按鈕
     */
    private void setupDatePicker() {
        binding.btnCalendarSelect.setOnClickListener(v -> {
            showDatePickerDialog();
        });

        // 長按清除篩選
        binding.btnCalendarSelect.setOnLongClickListener(v -> {
            if (myListViewModel.isDateFiltered()) {
                myListViewModel.loadAllFavorites();
                Toast.makeText(this, R.string.toast_shown_all_favorites, Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    /**
     * 顯示日期選擇對話框
     */
    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // 計算選擇日期的時間範圍
                    Calendar startCal = Calendar.getInstance();
                    startCal.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0);
                    startCal.set(Calendar.MILLISECOND, 0);
                    long startOfDay = startCal.getTimeInMillis();

                    Calendar endCal = Calendar.getInstance();
                    endCal.set(selectedYear, selectedMonth, selectedDay, 23, 59, 59);
                    endCal.set(Calendar.MILLISECOND, 999);
                    long endOfDay = endCal.getTimeInMillis();

                    // 執行日期篩選
                    myListViewModel.filterByDate(startOfDay, endOfDay);

                    // 顯示篩選日期
                    @SuppressLint("DefaultLocale") String dateStr = String.format("%d/%d/%d", selectedYear, selectedMonth + 1, selectedDay);
                    Toast.makeText(this, getString(R.string.toast_screening_date, dateStr), Toast.LENGTH_SHORT).show();
                },
                year, month, day
        );

        // 設置最大日期為今天
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    /**
     * 觀察 ViewModel 數據變化
     */
    private void observeViewModels() {
        // 觀察音樂列表變化
        myListViewModel.musicList.observe(this, musicItems -> {
            if (musicItems != null) {
                adapter.updateData(musicItems);
            }
        });

        // 觀察空數據提示
        myListViewModel.emptyMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        // 觀察 Toast 訊息
        myListViewModel.toastMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                myListViewModel.clearToastMessage();
            }
        });

        // 觀察 MiniPlayer Toast 訊息
        miniPlayerViewModel.toastMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                miniPlayerViewModel.clearToastMessage();
            }
        });
    }

    /**
     * 跳轉到播放頁面
     */
    private void navigateToPlayerView(int position) {
        List<MusicItem> musicItems = adapter.getMusicItems();
        if (musicItems == null || musicItems.isEmpty()) {
            Toast.makeText(this, R.string.toast_no_song_data, Toast.LENGTH_SHORT).show();
            return;
        }

        // 封裝歌單數據
        PlaylistData playlistData = new PlaylistData(musicItems, position);

        // 啟動 PlayerView
        Intent intent = new Intent(this, PlayerView.class);
        intent.putExtra(PlayerView.EXTRA_PLAYLIST_DATA, playlistData);
        startActivity(intent);
    }
}
