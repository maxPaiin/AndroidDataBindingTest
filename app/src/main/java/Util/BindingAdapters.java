package Util;

import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.databinding.BindingAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.graduationproject.R;

/**
 * Data Binding 自定義適配器
 * 用於 XML 中綁定圖片 URL 到 ImageView
 */
public class BindingAdapters {
    // 常量：專輯封面圓角半徑（像素）
    private static final int ALBUM_CORNER_RADIUS_PX = 16;

    /**
     * 綁定圓形頭像圖片
     * 使用方式：app:profileImageUrl="@{viewModel.profileImageUrl}"
     */
    @BindingAdapter("profileImageUrl")
    public static void loadProfileImage(ImageView imageView, String url) {
        if (url != null && !url.isEmpty()) {
            Glide.with(imageView.getContext())
                    .load(url)
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_launcher_background);
        }
    }

    /**
     * 綁定專輯封面圖片（圓角）
     * 使用方式：app:albumImageUrl="@{viewModel.currentTrack.albumImageUrl}"
     */
    @BindingAdapter("albumImageUrl")
    public static void loadAlbumImage(ImageView imageView, String url) {
        if (url != null && !url.isEmpty()) {
            Glide.with(imageView.getContext())
                    .load(url)
                    .transform(new RoundedCorners(ALBUM_CORNER_RADIUS_PX))
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_launcher_background);
        }
    }

    /**
     * 綁定播放/暫停按鈕圖示
     */
    @BindingAdapter("playPauseIcon")
    public static void setPlayPauseIcon(ImageButton imageButton, Boolean isPlaying) {
        if (isPlaying != null && isPlaying) {
            imageButton.setImageResource(R.drawable.ic_pause);
        } else {
            imageButton.setImageResource(R.drawable.ic_play);
        }
    }

    /**
     * 綁定播放/暫停按鈕圖示（含背景色變化）
     * 播放中：顯示播放圖示，白色背景
     * 暫停中：顯示暫停圖示，背景色為 color_emotion_angry
     * 默認：播放圖示，白色背景
     * 使用方式：app:playPauseIconWithBg="@{viewModel.isPlaying}"
     */
    @BindingAdapter("playPauseIconWithBg")
    public static void setPlayPauseIconWithBackground(ImageButton imageButton, Boolean isPlaying) {
        if (isPlaying != null && isPlaying) {
            // 播放中 -> 顯示播放圖示，白色背景
            imageButton.setImageResource(R.drawable.outline_play_arrow_24);
            imageButton.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            imageButton.getContext().getColor(R.color.white)));
        } else {
            // 暫停中 -> 顯示暫停圖示，紅色背景
            imageButton.setImageResource(R.drawable.outline_pause_24);
            imageButton.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            imageButton.getContext().getColor(R.color.color_emotion_angry)));
        }
    }

    /**
     * 綁定收藏按鈕圖示
     * 使用方式：app:favoriteIcon="@{viewModel.isFavorite}"
     */
    @BindingAdapter("favoriteIcon")
    public static void setFavoriteIcon(ImageButton imageButton, Boolean isFavorite) {
        if (isFavorite != null && isFavorite) {
            imageButton.setImageResource(R.drawable.ic_favorite_filled);
        } else {
            imageButton.setImageResource(R.drawable.ic_favorite);
        }
    }
}
