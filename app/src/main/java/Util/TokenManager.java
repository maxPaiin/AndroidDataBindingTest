package Util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 管理 Spotify Token 和用戶資訊的存儲
 * 使用 SharedPreferences 進行本地持久化存儲
 */
public class TokenManager {
    private static final String PREF_NAME = "spotify_auth";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PROFILE_IMAGE_URL = "profile_image_url";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry";

    private final SharedPreferences prefs;

    public TokenManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 保存 Access Token
     */
    public void saveAccessToken(String accessToken) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, accessToken).apply();
    }

    /**
     * 獲取 Access Token
     */
    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    /**
     * 保存 Refresh Token
     */
    public void saveRefreshToken(String refreshToken) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, refreshToken).apply();
    }

    /**
     * 獲取 Refresh Token
     */
    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    /**
     * 保存 Token 過期時間
     */
    public void saveTokenExpiry(long expiryTimeMillis) {
        prefs.edit().putLong(KEY_TOKEN_EXPIRY, expiryTimeMillis).apply();
    }

    /**
     * 獲取 Token 過期時間
     */
    public long getTokenExpiry() {
        return prefs.getLong(KEY_TOKEN_EXPIRY, 0);
    }

    /**
     * 保存用戶資訊
     */
    public void saveUserInfo(String userId, String displayName, String email, String profileImageUrl) {
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_DISPLAY_NAME, displayName)
                .putString(KEY_EMAIL, email)
                .putString(KEY_PROFILE_IMAGE_URL, profileImageUrl)
                .apply();
    }

    /**
     * 獲取用戶顯示名稱
     */
    public String getDisplayName() {
        return prefs.getString(KEY_DISPLAY_NAME, null);
    }

    /**
     * 獲取用戶 ID
     */
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    /**
     * 獲取用戶 Email
     */
    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    /**
     * 獲取用戶頭像 URL
     */
    public String getProfileImageUrl() {
        return prefs.getString(KEY_PROFILE_IMAGE_URL, null);
    }

    /**
     * 檢查用戶是否已登入
     * 判斷條件：有 Access Token 且未過期
     */
    public boolean isLoggedIn() {
        String token = getAccessToken();
        if (token == null || token.isEmpty()) {
            return false;
        }
        // 檢查 Token 是否過期（預留 5 分鐘緩衝）
        long expiry = getTokenExpiry();
        if (expiry > 0) {
            return System.currentTimeMillis() < (expiry - 5 * 60 * 1000);
        }
        // 如果沒有設置過期時間，只檢查 token 是否存在
        return true;
    }

    /**
     * 清除所有登入資訊（登出）
     */
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
