package Model.POJO;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 音樂項目數據模型
 * 用於 ListView 顯示的最終數據結構
 * 實現 Parcelable 接口以支持 Intent 傳遞
 */
public class MusicItem implements Parcelable {
    private String songName;
    private String artistName;
    private String albumImageUrl;   // 用於列表的縮略圖
    private String largeImageUrl;   // 用於播放器的高清圖
    private String spotifyTrackId;
    private long durationMs;        // 歌曲時長（毫秒）
    private boolean isPlaying;

    public MusicItem() {}

    public MusicItem(String songName, String artistName, String albumImageUrl, String spotifyTrackId) {
        this.songName = songName;
        this.artistName = artistName;
        this.albumImageUrl = albumImageUrl;
        this.largeImageUrl = null;
        this.spotifyTrackId = spotifyTrackId;
        this.durationMs = 0;
        this.isPlaying = false;
    }

    public MusicItem(String songName, String artistName, String albumImageUrl, String spotifyTrackId, long durationMs) {
        this.songName = songName;
        this.artistName = artistName;
        this.albumImageUrl = albumImageUrl;
        this.largeImageUrl = null;
        this.spotifyTrackId = spotifyTrackId;
        this.durationMs = durationMs;
        this.isPlaying = false;
    }

    public MusicItem(String songName, String artistName, String albumImageUrl, String largeImageUrl, String spotifyTrackId, long durationMs) {
        this.songName = songName;
        this.artistName = artistName;
        this.albumImageUrl = albumImageUrl;
        this.largeImageUrl = largeImageUrl;
        this.spotifyTrackId = spotifyTrackId;
        this.durationMs = durationMs;
        this.isPlaying = false;
    }

    // Parcelable 構造函數
    protected MusicItem(Parcel in) {
        songName = in.readString();
        artistName = in.readString();
        albumImageUrl = in.readString();
        largeImageUrl = in.readString();
        spotifyTrackId = in.readString();
        durationMs = in.readLong();
        isPlaying = in.readByte() != 0;
    }

    public static final Creator<MusicItem> CREATOR = new Creator<MusicItem>() {
        @Override
        public MusicItem createFromParcel(Parcel in) {
            return new MusicItem(in);
        }

        @Override
        public MusicItem[] newArray(int size) {
            return new MusicItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(songName);
        dest.writeString(artistName);
        dest.writeString(albumImageUrl);
        dest.writeString(largeImageUrl);
        dest.writeString(spotifyTrackId);
        dest.writeLong(durationMs);
        dest.writeByte((byte) (isPlaying ? 1 : 0));
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getAlbumImageUrl() {
        return albumImageUrl;
    }

    public void setAlbumImageUrl(String albumImageUrl) {
        this.albumImageUrl = albumImageUrl;
    }

    public String getLargeImageUrl() {
        return largeImageUrl;
    }

    public void setLargeImageUrl(String largeImageUrl) {
        this.largeImageUrl = largeImageUrl;
    }

    public String getSpotifyTrackId() {
        return spotifyTrackId;
    }

    public void setSpotifyTrackId(String spotifyTrackId) {
        this.spotifyTrackId = spotifyTrackId;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    /**
     * 獲取格式化的歌曲時長
     * @return 格式化的時長字串，如 "3:45"
     */
    public String getFormattedDuration() {
        if (durationMs <= 0) {
            return "0:00";
        }
        long totalSeconds = durationMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * 獲取 Spotify Track URI
     * @return spotify:track:xxxx 格式的 URI
     */
    public String getSpotifyUri() {
        if (spotifyTrackId == null || spotifyTrackId.isEmpty()) {
            return null;
        }
        if (spotifyTrackId.startsWith("spotify:track:")) {
            return spotifyTrackId;
        }
        return "spotify:track:" + spotifyTrackId;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }
}
