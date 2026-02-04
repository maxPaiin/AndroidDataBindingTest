package Model.Repository;

import android.util.Log;

import com.example.graduationproject.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import Model.Api.ApiClient;
import Model.Api.GeminiApiService;
import Model.Api.SpotifyApiService;
import Model.POJO.EmotionInput;
import Model.POJO.GeminiRequest;
import Model.POJO.GeminiResponse;
import Model.POJO.GeminiSong;
import Model.POJO.MusicItem;
import Model.POJO.SpotifySearchResponse;
import retrofit2.Response;

/**
 * 音樂數據倉庫
 * 處理 Gemini 和 Spotify API 請求
 */
public class MusicRepository {

    private static final String TAG = "MusicRepository";
    private static final int MAX_SONGS = 8;

    private final GeminiApiService geminiApiService;
    private final SpotifyApiService spotifyApiService;
    private final Gson gson;
    private final ExecutorService executorService;

    public MusicRepository() {
        this.geminiApiService = ApiClient.getGeminiApiService();
        this.spotifyApiService = ApiClient.getSpotifyApiService();
        this.gson = new Gson();
        this.executorService = Executors.newFixedThreadPool(5);
    }

    /**
     * 根據情緒獲取音樂推薦
     * @param emotionInput 情緒輸入
     * @param accessToken Spotify Access Token
     * @param callback 回調接口
     */
    public void getMusicRecommendations(EmotionInput emotionInput, String accessToken,
                                        MusicRepositoryCallback callback) {
        CompletableFuture.supplyAsync(() -> {
            try {
                // Step 1: 調用 Gemini API 獲取歌曲推薦
                List<GeminiSong> geminiSongs = fetchGeminiRecommendations(emotionInput);
                if (geminiSongs == null || geminiSongs.isEmpty()) {
                    throw new Exception("Gemini 未返回有效的歌曲推薦");
                }

                // 截取前8條
                List<GeminiSong> topSongs = geminiSongs.stream()
                        .limit(MAX_SONGS)
                        .collect(Collectors.toList());

                Log.d(TAG, "Gemini 返回 " + topSongs.size() + " 首歌曲");

                // Step 2: 並發請求 Spotify API 獲取歌曲詳情
                List<MusicItem> musicItems = fetchSpotifyDetailsParallel(topSongs, accessToken);

                return musicItems;

            } catch (Exception e) {
                Log.e(TAG, "獲取音樂推薦失敗", e);
                throw new RuntimeException(e);
            }
        }, executorService).thenAccept(musicItems -> {
            callback.onSuccess(musicItems);
        }).exceptionally(throwable -> {
            callback.onError(throwable.getMessage());
            return null;
        });
    }

    /**
     * 調用 Gemini API 獲取歌曲推薦
     */
    private List<GeminiSong> fetchGeminiRecommendations(EmotionInput emotionInput) throws IOException {
        String prompt = emotionInput.buildPrompt();
        GeminiRequest request = new GeminiRequest(prompt);

        Response<GeminiResponse> response = geminiApiService
                .generateContent(BuildConfig.GEMINI_API_KEY, request)
                .execute();

        if (response.isSuccessful() && response.body() != null) {
            String responseText = response.body().getResponseText();
            Log.d(TAG, "Gemini 原始回應: " + responseText);

            if (responseText != null) {
                // 清理可能的 Markdown 標記
                responseText = cleanJsonResponse(responseText);
                Log.d(TAG, "清理後的 JSON: " + responseText);

                // 解析 JSON 數組
                Type listType = new TypeToken<List<GeminiSong>>(){}.getType();
                return gson.fromJson(responseText, listType);
            }
        } else {
            int errorCode = response.code();
            Log.e(TAG, "Gemini API 請求失敗: " + errorCode);
            String errorDetail = "";
            if (response.errorBody() != null) {
                errorDetail = response.errorBody().string();
                Log.e(TAG, "錯誤詳情: " + errorDetail);
            }
            // 檢測限流錯誤（429 或包含 quota/rate limit 關鍵字）
            if (errorCode == 429 || errorDetail.contains("quota") || errorDetail.contains("RATE_LIMIT") ||
                errorDetail.contains("rate limit") || errorDetail.contains("Resource has been exhausted")) {
                throw new RateLimitException("API request limit reached");
            }
        }

        return null;
    }

    /**
     * API 限流異常
     */
    public static class RateLimitException extends IOException {
        public RateLimitException(String message) {
            super(message);
        }
    }

    /**
     * 清理 JSON 回應中的 Markdown 標記
     */
    private String cleanJsonResponse(String response) {
        if (response == null) return null;

        String cleaned = response.trim();

        // 移除 markdown 代碼塊標記
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }

