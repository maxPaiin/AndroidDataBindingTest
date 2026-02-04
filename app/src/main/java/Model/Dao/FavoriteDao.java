package Model.Dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import Model.Entity.FavoriteEntity;

/**
 * 收藏歌曲 DAO 接口
 * 定義數據庫操作方法
 */
@Dao
public interface FavoriteDao {

    /**
     * 插入收藏記錄
     * 衝突策略：替換（如果 trackId 已存在則更新）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FavoriteEntity entity);

    /**
     * 根據 trackId 刪除收藏
     */
    @Query("DELETE FROM favorites WHERE trackId = :trackId")
    void deleteByTrackId(String trackId);

    /**
     * 獲取所有收藏（LiveData）
     * 按收藏時間倒序排列（最新收藏在前）
     */
    @Query("SELECT * FROM favorites ORDER BY savedTimestamp DESC")
    LiveData<List<FavoriteEntity>> getAllLive();

    /**
     * 獲取指定日期範圍內的收藏（LiveData）
     * @param startTime 開始時間（毫秒）
     * @param endTime 結束時間（毫秒）
     */
    @Query("SELECT * FROM favorites WHERE savedTimestamp BETWEEN :startTime AND :endTime ORDER BY savedTimestamp DESC")
    LiveData<List<FavoriteEntity>> getByDateRange(long startTime, long endTime);

    /**
     * 檢查歌曲是否已收藏
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE trackId = :trackId)")
    boolean isFavorite(String trackId);

    /**
     * 獲取最近的收藏記錄（同步版本，用於 Cache 預熱）
     * @return 最近 50 條收藏記錄
     */
    @Query("SELECT * FROM favorites ORDER BY savedTimestamp DESC LIMIT 50")
    List<FavoriteEntity> getRecentSync();

    /**
     * 獲取收藏總數
     */
    @Query("SELECT COUNT(*) FROM favorites")
    int getCount();
}
