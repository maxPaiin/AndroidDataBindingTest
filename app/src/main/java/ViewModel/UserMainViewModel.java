package ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.graduationproject.R;

import Util.TokenManager;

public class UserMainViewModel extends AndroidViewModel {

    private final MutableLiveData<String> _username = new MutableLiveData<>();
    public LiveData<String> username = _username;
    private final MutableLiveData<String> _welcomeMessage = new MutableLiveData<>();
    public LiveData<String> welcomeMessage = _welcomeMessage;
    private final MutableLiveData<String> _profileImageUrl = new MutableLiveData<>();
    public LiveData<String> profileImageUrl = _profileImageUrl;
    private final TokenManager tokenManager;

    //  Slider 值 ：0-100
    private final MutableLiveData<Float> _happyValue = new MutableLiveData<>(0f);
    public MutableLiveData<Float> happyValue = _happyValue;

    private final MutableLiveData<Float> _sadValue = new MutableLiveData<>(0f);
    public MutableLiveData<Float> sadValue = _sadValue;

    private final MutableLiveData<Float> _angryValue = new MutableLiveData<>(0f);
    public MutableLiveData<Float> angryValue = _angryValue;

    private final MutableLiveData<Float> _disgustValue = new MutableLiveData<>(0f);
    public MutableLiveData<Float> disgustValue = _disgustValue;

    private final MutableLiveData<Float> _fearValue = new MutableLiveData<>(0f);
    public MutableLiveData<Float> fearValue = _fearValue;

    public UserMainViewModel(@NonNull Application application) {
        super(application);
        tokenManager = new TokenManager(application);
        loadUserInfo();
    }

    /**
     * 從 TokenManager 載入用戶資訊
     */
    private void loadUserInfo() {
        String displayName = tokenManager.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            _username.setValue(displayName);
            _welcomeMessage.setValue(getApplication().getString(R.string.welcome_message, displayName));
        } else {
            // 如果沒有顯示名稱，嘗試使用用戶 ID
            String userId = tokenManager.getUserId();
            if (userId != null) {
                _username.setValue(userId);
                _welcomeMessage.setValue(getApplication().getString(R.string.welcome_message, userId));
            } else {
                _username.setValue(getApplication().getString(R.string.default_username));
                _welcomeMessage.setValue(getApplication().getString(R.string.welcome_message_default));
            }
        }

        // 載入用戶頭像 URL
        String imageUrl = tokenManager.getProfileImageUrl();
        _profileImageUrl.setValue(imageUrl);
    }

    /**
     * 獲取用戶 Email
     */
    public String getUserEmail() {
        return tokenManager.getEmail();
    }

    /**
     * 登出
     */
    public void logout() {
        tokenManager.clearAll();
    }

    /**
     * 獲取 Happy 情緒值（int 類型，供 API 使用）
     * @return 0-100 的整數值
     */
    public int getHappyInt() {
        Float value = _happyValue.getValue();
        return value != null ? Math.round(value) : 0;
    }

    /**
     * 獲取 Sad 情緒值（int 類型，供 API 使用）
     * @return 0-100 的整數值
     */
    public int getSadInt() {
        Float value = _sadValue.getValue();
        return value != null ? Math.round(value) : 0;
    }

    /**
     * 獲取 Angry 情緒值（int 類型，供 API 使用）
     * @return 0-100 的整數值
     */
    public int getAngryInt() {
        Float value = _angryValue.getValue();
        return value != null ? Math.round(value) : 0;
    }

    /**
     * 獲取 Disgust 情緒值（int 類型，供 API 使用）
     * @return 0-100 的整數值
     */
    public int getDisgustInt() {
        Float value = _disgustValue.getValue();
        return value != null ? Math.round(value) : 0;
    }

    /**
     * 獲取 Fear 情緒值（int 類型，供 API 使用）
     * @return 0-100 的整數值
     */
    public int getFearInt() {
        Float value = _fearValue.getValue();
        return value != null ? Math.round(value) : 0;
    }
}
