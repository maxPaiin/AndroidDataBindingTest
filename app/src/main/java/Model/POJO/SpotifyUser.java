package Model.POJO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.google.gson.annotations.SerializedName;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpotifyUser {
    @SerializedName("id")
    public String id;

    @SerializedName("display_name")
    public String displayName;

    @SerializedName("email")
    public String email;

    @SerializedName("images")
    public List<SpotifyImage> images;

    /**
     * 獲取用戶頭像 URL
     * @return 頭像 URL，如果沒有則返回 null
     */
    public String getProfileImageUrl() {
        if (images != null && !images.isEmpty()) {
            return images.get(0).url;
        }
        return null;
    }

    /**
     * Spotify 用戶頭像圖片
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SpotifyImage {
        @SerializedName("url")
        public String url;

        @SerializedName("height")
        public Integer height;

        @SerializedName("width")
        public Integer width;
    }
}
