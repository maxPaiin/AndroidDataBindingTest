package Model.POJO;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Spotify Search API 回應數據模型
 */
public class SpotifySearchResponse {

    @SerializedName("tracks")
    private Tracks tracks;

    public Tracks getTracks() {
        return tracks;
    }

    public void setTracks(Tracks tracks) {
        this.tracks = tracks;
    }

    public static class Tracks {
        @SerializedName("items")
        private List<Track> items;

        public List<Track> getItems() {
            return items;
        }

        public void setItems(List<Track> items) {
            this.items = items;
        }
    }

    public static class Track {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("artists")
        private List<Artist> artists;

        @SerializedName("album")
        private Album album;

        @SerializedName("duration_ms")
        private long durationMs;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Artist> getArtists() {
            return artists;
        }

        public void setArtists(List<Artist> artists) {
            this.artists = artists;
        }

        public Album getAlbum() {
            return album;
        }

        public void setAlbum(Album album) {
            this.album = album;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }

        /**
         * 獲取第一位藝術家名稱
         */
        public String getFirstArtistName() {
            if (artists != null && !artists.isEmpty()) {
                return artists.get(0).getName();
            }
            return "Unknown Artist";
        }

        /**
         * 獲取專輯封面縮略圖 URL (優先取最小尺寸)
         * 用於列表顯示
         */
        public String getThumbnailUrl() {
            if (album != null && album.getImages() != null && !album.getImages().isEmpty()) {
                List<Image> images = album.getImages();
                // 取最後一個（64x64）
                return images.get(images.size() - 1).getUrl();
            }
            return null;
        }

        /**
         * 獲取專輯封面高清圖 URL (優先取最大尺寸)
         * 用於播放器頁面顯示
         */
        public String getLargeImageUrl() {
            if (album != null && album.getImages() != null && !album.getImages().isEmpty()) {
                // 取第一個（640x640）
                return album.getImages().get(0).getUrl();
            }
            return null;
        }

        /**
         * 獲取格式化的歌曲時長
         * @return 格式化的時長字串，如 "3:45"
         */
        public String getFormattedDuration() {
            long totalSeconds = durationMs / 1000;
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    public static class Artist {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Album {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("images")
        private List<Image> images;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Image> getImages() {
            return images;
        }

        public void setImages(List<Image> images) {
            this.images = images;
        }
    }

    public static class Image {
        @SerializedName("url")
        private String url;

        @SerializedName("height")
        private int height;

        @SerializedName("width")
        private int width;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }
    }
}
