package com.example.tradeup.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
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
import com.example.tradeup.model.User;
import com.example.tradeup.model.Report; // Import Report model to check for flagged users
import com.example.tradeup.utils.FirebaseHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserManagementFragment extends Fragment {

    private static final String TAG = "UserManagementFragment";

    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private TextView textNoUsers;
    private ImageView ivBackButton;
    private EditText etSearchUsers;
    private Spinner spinnerUserFilter;

    private NavController navController;
    private FirebaseHelper firebaseHelper;
    private List<User> allUsersList; // Stores all fetched users
    private List<User> filteredUsersList; // Stores users based on current filter/search
    private Set<String> flaggedUserIds; // To keep track of users who have been reported

    public UserManagementFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = new FirebaseHelper(requireContext());
        allUsersList = new ArrayList<>();
        filteredUsersList = new ArrayList<>();
        flaggedUserIds = new HashSet<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_management, container, false);
        initViews(view);
        setupListeners();
        setupRecyclerView();
        setupSpinner();
        loadFlaggedUserIds(); // Load flagged user IDs first
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_users);
        textNoUsers = view.findViewById(R.id.text_no_users);
        ivBackButton = view.findViewById(R.id.iv_back_button_user_management);
        etSearchUsers = view.findViewById(R.id.et_search_users);
        spinnerUserFilter = view.findViewById(R.id.spinner_user_filter);
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigateUp();
            }
        });

        etSearchUsers.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters(); // Apply search and spinner filter
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new UserAdapter(filteredUsersList, new UserAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {
                // Navigate to UserProfileFragment or a dedicated AdminUserDetailFragment
                Toast.makeText(getContext(), "Clicked on user: " + user.getDisplay_name(), Toast.LENGTH_SHORT).show();
                if (navController != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("userId", user.getUid());
                    // Assuming action_userManagementFragment_to_userProfileFragment exists in admin_nav_graph
                    navController.navigate(R.id.action_userManagementFragment_to_userProfileFragment, bundle);
                }
            }
        }, flaggedUserIds); // Pass flaggedUserIds to adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.user_filter_options, // Defined in strings.xml
                android.R.layout.simple_spinner_item
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUserFilter.setAdapter(spinnerAdapter);

        spinnerUserFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters(); // Apply filter when spinner selection changes
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void loadFlaggedUserIds() {
        // Query reports to find users who have been reported
        DatabaseReference reportsRef = FirebaseDatabase.getInstance().getReference("reports");
        reportsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    flaggedUserIds.clear();
                    for (DataSnapshot reportSnapshot : snapshot.getChildren()) {
                        Report report = reportSnapshot.getValue(Report.class);
                        if (report != null && "user".equalsIgnoreCase(report.getReport_type()) && report.getReported_object_id() != null) {
                            flaggedUserIds.add(report.getReported_object_id());
                        }
                    }
                    Log.d(TAG, "Loaded " + flaggedUserIds.size() + " flagged user IDs.");
                    loadAllUsers(); // After loading flagged IDs, load all users
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load flagged user IDs: " + error.getMessage());
                if (isAdded()) {
                    Toast.makeText(getContext(), "Lỗi khi tải danh sách người dùng bị gắn cờ.", Toast.LENGTH_SHORT).show();
                    loadAllUsers(); // Still try to load users even if flagged IDs fail
                }
            }
        });
    }

    private void loadAllUsers() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    allUsersList.clear();
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        User user = userSnapshot.getValue(User.class);
                        if (user != null) {
                            user.setUid(userSnapshot.getKey()); // Set UID from key
                            allUsersList.add(user);
                        }
                    }
                    Log.d(TAG, "Loaded " + allUsersList.size() + " total users.");
                    applyFilters(); // Apply filters after all users are loaded
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load all users: " + error.getMessage());
                if (isAdded()) {
                    Toast.makeText(getContext(), "Lỗi khi tải danh sách người dùng.", Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                }
            }
        });
    }

    private void applyFilters() {
        filteredUsersList.clear();
        String searchText = etSearchUsers.getText().toString().toLowerCase(Locale.getDefault());
        int filterPosition = spinnerUserFilter.getSelectedItemPosition();

        for (User user : allUsersList) {
            boolean matchesSearch = true;
            if (!searchText.isEmpty()) {
                if (user.getDisplay_name() == null || !user.getDisplay_name().toLowerCase(Locale.getDefault()).contains(searchText)) {
                    matchesSearch = false;
                }
            }

            boolean matchesFilter = false;
            switch (filterPosition) {
                case 0: // Tất cả người dùng
                    matchesFilter = true;
                    break;
                case 1: // Người dùng bị gắn cờ
                    if (user.getUid() != null && flaggedUserIds.contains(user.getUid())) {
                        matchesFilter = true;
                    }
                    break;
                case 2: // Người dùng bị khóa
                    if (user.getIs_banned() != null && user.getIs_banned()) {
                        matchesFilter = true;
                    }
                    break;
            }

            if (matchesSearch && matchesFilter) {
                filteredUsersList.add(user);
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (isAdded()) {
            if (filteredUsersList.isEmpty()) {
                textNoUsers.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                textNoUsers.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    // Adapter for the RecyclerView
    private static class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

        private List<User> users;
        private OnUserClickListener listener;
        private Set<String> flaggedUserIds; // Set of user UIDs who are flagged

        public interface OnUserClickListener {
            void onUserClick(User user);
        }

        public UserAdapter(List<User> users, OnUserClickListener listener, Set<String> flaggedUserIds) {
            this.users = users;
            this.listener = listener;
            this.flaggedUserIds = flaggedUserIds;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_management, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = users.get(position);
            holder.bind(user, listener, flaggedUserIds);
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        static class UserViewHolder extends RecyclerView.ViewHolder {
            CircleImageView ivUserProfilePic;
            TextView tvUserName, tvUserEmail, tvUserStatus;
            ImageView ivFlagStatus;

            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                ivUserProfilePic = itemView.findViewById(R.id.iv_user_profile_pic);
                tvUserName = itemView.findViewById(R.id.tv_user_name);
                tvUserEmail = itemView.findViewById(R.id.tv_user_email);
                tvUserStatus = itemView.findViewById(R.id.tv_user_status);
                ivFlagStatus = itemView.findViewById(R.id.iv_flag_status);
            }

            public void bind(final User user, final OnUserClickListener listener, Set<String> flaggedUserIds) {
                tvUserName.setText(user.getDisplay_name());
                tvUserEmail.setText(user.getEmail());

                String statusText = "Trạng thái: " + user.getAccount_status();
                if (user.getIs_banned() != null && user.getIs_banned()) {
                    statusText = "Trạng thái: Bị khóa";
                    tvUserStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
                } else {
                    tvUserStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.black));
                }
                tvUserStatus.setText(statusText);

                // Load profile picture
                if (user.getProfile_picture_url() != null && !user.getProfile_picture_url().isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(user.getProfile_picture_url())
                            .placeholder(R.drawable.img_profile_placeholder)
                            .error(R.drawable.img_profile_placeholder)
                            .into(ivUserProfilePic);
                } else {
                    ivUserProfilePic.setImageResource(R.drawable.img_profile_placeholder);
                }

                // Show flag icon if user is in the flaggedUserIds set
                if (user.getUid() != null && flaggedUserIds.contains(user.getUid())) {
                    ivFlagStatus.setVisibility(View.VISIBLE);
                } else {
                    ivFlagStatus.setVisibility(View.GONE);
                }

                itemView.setOnClickListener(v -> listener.onUserClick(user));
            }
        }
    }
}
