package com.example.tradeup.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout; // Import LinearLayout for Quick Actions
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide; // Import Glide
import com.example.tradeup.R;
import com.example.tradeup.activity.LoginActivity;
import com.example.tradeup.model.User; // Ensure User model is imported
import com.example.tradeup.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth; // Ensure FirebaseAuth is imported
import com.google.firebase.auth.FirebaseUser; // Ensure FirebaseUser is imported
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.Locale;

public class AdminDashboardHomeFragment extends Fragment {

    private static final String TAG = "AdminDashboardHomeFrag";

    // Updated TextViews for stat cards
    private TextView tvActiveUsersCount, tvTodaysTradesCount, tvRevenueAmount, tvNewUsersCount;
    private ImageView ivAdminNotifications, ivAdminProfile; // For toolbar icons
    private Button btnChartToday, btnChartWeek, btnChartMonth; // For performance chart filters
    private ImageView ivPerformanceChartPlaceholder; // Placeholder for chart

    // Quick Actions (LinearLayouts acting as buttons)
    private LinearLayout llManageUsers, llViewReports, llSettings, llSupport;

    private NavController navController;
    private FirebaseHelper firebaseHelper;

    public AdminDashboardHomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = new FirebaseHelper();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dashboard_home, container, false);
        initViews(view);
        setupListeners();
        loadDashboardStats();
        loadAdminProfilePicture(); // Load admin's profile picture
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
    }

    private void initViews(View view) {
        // Stat cards
        tvActiveUsersCount = view.findViewById(R.id.tv_active_users_count);
        tvTodaysTradesCount = view.findViewById(R.id.tv_todays_trades_count);
        tvRevenueAmount = view.findViewById(R.id.tv_revenue_amount);
        tvNewUsersCount = view.findViewById(R.id.tv_new_users_count);

        // Toolbar
        ivAdminNotifications = view.findViewById(R.id.iv_admin_notifications);
        ivAdminProfile = view.findViewById(R.id.iv_admin_profile);

        // Performance Chart buttons
        btnChartToday = view.findViewById(R.id.btn_chart_today);
        btnChartWeek = view.findViewById(R.id.btn_chart_week);
        btnChartMonth = view.findViewById(R.id.btn_chart_month);
        ivPerformanceChartPlaceholder = view.findViewById(R.id.iv_performance_chart_placeholder);

        // Quick Actions
        llManageUsers = view.findViewById(R.id.ll_manage_users);
        llViewReports = view.findViewById(R.id.ll_view_reports);
        llSettings = view.findViewById(R.id.ll_settings);
        llSupport = view.findViewById(R.id.ll_support);
    }

    private void setupListeners() {
        // Admin Toolbar actions
        ivAdminNotifications.setOnClickListener(v -> Toast.makeText(getContext(), "Notifications Clicked", Toast.LENGTH_SHORT).show());
        ivAdminProfile.setOnClickListener(v -> performAdminLogout()); // Tạm thời dùng cho logout, có thể đổi thành xem profile admin

        // Performance Chart filters
        btnChartToday.setOnClickListener(v -> Toast.makeText(getContext(), "Today Chart Clicked", Toast.LENGTH_SHORT).show());
        btnChartWeek.setOnClickListener(v -> Toast.makeText(getContext(), "Week Chart Clicked", Toast.LENGTH_SHORT).show());
        btnChartMonth.setOnClickListener(v -> Toast.makeText(getContext(), "Month Chart Clicked", Toast.LENGTH_SHORT).show());

        // Quick Actions
        llManageUsers.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Manage Users Clicked", Toast.LENGTH_SHORT).show();
            if (navController != null) {
                // Navigate to UserManagementFragment (FR-8.2.1 part: view flagged users)
                navController.navigate(R.id.action_adminDashboardHomeFragment_to_userManagementFragment);
            }
        });
        llViewReports.setOnClickListener(v -> {
            Toast.makeText(getContext(), "View Reports Clicked", Toast.LENGTH_SHORT).show();
            if (navController != null) {
                // Navigate to ReportListFragment (FR-8.2.1 part: view reports)
                navController.navigate(R.id.action_adminDashboardHomeFragment_to_reportListFragment);
            }
        });
        llSettings.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Settings Clicked", Toast.LENGTH_SHORT).show();
            // if (navController != null) navController.navigate(R.id.action_adminDashboardHomeFragment_to_adminSettingsFragment);
        });
        llSupport.setOnClickListener(v -> Toast.makeText(getContext(), "Support Clicked", Toast.LENGTH_SHORT).show());
    }

    private void loadDashboardStats() {
        // Load Active Users (assuming all active accounts from 'users' node)
        FirebaseDatabase.getInstance().getReference("users").orderByChild("account_status").equalTo("active")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isAdded()) {
                            tvActiveUsersCount.setText(String.valueOf(snapshot.getChildrenCount()));
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load active users: " + error.getMessage());
                    }
                });

        // Load Today's Trades (you'll need to query 'transactions' node for today's transactions)
        // This requires transaction_date to be indexed and maybe a more complex query.
        // For now, let's use a placeholder or total completed transactions.
        // Assuming "completed" status in transactions indicates a trade.
        FirebaseDatabase.getInstance().getReference("transactions").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    long completedTradesToday = 0;
                    // You would iterate through snapshot.getChildren() and check transaction_date
                    // For simplicity, just count total transactions for now.
                    tvTodaysTradesCount.setText(String.valueOf(snapshot.getChildrenCount())); // Placeholder: total transactions
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load total trades: " + error.getMessage());
            }
        });


        // Load Revenue (you'll need to sum final_price from completed transactions)
        // This is a complex aggregation. For now, use a placeholder.
        // In a real app, you'd have server-side functions or a dedicated analytics node.
        if (isAdded()) {
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "US")); // Use US locale for dollar sign
            currencyFormatter.setMaximumFractionDigits(0);
            tvRevenueAmount.setText(currencyFormatter.format(12456)); // Placeholder value
        }

        // Load New Users (users created today)
        FirebaseDatabase.getInstance().getReference("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    long newUsersToday = 0;
                    // You would iterate through snapshot.getChildren() and check created_at for today's date
                    // For simplicity, use a placeholder.
                    tvNewUsersCount.setText(String.valueOf(34)); // Placeholder value
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load new users: " + error.getMessage());
            }
        });

        // For chart placeholder
        if (isAdded()) {
            ivPerformanceChartPlaceholder.setImageResource(R.drawable.chart_placeholder);
        }
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
                                .placeholder(R.drawable.img_profile_placeholder) // Use img_profile_placeholder as default
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
