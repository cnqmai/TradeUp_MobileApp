package com.example.tradeup.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.activity.LoginActivity;
import com.example.tradeup.adapter.RecentActivityAdapter;
import com.example.tradeup.model.ActivityLog;
import com.example.tradeup.model.Item;
import com.example.tradeup.model.Offer;
import com.example.tradeup.model.Payment;
import com.example.tradeup.model.Report;
import com.example.tradeup.model.Transaction;
import com.example.tradeup.model.User;
import com.example.tradeup.utils.AppDateUtils;
import com.example.tradeup.utils.FirebaseHelper;
import com.example.tradeup.view.BarChartView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AdminDashboardHomeFragment extends Fragment {

    private static final String TAG = "AdminDashboardHomeFrag";

    private TextView tvActiveUsersCount, tvTodaysTradesCount, tvRevenueAmount, tvNewUsersCount;
    private ImageView ivAdminNotifications, ivAdminProfile;
    private Button btnChartToday, btnChartWeek, btnChartMonth;
    private LinearLayout chartContainer;
    private BarChartView barChartView;

    private RecyclerView recyclerRecentActivity;

    private LinearLayout llManageUsers, llViewReports, llReviewModeration, llSupport;

    private NavController navController;
    private FirebaseHelper firebaseHelper;


    private Map<String, Long> weeklyRevenueData;



    public AdminDashboardHomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = new FirebaseHelper(requireContext());
        weeklyRevenueData = new HashMap<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dashboard_home, container, false);
        initViews(view);
        setupListeners();
        setupPerformanceChart();
        loadDashboardStats();
        loadAdminProfilePicture();
        loadPerformanceChartData("week");
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
    }

    private void initViews(View view) {
        tvActiveUsersCount = view.findViewById(R.id.tv_active_users_count);
        tvTodaysTradesCount = view.findViewById(R.id.tv_todays_trades_count);
        tvRevenueAmount = view.findViewById(R.id.tv_revenue_amount);
        tvNewUsersCount = view.findViewById(R.id.tv_new_users_count);

        ivAdminNotifications = view.findViewById(R.id.iv_admin_notifications);
        ivAdminProfile = view.findViewById(R.id.iv_admin_profile);

        btnChartToday = view.findViewById(R.id.btn_chart_today);
        btnChartWeek = view.findViewById(R.id.btn_chart_week);
        btnChartMonth = view.findViewById(R.id.btn_chart_month);
        chartContainer = view.findViewById(R.id.chart_container);

        recyclerRecentActivity = view.findViewById(R.id.recycler_recent_activity);

        llManageUsers = view.findViewById(R.id.ll_manage_users);
        llViewReports = view.findViewById(R.id.ll_view_reports);
        llReviewModeration = view.findViewById(R.id.ll_review_moderation);
        llSupport = view.findViewById(R.id.ll_support);
    }

    private void setupListeners() {
        ivAdminNotifications.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Notifications Clicked", Toast.LENGTH_SHORT).show();
            if (navController != null) {
                navController.navigate(R.id.action_adminDashboardHomeFragment_to_notificationFragment);
            }
        });
        ivAdminProfile.setOnClickListener(v -> performAdminLogout());

        btnChartToday.setOnClickListener(v -> {
            loadPerformanceChartData("today");
            updateChartButtonState(btnChartToday);
        });
        btnChartWeek.setOnClickListener(v -> {
            loadPerformanceChartData("week");
            updateChartButtonState(btnChartWeek);
        });
        btnChartMonth.setOnClickListener(v -> {
            loadPerformanceChartData("month");
            updateChartButtonState(btnChartMonth);
        });
        updateChartButtonState(btnChartWeek);

        llManageUsers.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Manage Users Clicked", Toast.LENGTH_SHORT).show();
            if (navController != null) {
                navController.navigate(R.id.action_adminDashboardHomeFragment_to_userManagementFragment);
            }
        });
        llViewReports.setOnClickListener(v -> {
            Toast.makeText(getContext(), "View Reports Clicked", Toast.LENGTH_SHORT).show();
            if (navController != null) {
                navController.navigate(R.id.action_adminDashboardHomeFragment_to_reportListFragment);
            }
        });
        llReviewModeration.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Review Moderation Clicked", Toast.LENGTH_SHORT).show();
            if (navController != null) {
                navController.navigate(R.id.action_adminDashboardHomeFragment_to_reviewModerationFragment);
            }
        });
        llSupport.setOnClickListener(v -> Toast.makeText(getContext(), "Support Clicked", Toast.LENGTH_SHORT).show());
    }

    private void updateChartButtonState(Button activeButton) {
        btnChartToday.setBackgroundTintList(getResources().getColorStateList(R.color.orange_bold)); // Hoặc màu nền khác cho nút không được chọn
        btnChartToday.setTextColor(getResources().getColor(R.color.white)); // Hoặc màu chữ khác cho nút không được chọn
        btnChartWeek.setBackgroundTintList(getResources().getColorStateList(R.color.orange_bold));
        btnChartWeek.setTextColor(getResources().getColor(R.color.white));
        btnChartMonth.setBackgroundTintList(getResources().getColorStateList(R.color.orange_bold));
        btnChartMonth.setTextColor(getResources().getColor(R.color.white));

        activeButton.setBackgroundTintList(getResources().getColorStateList(R.color.green_bold));
        activeButton.setTextColor(getResources().getColor(R.color.white));
    }

    private void setupPerformanceChart() {
        barChartView = new BarChartView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        chartContainer.addView(barChartView, params);
    }

    private void loadDashboardStats() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User not logged in, cannot load dashboard stats.");
            return;
        }

        // Load Active Users and New Users Today
        FirebaseDatabase.getInstance().getReference("users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isAdded()) {
                            long activeUsers = 0;
                            long newUsersToday = 0;
                            String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                User user = userSnapshot.getValue(User.class);
                                if (user != null) {
                                    if (user.getIs_banned() == null || !user.getIs_banned()) {
                                        activeUsers++;
                                    }
                                    if (user.getCreated_at() != null) {
                                        try {
                                            Date createdAt = AppDateUtils.parseIsoDate(user.getCreated_at());
                                            if (createdAt != null) {
                                                String userCreationDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(createdAt);
                                                if (userCreationDate.equals(todayDate)) {
                                                    newUsersToday++;
                                                }
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error parsing user creation date: " + e.getMessage());
                                        }
                                    }
                                }
                            }
                            tvActiveUsersCount.setText(String.valueOf(activeUsers));
                            tvNewUsersCount.setText(String.valueOf(newUsersToday));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load user stats: " + error.getMessage());
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Lỗi tải thống kê người dùng.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Load Today's Trades and Total Revenue from Payments
        FirebaseDatabase.getInstance().getReference("payments")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isAdded()) {
                            long todaysTrades = 0;
                            long totalRevenue = 0L;
                            String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                            for (DataSnapshot paymentSnapshot : snapshot.getChildren()) {
                                Payment payment = paymentSnapshot.getValue(Payment.class);
                                if (payment != null && Objects.equals(payment.getStatus(), "completed") && payment.getAmount() != null && payment.getTimestamp() != null) {
                                    totalRevenue += payment.getAmount();
                                    try {
                                        Date paymentDate = AppDateUtils.parseIsoDate(payment.getTimestamp());
                                        if (paymentDate != null) {
                                            String paymentDay = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(paymentDate);
                                            if (paymentDay.equals(todayDate)) {
                                                todaysTrades++;
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error parsing payment timestamp for trades: " + e.getMessage());
                                    }
                                }
                            }
                            tvTodaysTradesCount.setText(String.valueOf(todaysTrades));
                            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                            currencyFormatter.setMaximumFractionDigits(0);
                            tvRevenueAmount.setText(currencyFormatter.format(totalRevenue));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load payment stats: " + error.getMessage());
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Lỗi tải thống kê doanh thu.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    private void loadPerformanceChartData(String timeRange) {
        weeklyRevenueData.clear();

        String[] daysOfWeek = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (String day : daysOfWeek) {
            weeklyRevenueData.put(day, 0L);
        }

        FirebaseDatabase.getInstance().getReference("payments")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isAdded()) {
                            Calendar calendar = Calendar.getInstance();
                            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.US);

                            long totalRevenueForPeriod = 0L; // Track total revenue for the selected period

                            for (DataSnapshot paymentSnapshot : snapshot.getChildren()) {
                                Payment payment = paymentSnapshot.getValue(Payment.class);
                                if (payment != null && Objects.equals(payment.getStatus(), "completed") && payment.getAmount() != null && payment.getTimestamp() != null) {
                                    try {
                                        Date paymentDate = AppDateUtils.parseIsoDate(payment.getTimestamp());
                                        if (paymentDate != null) {
                                            calendar.setTime(paymentDate);

                                            boolean includePayment = false;
                                            Calendar now = Calendar.getInstance();
                                            Calendar paymentCal = Calendar.getInstance();
                                            paymentCal.setTime(paymentDate);

                                            if (timeRange.equals("today")) {
                                                if (now.get(Calendar.YEAR) == paymentCal.get(Calendar.YEAR) &&
                                                        now.get(Calendar.DAY_OF_YEAR) == paymentCal.get(Calendar.DAY_OF_YEAR)) {
                                                    includePayment = true;
                                                }
                                            } else if (timeRange.equals("week")) {
                                                Calendar startOfWeek = (Calendar) now.clone();
                                                startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                                                startOfWeek.set(Calendar.HOUR_OF_DAY, 0);
                                                startOfWeek.set(Calendar.MINUTE, 0);
                                                startOfWeek.set(Calendar.SECOND, 0);
                                                startOfWeek.set(Calendar.MILLISECOND, 0);

                                                if (paymentCal.getTimeInMillis() >= startOfWeek.getTimeInMillis()) {
                                                    includePayment = true;
                                                }
                                            } else if (timeRange.equals("month")) {
                                                Calendar startOfMonth = (Calendar) now.clone();
                                                startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
                                                startOfMonth.set(Calendar.HOUR_OF_DAY, 0);
                                                startOfMonth.set(Calendar.MINUTE, 0);
                                                startOfMonth.set(Calendar.SECOND, 0);
                                                startOfMonth.set(Calendar.MILLISECOND, 0);

                                                if (paymentCal.getTimeInMillis() >= startOfMonth.getTimeInMillis()) {
                                                    includePayment = true;
                                                }
                                            }

                                            if (includePayment) {
                                                String normalizedDay;
                                                switch (calendar.get(Calendar.DAY_OF_WEEK)) {
                                                    case Calendar.MONDAY: normalizedDay = "Mon"; break;
                                                    case Calendar.TUESDAY: normalizedDay = "Tue"; break;
                                                    case Calendar.WEDNESDAY: normalizedDay = "Wed"; break;
                                                    case Calendar.THURSDAY: normalizedDay = "Thu"; break;
                                                    case Calendar.FRIDAY: normalizedDay = "Fri"; break;
                                                    case Calendar.SATURDAY: normalizedDay = "Sat"; break;
                                                    case Calendar.SUNDAY: normalizedDay = "Sun"; break;
                                                    default: normalizedDay = "Unknown"; break;
                                                }
                                                weeklyRevenueData.put(normalizedDay, weeklyRevenueData.getOrDefault(normalizedDay, 0L) + payment.getAmount());
                                                totalRevenueForPeriod += payment.getAmount(); // Accumulate total revenue
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error parsing payment date for chart: " + e.getMessage());
                                    }
                                }
                            }
                            barChartView.setChartData(weeklyRevenueData);
                            // Update total revenue display for the selected period
                            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                            currencyFormatter.setMaximumFractionDigits(0);
                            tvRevenueAmount.setText(currencyFormatter.format(totalRevenueForPeriod));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load revenue data for chart: " + error.getMessage());
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Lỗi tải dữ liệu biểu đồ doanh thu.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void loadAdminProfilePicture() {
        FirebaseUser currentUser = firebaseHelper.getCurrentUser();
        if (currentUser != null) {
            firebaseHelper.getUserProfile(currentUser.getUid(), new FirebaseHelper.DbReadCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    if (isAdded() && user != null && user.getProfile_picture_url() != null && !user.getProfile_picture_url().isEmpty()) {
                        Glide.with(requireContext())
                                .load(user.getProfile_picture_url())
                                .placeholder(R.drawable.img_profile_placeholder)
                                .error(R.drawable.img_profile_placeholder)
                                .into(ivAdminProfile);
                    } else if (isAdded()) {
                        ivAdminProfile.setImageResource(R.drawable.img_profile_placeholder);
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Failed to load admin profile picture: " + errorMessage);
                    if (isAdded()) {
                        ivAdminProfile.setImageResource(R.drawable.img_profile_placeholder);
                    }
                }
            });
        } else {
            if (isAdded()) {
                ivAdminProfile.setImageResource(R.drawable.img_profile_placeholder);
            }
        }
    }

    private void performAdminLogout() {
        if (isAdded()) {
            firebaseHelper.signOut();
            Toast.makeText(getContext(), "Đã đăng xuất khỏi tài khoản Admin.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        }
    }
}
