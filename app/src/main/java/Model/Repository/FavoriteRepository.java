package Model.Repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Model.Cache.FavoriteCache;
import Model.Dao.FavoriteDao;
import Model.Database.AppDatabase;
import Model.Entity.FavoriteEntity;
import Model.POJO.MusicItem;

/**
 * 收藏功能 Repository
 * 實現「寫入穿透」策略：同時維護 Cache 和 Room
 */
public class FavoriteRepository {

    private final FavoriteDao dao;
    private final FavoriteCache cache;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public FavoriteRepository(Context context) {
        this.dao = AppDatabase.getInstance(context).favoriteDao();
        this.cache = FavoriteCache.getInstance();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());

        // 預熱緩存
        warmUpCache();
    }

    /**
     * 預熱緩存：從數據庫加載最近的收藏
     */
    private void warmUpCache() {
        executor.execute(() -> {
            List<FavoriteEntity> recentFavorites = dao.getRecentSync();
            cache.warmUp(recentFavorites);
        });
    }

    /**
     * 添加收藏
     * 寫入穿透：同時寫入 Cache 和 Room
     */
    public void addFavorite(MusicItem item, Runnable onComplete) {
        if (item == null || item.getSpotifyTrackId() == null) {
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
            return;
        }

        executor.execute(() -> {
            FavoriteEntity entity = FavoriteEntity.fromMusicItem(item);
            if (entity != null) {
                // 寫入數據庫
                dao.insert(entity);
                // 寫入緩存
                cache.put(entity.getTrackId(), entity);
            }
            // 回調主線程
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
        });
    }

    /**
     * 移除收藏
     * 同時從 Cache 和 Room 刪除
     */
    public void removeFavorite(String trackId, Runnable onComplete) {
        if (trackId == null) {
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
            return;
        }

        executor.execute(() -> {
            // 從數據庫刪除
            dao.deleteByTrackId(trackId);
            // 從緩存移除
            cache.remove(trackId);
            // 回調主線程
            if (onComplete != null) {
                mainHandler.post(onComplete);
            }
        });
    }

    /**
     * 檢查是否已收藏
     * 優先檢查 Cache，未命中則查詢 Room 並回填 Cache
     */
    public void checkIsFavorite(String trackId, BooleanCallback callback) {
        if (trackId == null) {
            if (callback != null) {
                mainHandler.post(() -> callback.onResult(false));
            }
            return;
        }

        executor.execute(() -> {
            boolean isFavorite;
            // 優先檢查緩存
            if (cache.contains(trackId)) {
                isFavorite = true;
            } else {
                // 緩存未命中，查詢數據庫
                isFavorite = dao.isFavorite(trackId);
            }
            // 回調主線程
            if (callback != null) {
                mainHandler.post(() -> callback.onResult(isFavorite));
            }
        });
    }

    /**
     * 獲取所有收藏（LiveData，自動按時間倒序）
     */
    public LiveData<List<FavoriteEntity>> getAllFavorites() {
        return dao.getAllLive();
    }

    /**
     * 獲取指定日期範圍的收藏（LiveData）
     */
    public LiveData<List<FavoriteEntity>> getFavoritesByDateRange(long startTime, long endTime) {
        return dao.getByDateRange(startTime, endTime);
    }

    /**
     * 關閉 ExecutorService
     */
    public void shutdown() {
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }

    /**
     * Boolean 結果回調接口
     */
    public interface BooleanCallback {
        void onResult(boolean result);
    }
}
