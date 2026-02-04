package View.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.graduationproject.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Model.POJO.MusicItem;

/**
 * 音樂列表適配器
 * 用於 ListView 顯示音樂項目
 */
public class MusicListAdapter extends BaseAdapter {

    private final Context context;
    private List<MusicItem> musicItems;
    private OnMusicItemClickListener listener;
    private String currentPlayingTrackId;
    private boolean isPlaying;
    private final Map<String, Boolean> favoriteStatusCache = new HashMap<>();

    public MusicListAdapter(Context context) {
        this.context = context;
        this.musicItems = new ArrayList<>();
        this.currentPlayingTrackId = null;
        this.isPlaying = false;
    }

    /**
     * 更新音樂列表數據
     */
    public void updateData(List<MusicItem> newMusicItems) {
        if (newMusicItems != null) {
            this.musicItems = new ArrayList<>(newMusicItems);
        } else {
            this.musicItems = new ArrayList<>();
        }
        notifyDataSetChanged();
    }

    /**
     * 更新當前播放狀態
     */
    public void updatePlaybackState(String trackId, boolean isPlaying) {
        this.currentPlayingTrackId = trackId;
        this.isPlaying = isPlaying;
        notifyDataSetChanged();
    }

    /**
     * 更新單個歌曲的收藏狀態
     * @param trackId Spotify Track ID
     * @param isFavorite 是否已收藏
     */
    public void updateFavoriteStatus(String trackId, boolean isFavorite) {
        if (trackId != null) {
            favoriteStatusCache.put(trackId, isFavorite);
            notifyDataSetChanged();
        }
    }

    /**
     * 設置點擊事件監聽器
     */
    public void setOnMusicItemClickListener(OnMusicItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return musicItems.size();
    }

    @Override
    public MusicItem getItem(int position) {
        return musicItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_music, parent, false);
            holder = new ViewHolder();
            holder.ivAlbumCover = convertView.findViewById(R.id.iv_album_cover);
            holder.tvSongName = convertView.findViewById(R.id.tv_song_name);
            holder.tvArtistName = convertView.findViewById(R.id.tv_artist_name);
            holder.btnLike = convertView.findViewById(R.id.btn_like);
            holder.btnPlayPause = convertView.findViewById(R.id.btn_play_pause);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        MusicItem item = getItem(position);

        // 設置歌曲名稱
        holder.tvSongName.setText(item.getSongName() != null ? item.getSongName() : context.getString(R.string.text_unknown_song));

        // 設置藝術家名稱
        holder.tvArtistName.setText(item.getArtistName() != null ? item.getArtistName() : context.getString(R.string.text_unknown_artist));

        // 使用 Glide 加載專輯封面
        if (item.getAlbumImageUrl() != null && !item.getAlbumImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getAlbumImageUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(holder.ivAlbumCover);
        } else {
            holder.ivAlbumCover.setImageResource(R.drawable.ic_launcher_background);
        }

        // 檢查當前項目是否正在播放
        boolean isCurrentTrack = item.getSpotifyTrackId() != null &&
                item.getSpotifyTrackId().equals(currentPlayingTrackId);
        boolean isCurrentlyPlaying = isCurrentTrack && isPlaying;

        // 圖標邏輯與 layout_mini_player.xml 的 playPauseIconWithBg 保持一致：
        // 播放中 -> 顯示 play 圖示；暫停中 -> 顯示 pause 圖示
        holder.btnPlayPause.setImageResource(isCurrentlyPlaying ?
                R.drawable.outline_play_arrow_24 : R.drawable.outline_pause_24);

        holder.btnPlayPause.setOnClickListener(v -> {
            if (listener != null) {
                if (isCurrentlyPlaying) {
                    listener.onPauseClick(item, position);
                } else {
                    listener.onPlayClick(item, position);
                }
            }
        });

        // 根據收藏狀態設置圖標
        boolean isFavorite = Boolean.TRUE.equals(favoriteStatusCache.getOrDefault(item.getSpotifyTrackId(), false));
        holder.btnLike.setImageResource(isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite);

        // 設置 Like 按鈕點擊事件
        holder.btnLike.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLikeClick(item, position);
            }
        });

        // 設置整個 item 點擊事件（跳轉到播放器頁面）
        convertView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item, position);
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        ImageView ivAlbumCover;
        TextView tvSongName;
        TextView tvArtistName;
        ImageButton btnLike;
        ImageButton btnPlayPause;
//        Button btnPlay;
//        Button btnPause;
    }

    /**
     * 獲取音樂列表
     */
    public List<MusicItem> getMusicItems() {
        return musicItems;
    }
}
