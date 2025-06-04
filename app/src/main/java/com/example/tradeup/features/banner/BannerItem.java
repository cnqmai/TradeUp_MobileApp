package com.example.tradeup.features.banner;

public class BannerItem {
    private String imageUrl;
    private String buttonText;
    private String actionUrl; // nếu có

    // ✅ Bắt buộc: Constructor rỗng cho Firebase
    public BannerItem() {
    }

    // Constructor đầy đủ (có thể dùng khi bạn muốn tạo đối tượng)
    public BannerItem(String imageUrl, String buttonText, String actionUrl) {
        this.imageUrl = imageUrl;
        this.buttonText = buttonText;
        this.actionUrl = actionUrl;
    }

    // ✅ Getters và Setters
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getButtonText() {
        return buttonText;
    }

    public void setButtonText(String buttonText) {
        this.buttonText = buttonText;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }
}
