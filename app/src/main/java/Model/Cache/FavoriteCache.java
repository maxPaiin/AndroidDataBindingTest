package Model.Cache;

import android.util.LruCache;

import java.util.List;

import Model.Entity.FavoriteEntity;

/**
 * 收藏歌曲緩存
 * 使用 LruCache 實現內存緩存，提升查詢性能
 */
public class FavoriteCache {

    private static volatile FavoriteCache instance;
    private final LruCache<String, FavoriteEntity> cache;

    private FavoriteCache() {
        // 內存大小設置為最大可用內存的 1/8
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 8);
        cache = new LruCache<String, FavoriteEntity>(maxMemory) {
            @Override
            protected int sizeOf(String key, FavoriteEntity value) {
                // 估算每個實體的大小
                int musicNameSize = value.getMusicName() != null ? value.getMusicName().length() : 0;
                int artistNameSize = value.getArtistName() != null ? value.getArtistName().length() : 0;
                // 40 是其他字段的估算大小（trackId, albumCoverUrl, timestamp 等）
                return musicNameSize + artistNameSize + 40;
            }
        };
    }

    /**
     * 獲取單例實例
     */
    public static FavoriteCache getInstance() {
        if (instance == null) {
            synchronized (FavoriteCache.class) {
                if (instance == null) {
                    instance = new FavoriteCache();
                }
            }
        }
        return instance;
    }

    /**
     * 存入緩存
     */
    public void put(String trackId, FavoriteEntity entity) {
        if (trackId != null && entity != null) {
            cache.put(trackId, entity);
        }
    }

    /**
     * 從緩存獲取
     */
    public FavoriteEntity get(String trackId) {
        if (trackId == null) {
            return null;
        }
        return cache.get(trackId);
    }

    /**
     * 從緩存移除
     */
    public void remove(String trackId) {
        if (trackId != null) {
            cache.remove(trackId);
        }
    }

    /**
     * 檢查是否在緩存中
     */
    public boolean contains(String trackId) {
        return trackId != null && cache.get(trackId) != null;
    }

    /**
     * 預熱緩存（從數據庫加載熱數據）
     */
    public void warmUp(List<FavoriteEntity> entities) {
        if (entities == null) return;
        for (FavoriteEntity entity : entities) {
            if (entity != null && entity.getTrackId() != null) {
                cache.put(entity.getTrackId(), entity);
            }
        }
    }

    /**
     * 清空緩存
     */
    public void clear() {
        cache.evictAll();
    }

    /**
     * 獲取緩存大小
     */
    public int size() {
        return cache.size();
    }
}
