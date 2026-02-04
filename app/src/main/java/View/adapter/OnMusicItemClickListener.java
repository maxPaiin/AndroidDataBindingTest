package View.adapter;

import Model.POJO.MusicItem;

/**
 * 音樂項目點擊事件監聽器接口
 * 獨立出來以便復用
 */
public interface OnMusicItemClickListener {
    void onPlayClick(MusicItem item, int position);
    void onPauseClick(MusicItem item, int position);
    void onItemClick(MusicItem item, int position);
    void onLikeClick(MusicItem item, int position);
}
