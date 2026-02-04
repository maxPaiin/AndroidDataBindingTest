package View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.example.graduationproject.R;
import ViewModel.LoginViewModel;
import com.example.graduationproject.databinding.ActivityMainBinding;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;

/**
 * 登錄頁面 Activity
 * OAuth 登錄流程：
 * 1. 用戶點擊登錄按鈕
 * 2. 啟動瀏覽器進行 Spotify OAuth 授權
 * 3. 授權完成後，瀏覽器通過 deep link (myapp://callback) 返回
 * 4. singleTop launchMode 確保 onNewIntent() 被調用
 * 5. 處理授權回調，交換 token
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private LoginViewModel loginViewModel;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);//強制dark model

        // 初始化 ViewModel
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // 檢查登錄狀態，若已登錄則直接跳轉到 UserMainActivity
        if (loginViewModel.isAlreadyLoggedIn()) {
            navigateToUserMain();
            return;
        }

        // 未登入，顯示登入頁面
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setLgv(loginViewModel);
        binding.setLifecycleOwner(this);

        // 監聽 OAuth 授權 Intent - 直接啟動瀏覽器
        loginViewModel.launchAuthIntent.observe(this, intent -> {
            if (intent != null) {
                Log.d(TAG, "啟動 OAuth 授權流程");
                startActivity(intent);
                // 重置 Intent，避免重複啟動
                loginViewModel.clearLaunchIntent();
            }
        });

        // 監聽登入成功後的導航事件
        loginViewModel.navigateToUserMain.observe(this, shouldNavigate -> {
            if (shouldNavigate != null && shouldNavigate) {
                loginViewModel.onNavigationComplete();
                navigateToUserMain();
            }
        });

        // 處理啟動時的 Intent（可能是 OAuth 回調）
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Log.d(TAG, "onNewIntent 被調用");

        // 處理 OAuth 回調（singleTop 模式下，Activity 已在棧頂時會調用此方法）
        handleIntent(intent);
    }

    /**
     * 處理 Intent，檢查是否為 OAuth 回調
     */
    private void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        Uri uri = intent.getData();
        if (uri != null) {
            String scheme = uri.getScheme();
            String host = uri.getHost();

            Log.d(TAG, "收到 Intent URI: " + uri.toString());
            Log.d(TAG, "Scheme: " + scheme + ", Host: " + host);

            // 檢查是否為 OAuth 回調 (myapp://callback)
            if ("myapp".equals(scheme) && "callback".equals(host)) {
                Log.d(TAG, "處理 OAuth 回調");

                // 從 URI 解析意圖
                AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
                AuthorizationException exception = AuthorizationException.fromIntent(intent);

                // 如果 fromIntent 返回 null，嘗試直接從 URI 解析
                if (response == null && exception == null) {
                    // 檢查 URI 中是否有授權碼或錯誤
                    String code = uri.getQueryParameter("code");
                    String error = uri.getQueryParameter("error");

                    if (code != null) {
                        Log.d(TAG, "從 URI 獲取到授權碼: " + code.substring(0, Math.min(10, code.length())) + "...");
                        // 需要手動創建 AuthorizationResponse
                        loginViewModel.handleAuthCodeFromUri(uri);
                    } else if (error != null) {
                        Log.e(TAG, "OAuth 錯誤: " + error);
                        loginViewModel.handleLoginFailure("授權失敗: " + error);
                    } else {
                        Log.e(TAG, "無法解析 OAuth 回調");
                        loginViewModel.handleLoginCancelled();
                    }
                } else if (response != null) {
                    Log.d(TAG, "成功解析 AuthorizationResponse");
                    loginViewModel.handleAuthResponse(response);
                } else {
                    Log.e(TAG, "OAuth 異常: " + exception.getMessage());
                    loginViewModel.handleLoginFailure(exception.getMessage());
                }

                // 清除 Intent 數據，避免重複處理
                intent.setData(null);
            }
        }
    }

    /**
     * 導航到 UserMainActivity
     */
    private void navigateToUserMain() {
        Intent intent = new Intent(this, UserMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}