    /**
     * 並發請求 Spotify API 獲取歌曲詳情
     * 使用 CompletableFuture 實現並行請求
     */
    private List<MusicItem> fetchSpotifyDetailsParallel(List<GeminiSong> songs, String accessToken) {
        String authHeader = "Bearer " + accessToken;

        // 創建所有 CompletableFuture
        List<CompletableFuture<MusicItem>> futures = songs.stream()
                .map(song -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return searchSpotifyTrack(song, authHeader);
                    } catch (Exception e) {
                        Log.e(TAG, "搜索歌曲失敗: " + song.getSongName(), e);
                        // 搜索失敗時返回 null，後續會過濾掉
                        return null;
                    }
                }, executorService))
                .collect(Collectors.toList());

        // 等待所有請求完成並收集結果
        // 過濾掉 null 和沒有有效 spotifyTrackId 的項目
        return futures.stream()
                .map(CompletableFuture::join)
                .filter(item -> item != null && item.getSpotifyTrackId() != null && !item.getSpotifyTrackId().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 搜索單首歌曲的 Spotify 詳情
     */
    private MusicItem searchSpotifyTrack(GeminiSong song, String authHeader) throws IOException {
        // 構建搜索查詢: "track:歌名 artist:藝術家"
        String query = "track:" + song.getSongName() + " artist:" + song.getArtist();

        Response<SpotifySearchResponse> response = spotifyApiService
                .searchTracks(authHeader, query, "track", 1)
                .execute();

        if (response.isSuccessful() && response.body() != null) {
            SpotifySearchResponse searchResponse = response.body();

            if (searchResponse.getTracks() != null &&
                searchResponse.getTracks().getItems() != null &&
                !searchResponse.getTracks().getItems().isEmpty()) {

                SpotifySearchResponse.Track track = searchResponse.getTracks().getItems().get(0);

                return new MusicItem(
                        track.getName(),
                        track.getFirstArtistName(),
                        track.getThumbnailUrl(),
                        track.getLargeImageUrl(),
                        track.getId(),
                        track.getDurationMs()
                );
            } else {
                Log.w(TAG, "Spotify 搜索無結果: " + song.getSongName() + " by " + song.getArtist());
            }
        } else {
            Log.e(TAG, "Spotify 搜索失敗: " + response.code() + " for " + song.getSongName());
        }

        // 如果搜索失敗或無結果，返回 null（會被過濾掉）
        return null;
    }

    /**
     * 根據用戶直接輸入的情緒文字獲取音樂推薦
     * 遵循 SRP 原則，獨立於情緒數值推薦方法
     * @param emotionText 用戶輸入的情緒文字（20字以內）
     * @param accessToken Spotify Access Token
     * @param callback 回調接口
     */
    public void getMusicByDirectText(String emotionText, String accessToken,
                                     MusicRepositoryCallback callback) {
        CompletableFuture.supplyAsync(() -> {
            try {
                // Step 1: 調用 Gemini API 分析情緒文字並獲取歌曲推薦
                List<GeminiSong> geminiSongs = fetchGeminiRecommendationsByText(emotionText);
                if (geminiSongs == null || geminiSongs.isEmpty()) {
                    throw new Exception("Gemini 未返回有效的歌曲推薦");
                }

                // 截取前8條
                List<GeminiSong> topSongs = geminiSongs.stream()
                        .limit(MAX_SONGS)
                        .collect(Collectors.toList());

                Log.d(TAG, "Gemini 返回 " + topSongs.size() + " 首歌曲（直接輸入模式）");

                // Step 2: 並發請求 Spotify API 獲取歌曲詳情
                List<MusicItem> musicItems = fetchSpotifyDetailsParallel(topSongs, accessToken);

                return musicItems;

            } catch (Exception e) {
                Log.e("apiError", "直接輸入模式獲取音樂推薦失敗: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executorService).thenAccept(musicItems -> {
            callback.onSuccess(musicItems);
        }).exceptionally(throwable -> {
            Log.e("apiError", "API 錯誤: " + throwable.getMessage());
            callback.onError(throwable.getMessage());
            return null;
        });
    }

    /**
     * 調用 Gemini API 分析情緒文字並獲取歌曲推薦
     * 使用 hard-coded prompt，強制 Gemini 返回 JSON 格式
     */
    private List<GeminiSong> fetchGeminiRecommendationsByText(String emotionText) throws IOException {
        // Hard-coded prompt，強制返回 JSON 格式的歌曲列表
        String prompt = "You are a music recommendation assistant. " +
                "The user describes their current emotion as: \"" + emotionText + "\". " +
                "Based on this emotion, recommend 8 songs that match the user's mood. " +
                "You MUST respond with ONLY a valid JSON array, no other text. " +
                "Each object in the array must have exactly two fields: " +
                "\"songName\" (the name of the song) and \"artist\" (the artist name). " +
                "Example format: [{\"songName\":\"Song Title\",\"artist\":\"Artist Name\"}] " +
                "Do not include any explanation, markdown, or additional text. Only the JSON array.";

        GeminiRequest request = new GeminiRequest(prompt);

        Response<GeminiResponse> response = geminiApiService
                .generateContent(BuildConfig.GEMINI_API_KEY, request)
                .execute();

        if (response.isSuccessful() && response.body() != null) {
            String responseText = response.body().getResponseText();
            Log.d(TAG, "Gemini 原始回應（直接輸入）: " + responseText);

            if (responseText != null) {
                // 清理可能的 Markdown 標記
                responseText = cleanJsonResponse(responseText);
                Log.d(TAG, "清理後的 JSON（直接輸入）: " + responseText);

                // 解析 JSON 數組
                Type listType = new TypeToken<List<GeminiSong>>(){}.getType();
                return gson.fromJson(responseText, listType);
            }
        } else {
            int errorCode = response.code();
            Log.e("apiError", "Gemini API 請求失敗: " + errorCode);
            String errorDetail = "";
            if (response.errorBody() != null) {
                errorDetail = response.errorBody().string();
                Log.e("apiError", "錯誤詳情: " + errorDetail);
            }
            // 檢測限流錯誤（429 或包含 quota/rate limit 關鍵字）
            if (errorCode == 429 || errorDetail.contains("quota") || errorDetail.contains("RATE_LIMIT") ||
                errorDetail.contains("rate limit") || errorDetail.contains("Resource has been exhausted")) {
                throw new RateLimitException("API request limit reached");
            }
        }

        return null;
    }

    /**
     * 釋放資源
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    /**
     * 回調接口
     */
    public interface MusicRepositoryCallback {
        void onSuccess(List<MusicItem> musicItems);
        void onError(String errorMessage);
    }
}
