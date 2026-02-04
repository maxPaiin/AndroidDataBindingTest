package Model.Entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import Model.POJO.MusicItem;

/**
 * 收藏歌曲實體類
 * 用於 Room 數據庫存儲
 */
@Entity(tableName = "favorites", indices = {@Index(value = "trackId", unique = true)})
public class FavoriteEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;
    @NonNull
    private String trackId;        // Spotify Track ID，用於去重
    private String musicName;      // 歌曲名稱
    private String artistName;     // 藝術家名稱
    private String albumCoverUrl;  // 專輯封面 URL
    private long savedTimestamp;   // 收藏時間（毫秒）

    public FavoriteEntity() {
        this.trackId = "";
    }

    // 全參構造函數
    public FavoriteEntity(@NonNull String trackId, String musicName, String artistName,
                          String albumCoverUrl, long savedTimestamp) {
        this.trackId = trackId;
        this.musicName = musicName;
        this.artistName = artistName;
        this.albumCoverUrl = albumCoverUrl;
        this.savedTimestamp = savedTimestamp;
    }

    /**
     * 從 MusicItem 創建 FavoriteEntity
     */
    public static FavoriteEntity fromMusicItem(MusicItem item) {
        if (item == null || item.getSpotifyTrackId() == null) {
            return null;
        }
        return new FavoriteEntity(
                item.getSpotifyTrackId(),
                item.getSongName(),
                item.getArtistName(),
                item.getAlbumImageUrl(),
                System.currentTimeMillis()
        );
    }

    /**
     * 轉換為 MusicItem
     */
    public MusicItem toMusicItem() {
        MusicItem item = new MusicItem();
        item.setSpotifyTrackId(this.trackId);
        item.setSongName(this.musicName);
        item.setArtistName(this.artistName);
        item.setAlbumImageUrl(this.albumCoverUrl);
        return item;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public String getTrackId() {
        return trackId;
    }

    public void setTrackId(@NonNull String trackId) {
        this.trackId = trackId;
    }

    public String getMusicName() {
        return musicName;
    }

    public void setMusicName(String musicName) {
        this.musicName = musicName;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getAlbumCoverUrl() {
        return albumCoverUrl;
    }

    public void setAlbumCoverUrl(String albumCoverUrl) {
        this.albumCoverUrl = albumCoverUrl;
    }

    public long getSavedTimestamp() {
        return savedTimestamp;
    }

    public void setSavedTimestamp(long savedTimestamp) {
        this.savedTimestamp = savedTimestamp;
    }
}
