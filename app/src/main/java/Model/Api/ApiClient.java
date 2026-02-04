package Model.Api;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * API Client 單例類
 * 提供 Gemini 和 Spotify API 的 Retrofit 實例
 */
public class ApiClient {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/";
    private static final String SPOTIFY_BASE_URL = "https://api.spotify.com/";

    // 超時時間設置為 360 秒
    private static final long TIMEOUT_SECONDS = 360;

    private static Retrofit geminiRetrofit = null;
    private static Retrofit spotifyRetrofit = null;
    private static OkHttpClient okHttpClient = null;

    private static OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build();
        }
        return okHttpClient;
    }

    /**
     * 獲取 Gemini API Retrofit 實例
     */
    public static Retrofit getGeminiRetrofit() {
        if (geminiRetrofit == null) {
            geminiRetrofit = new Retrofit.Builder()
                    .baseUrl(GEMINI_BASE_URL)
                    .client(getOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return geminiRetrofit;
    }

    /**
     * 獲取 Spotify API Retrofit 實例
     */
    public static Retrofit getSpotifyRetrofit() {
        if (spotifyRetrofit == null) {
            spotifyRetrofit = new Retrofit.Builder()
                    .baseUrl(SPOTIFY_BASE_URL)
                    .client(getOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return spotifyRetrofit;
    }

    /**
     * 獲取 Gemini API 服務
     */
    public static GeminiApiService getGeminiApiService() {
        return getGeminiRetrofit().create(GeminiApiService.class);
    }

    /**
     * 獲取 Spotify API 服務
     */
    public static SpotifyApiService getSpotifyApiService() {
        return getSpotifyRetrofit().create(SpotifyApiService.class);
    }
}
