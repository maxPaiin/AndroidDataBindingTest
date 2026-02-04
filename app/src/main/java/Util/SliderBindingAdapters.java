package Util;

import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;

import com.google.android.material.slider.Slider;

/**
 * Slider 專用 BindingAdapter
 * 處理 Slider 的雙向數據綁定
 * 將 Float 類型的 Slider 值與 ViewModel 中的 LiveData 綁定
 */
public class SliderBindingAdapters {

    /**
     * 設置 Slider 的值（單向綁定：ViewModel -> View）
     * 使用自定義屬性名 "sliderValue" 避免與系統屬性衝突
     *
     * @param slider Slider 控件
     * @param value  要設置的值（Float 類型）
     */
    @BindingAdapter("sliderValue")
    public static void setSliderValue(Slider slider, Float value) {
        if (slider == null) return;

        float newValue = (value != null) ? value : 0f;
        // 確保值在範圍內
        newValue = Math.max(slider.getValueFrom(), Math.min(slider.getValueTo(), newValue));

        // 避免無限循環：只有當值不同時才更新
        if (Math.abs(slider.getValue() - newValue) > 0.01f) {
            slider.setValue(newValue);
        }
    }

    /**
     * 獲取 Slider 的值（反向綁定：View -> ViewModel）
     *
     * @param slider Slider 控件
     * @return 當前 Slider 的值
     */
    @InverseBindingAdapter(attribute = "sliderValue", event = "sliderValueAttrChanged")
    public static Float getSliderValue(Slider slider) {
        if (slider == null) return 0f;
        return slider.getValue();
    }

    /**
     * 設置 Slider 值變化監聽器（用於反向綁定）
     *
     * @param slider   Slider 控件
     * @param attrChange  InverseBindingListener，用於通知數據綁定值已改變
     */
    @BindingAdapter("sliderValueAttrChanged")
    public static void setSliderValueListener(Slider slider, InverseBindingListener attrChange) {
        if (slider == null) return;

        if (attrChange != null) {
            slider.addOnChangeListener((s, value, fromUser) -> {
                // 只在用戶操作時觸發反向綁定，避免程式設定值時的無限循環
                if (fromUser) {
                    attrChange.onChange();
                }
            });
        }
    }

    /**
     * 設置 Slider 的啟用狀態
     *
     * @param slider  Slider 控件
     * @param enabled 是否啟用
     */
    @BindingAdapter("sliderEnabled")
    public static void setSliderEnabled(Slider slider, Boolean enabled) {
        if (slider == null) return;
        slider.setEnabled(enabled != null && enabled);
    }
}
