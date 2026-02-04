package Model.Api;

import Model.POJO.SpotifySearchResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

/**
 * Spotify Web API 服務接口
 */
public interface SpotifyApiService {

    /**
     * 搜索歌曲
     * @param authorization Bearer token
     * @param query 搜索關鍵字 (歌名 + 藝術家)
     * @param type 搜索類型 (track, artist, album 等)
     * @param limit 返回結果數量限制
     */
    @GET("v1/search")
    Call<SpotifySearchResponse> searchTracks(
            @Header("Authorization") String authorization,
            @Query("q") String query,
            @Query("type") String type,
            @Query("limit") int limit
    );
}
