package ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.graduationproject.R;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import Model.Entity.FavoriteEntity;
import Model.POJO.MusicItem;
import Model.Repository.FavoriteRepository;

/**
 * 收藏列表 ViewModel
 * 管理收藏歌曲的顯示和篩選
 */
public class MyListViewModel extends AndroidViewModel {

    private final FavoriteRepository repository;

    // 當前收藏數據源（可切換全部/日期篩選）
    private LiveData<List<FavoriteEntity>> currentFavoritesSource;

    // 轉換後的 MusicItem 列表（供 UI 使用）
    private final MediatorLiveData<List<MusicItem>> _musicList = new MediatorLiveData<>();
    public LiveData<List<MusicItem>> musicList = _musicList;

    // 空數據提示訊息
    private final MutableLiveData<String> _emptyMessage = new MutableLiveData<>();
    public LiveData<String> emptyMessage = _emptyMessage;

    // 是否顯示空狀態
    private final MutableLiveData<Boolean> _isEmpty = new MutableLiveData<>(false);
    public LiveData<Boolean> isEmpty = _isEmpty;

    // Toast 訊息
    private final MutableLiveData<String> _toastMessage = new MutableLiveData<>();
    public LiveData<String> toastMessage = _toastMessage;

    // 當前是否為日期篩選模式
    private boolean isDateFiltered = false;

    public MyListViewModel(@NonNull Application application) {
        super(application);
        repository = new FavoriteRepository(application);

        // 默認加載所有收藏
        loadAllFavorites();
    }

    /**
     * 加載所有收藏
     */
    public void loadAllFavorites() {
        isDateFiltered = false;
        _emptyMessage.setValue(null);

        // 移除舊的數據源觀察
        if (currentFavoritesSource != null) {
            _musicList.removeSource(currentFavoritesSource);
        }

        // 設置新的數據源
        currentFavoritesSource = repository.getAllFavorites();
        _musicList.addSource(currentFavoritesSource, entities -> {
            updateMusicList(entities);
        });
    }

    /**
     * 按日期篩選收藏
     * @param startOfDay 當天開始時間（毫秒）
     * @param endOfDay 當天結束時間（毫秒）
     */
    public void filterByDate(long startOfDay, long endOfDay) {
        isDateFiltered = true;

        // 移除舊的數據源觀察
        if (currentFavoritesSource != null) {
            _musicList.removeSource(currentFavoritesSource);
        }

        // 設置新的數據源（日期篩選）
        currentFavoritesSource = repository.getFavoritesByDateRange(startOfDay, endOfDay);
        _musicList.addSource(currentFavoritesSource, entities -> {
            updateMusicList(entities);
            if (entities == null || entities.isEmpty()) {
                _emptyMessage.setValue(getApplication().getString(R.string.empty_no_songs_for_date));
            } else {
                _emptyMessage.setValue(null);
            }
        });
    }

    /**
     * 將 FavoriteEntity 列表轉換為 MusicItem 列表
     */
    private void updateMusicList(List<FavoriteEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            _musicList.setValue(new ArrayList<>());
            _isEmpty.setValue(true);
        } else {
            List<MusicItem> items = entities.stream()
                    .map(FavoriteEntity::toMusicItem)
                    .collect(Collectors.toList());
            _musicList.setValue(items);
            _isEmpty.setValue(false);
        }
    }

    /**
     * 移除收藏
     */
    public void removeFavorite(MusicItem item) {
        if (item == null || item.getSpotifyTrackId() == null) {
            return;
        }
        repository.removeFavorite(item.getSpotifyTrackId(), () -> {
            _toastMessage.postValue(getApplication().getString(R.string.toast_removed_from_favorites));
        });
    }

    /**
     * 清除 Toast 訊息
     */
    public void clearToastMessage() {
        _toastMessage.setValue(null);
    }

    /**
     * 清除篩選，顯示全部收藏
     */
    public void clearFilter() {
        if (isDateFiltered) {
            loadAllFavorites();
        }
    }

    /**
     * 檢查是否為日期篩選模式
     */
    public boolean isDateFiltered() {
        return isDateFiltered;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.shutdown();
    }
}
