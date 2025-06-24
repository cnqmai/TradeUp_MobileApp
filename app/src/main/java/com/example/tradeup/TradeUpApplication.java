package com.example.tradeup; // Đảm bảo package này đúng với package gốc của bạn

import android.app.Application;
import com.google.firebase.FirebaseApp; // Import này cần nếu bạn khởi tạo Firebase ở đây

public class TradeUpApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Khởi tạo Firebase ở đây, nếu project của bạn yêu cầu.
        // Nếu bạn không khởi tạo Firebase ở đây, bạn có thể bỏ qua dòng này.
        // FirebaseApp.initializeApp(this);
        // Bạn có thể thêm các khởi tạo toàn cục khác của ứng dụng ở đây.
        // Ví dụ: khởi tạo thư viện, cấu hình ban đầu...
    }
}