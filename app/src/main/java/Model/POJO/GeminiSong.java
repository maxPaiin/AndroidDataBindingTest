package Model.POJO;

import com.google.gson.annotations.SerializedName;

/**
 * Gemini API 回傳的歌曲數據模型
 */
public class GeminiSong {
    @SerializedName("song_name")
    private String songName;

    @SerializedName("artist")
    private String artist;

    public GeminiSong() {}

    public GeminiSong(String songName, String artist) {
        this.songName = songName;
        this.artist = artist;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
}
