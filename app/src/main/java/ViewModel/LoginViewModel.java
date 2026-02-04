package ViewModel;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.graduationproject.BuildConfig;
import com.example.graduationproject.R;
import Model.POJO.SpotifyUser;
import Util.TokenManager;
import com.google.gson.Gson;

import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginViewModel extends AndroidViewModel {
    private static final String SPOTIFY_AUTH_ENDPOINT = "https://accounts.spotify.com/authorize";
    private static final String SPOTIFY_TOKEN_ENDPOINT = "https://accounts.spotify.com/api/token";
    private static final String SPOTIFY_USER_API = "https://api.spotify.com/v1/me";
    // OAuth Scope - 包含 streaming 權限以支持 Spotify App Remote SDK 播放控制
    private static final String SCOPE = "user-read-private user-read-email streaming user-read-playback-state user-modify-playback-state";

    private final MutableLiveData<String> _statusText = new MutableLiveData<>();
    public LiveData<String> statusText = _statusText;

    private final MutableLiveData<Intent> _launchAuthIntent = new MutableLiveData<>();
    public LiveData<Intent> launchAuthIntent = _launchAuthIntent;

    // 登入成功後的導航事件
    private final MutableLiveData<Boolean> _navigateToUserMain = new MutableLiveData<>();
    public LiveData<Boolean> navigateToUserMain = _navigateToUserMain;

    private AuthorizationService authService;
    private AuthorizationServiceConfiguration serviceConfig;
    private AuthorizationRequest authRequest; // 保存授權請求，用於從 URI 重建響應
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();
    private final TokenManager tokenManager;

    public LoginViewModel(@NonNull Application application) {
        super(application);
        authService = new AuthorizationService(application);
        tokenManager = new TokenManager(application);
        //對OAuth初始化
        serviceConfig = new AuthorizationServiceConfiguration(
                Uri.parse(SPOTIFY_AUTH_ENDPOINT),
                Uri.parse(SPOTIFY_TOKEN_ENDPOINT)
        );
        // 初始化狀態文字
        _statusText.setValue(getApplication().getString(R.string.login_status_no_login));
    }

    /**
     * 檢查是否已登入
     */
    public boolean isAlreadyLoggedIn() {
        return tokenManager.isLoggedIn();
    }

    /**
     * 重置導航事件
     */
    public void onNavigationComplete() {
        _navigateToUserMain.setValue(false);
    }

    //狀態文字更新（以後也許可以換成動畫？）
    public void updateStatusText(String status) {
        _statusText.postValue(status);
    }

    //button按下後的反應
    public void onLoginClick(){
        _statusText.setValue(getApplication().getString(R.string.login_status_login));
        startSpotifyOAuth();

    }

    /**
     * 啟動Spotify OAuth 授權流程
     */
    private void startSpotifyOAuth() {
        // 創建授權請求
        authRequest = new AuthorizationRequest.Builder(
                serviceConfig,
                BuildConfig.SPOTIFY_CLIENT_ID,
                ResponseTypeValues.CODE,
                Uri.parse(BuildConfig.SPOTIFY_REDIRECT_URI)
        )
        .setScope(SCOPE)
        .build();

        Intent authIntent = authService.getAuthorizationRequestIntent(authRequest);
        _launchAuthIntent.setValue(authIntent);
    }

    /**
     * 清除啟動 Intent，避免重複啟動
     */
    public void clearLaunchIntent() {
        _launchAuthIntent.setValue(null);
    }

    /**
     * 從 URI 直接處理授權碼
     * 當 AuthorizationResponse.fromIntent() 返回 null 時使用此方法
     */
    public void handleAuthCodeFromUri(Uri uri) {
        if (uri == null || authRequest == null) {
            _statusText.postValue(getApplication().getString(R.string.login_status_failed_invalid_callback));
            return;
        }

        _statusText.postValue(getApplication().getString(R.string.login_status_processing));

        // 使用保存的 authRequest 創建 AuthorizationResponse
        AuthorizationResponse response = new AuthorizationResponse.Builder(authRequest)
                .fromUri(uri)
                .build();

        if (response.authorizationCode != null) {
            handleAuthResponse(response);
        } else {
            _statusText.postValue(getApplication().getString(R.string.login_status_failed_no_code));
        }
    }

    // 處理網頁的信息回調，並獲取token當中的用戶訊息
    public void handleAuthResponse(AuthorizationResponse response) {
        _statusText.postValue(getApplication().getString(R.string.login_status_getting_token));

        authService.performTokenRequest(
                response.createTokenExchangeRequest(),
                (tokenResponse, exception) -> {
                    if (tokenResponse != null && tokenResponse.accessToken != null) {
                        // 保存 Access Token
                        tokenManager.saveAccessToken(tokenResponse.accessToken);

                        // 保存 Refresh Token（如果有）
                        if (tokenResponse.refreshToken != null) {
                            tokenManager.saveRefreshToken(tokenResponse.refreshToken);
                        }

                        // 保存 Token 過期時間
                        if (tokenResponse.accessTokenExpirationTime != null) {
                            tokenManager.saveTokenExpiry(tokenResponse.accessTokenExpirationTime);
                        }

                        // 成功獲取 Access Token，調用 API 獲取用戶名
                        fetchSpotifyUserName(tokenResponse.accessToken);
                    } else {
                        String error = exception != null ? exception.getMessage() : "error";
                        _statusText.postValue(getApplication().getString(R.string.login_status_error_with_msg, error));
                    }
                }
        );
    }

    //調用 Spotify Web API 獲取用戶名
    private void fetchSpotifyUserName(String accessToken) {
        Request request = new Request.Builder()
                .url(SPOTIFY_USER_API)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                _statusText.postValue(getApplication().getString(R.string.login_status_failed_user_info));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    SpotifyUser user = gson.fromJson(json, SpotifyUser.class);

                    String displayName = user.displayName != null ? user.displayName : user.id;
                    String profileImageUrl = user.getProfileImageUrl();

                    // 保存用戶資訊
                    tokenManager.saveUserInfo(user.id, displayName, user.email, profileImageUrl);

                    _statusText.postValue(getApplication().getString(R.string.login_status_success, displayName));

                    // 觸發導航到 UserMainActivity
                    _navigateToUserMain.postValue(true);
                } else {
                    _statusText.postValue(getApplication().getString(R.string.login_status_error));
                }
            }
        });
    }

    /**
     * 登錄失敗
     */
    public void handleLoginFailure(String errorMessage) {
        _statusText.postValue(getApplication().getString(R.string.login_status_error_with_msg, errorMessage));
    }

    /**
     * 登錄取消
     */
    public void handleLoginCancelled() {
        _statusText.postValue(getApplication().getString(R.string.login_status_cancelled));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (authService != null) {
            authService.dispose();
        }
    }

}
