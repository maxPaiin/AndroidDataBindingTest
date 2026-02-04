package Model.POJO;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * 歌單數據封裝類
 * 用於 Intent 傳遞歌單列表和當前播放索引
 */
public class PlaylistData implements Parcelable {
    private List<MusicItem> items;
    private int currentIndex;

    public PlaylistData() {
        this.items = new ArrayList<>();
        this.currentIndex = 0;
    }

    public PlaylistData(List<MusicItem> items, int currentIndex) {
        this.items = items != null ? items : new ArrayList<>();
        this.currentIndex = currentIndex;
    }

    protected PlaylistData(Parcel in) {
        items = in.createTypedArrayList(MusicItem.CREATOR);
        currentIndex = in.readInt();
    }

    public static final Creator<PlaylistData> CREATOR = new Creator<PlaylistData>() {
        @Override
        public PlaylistData createFromParcel(Parcel in) {
            return new PlaylistData(in);
        }

        @Override
        public PlaylistData[] newArray(int size) {
            return new PlaylistData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(items);
        dest.writeInt(currentIndex);
    }

    public List<MusicItem> getItems() {
        return items;
    }

    public void setItems(List<MusicItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    /**
     * 獲取當前歌曲
     */
    public MusicItem getCurrentItem() {
        if (items != null && currentIndex >= 0 && currentIndex < items.size()) {
            return items.get(currentIndex);
        }
        return null;
    }

    /**
     * 獲取歌單大小
     */
    public int size() {
        return items != null ? items.size() : 0;
    }

    /**
     * 檢查歌單是否為空
     */
    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }
}
