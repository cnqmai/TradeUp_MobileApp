package com.example.tradeup.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.R;
import com.example.tradeup.adapter.CategoryAdapter;
import com.example.tradeup.adapter.ItemAdapter;
import com.example.tradeup.model.Item;
import com.example.tradeup.model.Location;
import com.example.tradeup.utils.FirebaseHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeFragment extends Fragment implements CategoryAdapter.OnCategoryClickListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String TAG = "HomeFragment";

    private RecyclerView rvPopularItems, rvItemsNearYou, rvCategories, rvPersonalizedItems;
    private ItemAdapter popularItemsAdapter, itemsNearYouAdapter, personalizedItemsAdapter;
    private CategoryAdapter categoryAdapter;
    private FirebaseHelper firebaseHelper;
    private NavController navController;
    private ImageView ivNotificationBellHome;
    private TextView tvPersonalizedItemsTitle;

    private FusedLocationProviderClient fusedLocationClient;
    private CancellationTokenSource cancellationTokenSource;
    private FirebaseAuth mAuth;

    private ExecutorService backgroundExecutor;
    private boolean isExecutorActive = true;
    private android.location.Location currentUserLocation; // Lưu trữ vị trí người dùng

    private String currentCategoryFilter = null;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        backgroundExecutor = Executors.newSingleThreadExecutor();
        isExecutorActive = true;
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        firebaseHelper = new FirebaseHelper();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        mAuth = FirebaseAuth.getInstance();

        // Initialize RecyclerViews
        rvCategories = view.findViewById(R.id.rvCategories);
        rvItemsNearYou = view.findViewById(R.id.rvItemsNearYou);
        rvPopularItems = view.findViewById(R.id.rvPopularItems);
        rvPersonalizedItems = view.findViewById(R.id.rvPersonalizedItems);

        // Initialize TextView for personalized items title
        tvPersonalizedItemsTitle = view.findViewById(R.id.tvPersonalizedItemsTitle);

        // Setup Adapters
        categoryAdapter = new CategoryAdapter(getContext(), new ArrayList<>());
        itemsNearYouAdapter = new ItemAdapter(getContext(), new ArrayList<>(), R.layout.item_list_horizontal);
        popularItemsAdapter = new ItemAdapter(getContext(), new ArrayList<>(), R.layout.item_list_horizontal);
        personalizedItemsAdapter = new ItemAdapter(getContext(), new ArrayList<>(), R.layout.item_list_horizontal);

        // Set Layout Managers and Adapters
        rvCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);
        categoryAdapter.setOnCategoryClickListener(this);

        rvItemsNearYou.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvItemsNearYou.setAdapter(itemsNearYouAdapter);

        rvPopularItems.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvPopularItems.setAdapter(popularItemsAdapter);

        rvPersonalizedItems.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvPersonalizedItems.setAdapter(personalizedItemsAdapter);

        // Initialize ImageView
        ivNotificationBellHome = view.findViewById(R.id.iv_notification_bell_home);

        // Set OnClickListener for ImageView
        ivNotificationBellHome.setOnClickListener(v -> {
            navController.navigate(R.id.action_homeFragment_to_notificationFragment);
        });

        // Set click listeners for items
        itemsNearYouAdapter.setOnItemClickListener(itemId -> {
            Bundle bundle = new Bundle();
            bundle.putString("itemId", itemId);
            navController.navigate(R.id.action_homeFragment_to_itemDetailFragment, bundle);
        });

        popularItemsAdapter.setOnItemClickListener(itemId -> {
            Bundle bundle = new Bundle();
            bundle.putString("itemId", itemId);
            navController.navigate(R.id.action_homeFragment_to_itemDetailFragment, bundle);
        });

        personalizedItemsAdapter.setOnItemClickListener(itemId -> {
            Bundle bundle = new Bundle();
            bundle.putString("itemId", itemId);
            navController.navigate(R.id.action_homeFragment_to_itemDetailFragment, bundle);
        });

        // Fetch initial data
        fetchCategories();
        checkLocationPermissionAndFetchNearYou(); // This will also trigger fetchTrendingItems and fetchPersonalizedItems
    }

    // THÊM PHƯƠNG THỨC XỬ LÝ CLICK TỪ CategoryAdapter
    @Override
    public void onCategoryClick(String categoryName) {
        Log.d(TAG, "Category clicked: " + categoryName);
        // Chuyển sang một Fragment mới để hiển thị danh sách mặt hàng theo category
        // Ví dụ: Tạo một CategoryItemListFragment
        Bundle bundle = new Bundle();
        bundle.putString("categoryName", categoryName); // Truyền tên danh mục
        // Đảm bảo navigation graph của bạn có action từ HomeFragment đến CategoryItemListFragment
        navController.navigate(R.id.action_homeFragment_to_categoryItemListFragment, bundle);
    }

    // Cập nhật phương thức này để nhận userLocation
    private void fetchItemsBySpecificCategory(String category, @Nullable android.location.Location userLocation) {
        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("items");
        itemsRef.orderByChild("category").equalTo(category).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                backgroundExecutor.execute(() -> {
                    List<Item> filteredItems = new ArrayList<>();
                    for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                        Item item = itemSnapshot.getValue(Item.class);
                        if (item != null && item.getStatus() != null && item.getStatus().equals("Available")) {
                            item.setId(itemSnapshot.getKey());
                            // Tính toán khoảng cách nếu userLocation có sẵn
                            if (userLocation != null && item.getLocation() != null && item.getLocation().getLat() != null && item.getLocation().getLng() != null) {
                                android.location.Location itemLocation = new android.location.Location("");
                                itemLocation.setLatitude(item.getLocation().getLat());
                                itemLocation.setLongitude(item.getLocation().getLng());
                                float distanceInKm = userLocation.distanceTo(itemLocation) / 1000;
                                item.setDistanceToUser(distanceInKm);
                            } else {
                                item.setDistanceToUser(-1); // Không hiển thị khoảng cách
                            }
                            filteredItems.add(item);
                        }
                    }
                    requireActivity().runOnUiThread(() -> {
                        personalizedItemsAdapter.setItems(filteredItems);
                        if (filteredItems.isEmpty() && isAdded()) {
                            Toast.makeText(getContext(), getString(R.string.no_items_in_category, category), Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load items for specific category " + category + ": " + error.getMessage());
                requireActivity().runOnUiThread(() -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), getString(R.string.error_loading_category_items, error.getMessage()), Toast.LENGTH_SHORT).show();
                    }
                    personalizedItemsAdapter.setItems(new ArrayList<>());
                });
            }
        });
    }

    // Cập nhật phương thức này để nhận userLocation
    private void fetchTrendingItems(@Nullable android.location.Location userLocation) {
        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("items");
        itemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                backgroundExecutor.execute(() -> {
                    List<Item> trendingItems = new ArrayList<>();
                    for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                        Item item = itemSnapshot.getValue(Item.class);
                        if (item != null && item.getStatus() != null && item.getStatus().equals("Available")) {
                            item.setId(itemSnapshot.getKey());
                            // Tính toán khoảng cách nếu userLocation có sẵn
                            if (userLocation != null && item.getLocation() != null && item.getLocation().getLat() != null && item.getLocation().getLng() != null) {
                                android.location.Location itemLocation = new android.location.Location("");
                                itemLocation.setLatitude(item.getLocation().getLat());
                                itemLocation.setLongitude(item.getLocation().getLng());
                                float distanceInKm = userLocation.distanceTo(itemLocation) / 1000;
                                item.setDistanceToUser(distanceInKm);
                            } else {
                                item.setDistanceToUser(-1); // Không hiển thị khoảng cách
                            }
                            trendingItems.add(item);
                        }
                    }
                    trendingItems.removeIf(item -> item.getRating_count() == null || item.getRating_count() <= 0);

                    Collections.sort(trendingItems, (item1, item2) -> {
                        int ratingComparison = Double.compare(item2.getAverage_rating(), item1.getAverage_rating());
                        if (ratingComparison != 0) {
                            return ratingComparison;
                        }
                        return Long.compare(item2.getRating_count(), item1.getRating_count());
                    });

                    List<Item> topTrendingItems = trendingItems.subList(0, Math.min(trendingItems.size(), 10));

                    requireActivity().runOnUiThread(() -> {
                        popularItemsAdapter.setItems(topTrendingItems);
                    });
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load trending items by rating: " + error.getMessage());
                if (isAdded()) {
                    Toast.makeText(getContext(), getString(R.string.failed_to_load_trending_items), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    // Cập nhật phương thức này để nhận userLocation
    private void fetchItemsByIds(List<String> itemIds, ItemAdapter adapter, @Nullable android.location.Location userLocation) {
        if (itemIds.isEmpty()) {
            adapter.setItems(new ArrayList<>());
            return;
        }

        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("items");
        Map<String, Item> fetchedItemMap = new HashMap<>();
        final AtomicInteger fetchedCount = new AtomicInteger(0);

        for (String itemId : itemIds) {
            itemsRef.child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Item item = snapshot.getValue(Item.class);
                    if (item != null) {
                        item.setId(snapshot.getKey());
                        // Tính toán khoảng cách nếu userLocation có sẵn
                        if (userLocation != null && item.getLocation() != null && item.getLocation().getLat() != null && item.getLocation().getLng() != null) {
                            android.location.Location itemLocation = new android.location.Location("");
                            itemLocation.setLatitude(item.getLocation().getLat());
                            itemLocation.setLongitude(item.getLocation().getLng());
                            float distanceInKm = userLocation.distanceTo(itemLocation) / 1000;
                            item.setDistanceToUser(distanceInKm);
                        } else {
                            item.setDistanceToUser(-1); // Không hiển thị khoảng cách
                        }
                        fetchedItemMap.put(snapshot.getKey(), item);
                    }
                    if (fetchedCount.incrementAndGet() == itemIds.size()) {
                        backgroundExecutor.execute(() -> {
                            List<Item> orderedItems = new ArrayList<>();
                            for (String id : itemIds) {
                                if (fetchedItemMap.containsKey(id)) {
                                    orderedItems.add(fetchedItemMap.get(id));
                                }
                            }
                            requireActivity().runOnUiThread(() -> {
                                adapter.setItems(orderedItems);
                            });
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load item by ID: " + error.getMessage());
                    if (fetchedCount.incrementAndGet() == itemIds.size()) {
                        backgroundExecutor.execute(() -> {
                            List<Item> orderedItems = new ArrayList<>();
                            for (String id : itemIds) {
                                if (fetchedItemMap.containsKey(id)) {
                                    orderedItems.add(fetchedItemMap.get(id));
                                }
                            }
                            requireActivity().runOnUiThread(() -> {
                                adapter.setItems(orderedItems);
                            });
                        });
                    }
                }
            });
        }
    }


    private void fetchCategories() {
        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("items");
        itemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                executeIfPossible(() -> {
                    Set<String> categories = new HashSet<>();
                    categories.add("Tất cả");

                    for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                        Item item = itemSnapshot.getValue(Item.class);
                        if (item != null && item.getCategory() != null && !item.getCategory().isEmpty()) {
                            categories.add(item.getCategory());
                        }
                    }
                    List<String> categoryList = new ArrayList<>(categories);
                    Collections.sort(categoryList);
                    if (categoryList.contains("Tất cả")) {
                        categoryList.remove("Tất cả");
                        categoryList.add(0, "Tất cả");
                    }

                    requireActivity().runOnUiThread(() -> {
                        categoryAdapter.setCategories(categoryList);
                        categoryAdapter.setSelectedCategory("Tất cả");
                        // Xóa dòng này để tránh tự động chuyển sang CategoryItemListFragment
                        // onCategoryClick("Tất cả");
                    });
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load categories: " + error.getMessage());
                if (isAdded()) {
                    Toast.makeText(getContext(), getString(R.string.failed_to_load_categories), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void checkLocationPermissionAndFetchNearYou() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchUserLocationAndItemsNearYou();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void fetchUserLocationAndItemsNearYou() {
        cancellationTokenSource = new CancellationTokenSource();

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (isAdded()) {
                Toast.makeText(getContext(), getString(R.string.no_location_permission), Toast.LENGTH_SHORT).show();
            }
            // Fallback to fetching items without location if permission is denied
            fetchTrendingItems(null); // Pass null userLocation
            fetchPersonalizedItems(null); // Pass null userLocation
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    currentUserLocation = location; // Lưu trữ vị trí người dùng
                    if (location != null) {
                        Log.d(TAG, "User location: Lat=" + location.getLatitude() + ", Lng=" + location.getLongitude());
                        fetchItemsNearYou(location);
                        fetchTrendingItems(location); // Truyền vị trí người dùng
                        fetchPersonalizedItems(location); // Truyền vị trí người dùng
                    } else {
                        Log.w(TAG, "Could not get last location. Trying to request new location updates.");
                        requestNewLocationUpdates();
                        // Fallback in case new location updates also fail
                        fetchTrendingItems(null);
                        fetchPersonalizedItems(null);
                    }
                })
                .addOnFailureListener(requireActivity(), e -> {
                    Log.e(TAG, "Error getting location: " + e.getMessage());
                    if (isAdded()) {
                        Toast.makeText(getContext(), getString(R.string.cannot_get_current_location_with_reason, e.getMessage()), Toast.LENGTH_LONG).show();
                    }
                    // Fallback to fetching items without location on failure
                    fetchTrendingItems(null);
                    fetchPersonalizedItems(null);
                });
    }

    private void requestNewLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (android.location.Location location : locationResult.getLocations()) {
                    if (location != null) {
                        currentUserLocation = location; // Lưu trữ vị trí người dùng
                        Log.d(TAG, "New location: Lat=" + location.getLatitude() + ", Lng=" + location.getLongitude());
                        fetchItemsNearYou(location);
                        fetchTrendingItems(location); // Truyền vị trí người dùng
                        fetchPersonalizedItems(location); // Truyền vị trí người dùng
                        fusedLocationClient.removeLocationUpdates(this);
                        return;
                    }
                }
            }
        };

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void fetchItemsNearYou(android.location.Location userLocation) {
        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("items");
        itemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                backgroundExecutor.execute(() -> {
                    List<Item> nearbyItems = new ArrayList<>();
                    float searchRadiusKm = 10.0f;

                    for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                        Item item = itemSnapshot.getValue(Item.class);
                        if (item != null && item.getLocation() != null && item.getStatus() != null && item.getStatus().equals("Available")) {
                            DataSnapshot locationSnapshot = itemSnapshot.child("location");
                            if (locationSnapshot.child("lat").exists() && locationSnapshot.child("lng").exists()) {
                                android.location.Location itemLocation = new android.location.Location("");
                                itemLocation.setLatitude(item.getLocation().getLat());
                                itemLocation.setLongitude(item.getLocation().getLng());

                                float distanceInMeters = userLocation.distanceTo(itemLocation);
                                float distanceInKm = distanceInMeters / 1000;

                                if (distanceInKm <= searchRadiusKm) {
                                    item.setId(itemSnapshot.getKey());
                                    item.setDistanceToUser(distanceInKm); // Set distance for display
                                    nearbyItems.add(item);
                                }
                            } else {
                                Log.w(TAG, "Item " + itemSnapshot.getKey() + " is missing lat/lng fields in its location data.");
                            }
                        }
                    }
                    Collections.sort(nearbyItems, (item1, item2) -> {
                        double dist1 = item1.getDistanceToUser();
                        double dist2 = item2.getDistanceToUser();
                        return Double.compare(dist1, dist2);
                    });

                    requireActivity().runOnUiThread(() -> {
                        itemsNearYouAdapter.setItems(nearbyItems);
                    });
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load items near you: " + error.getMessage());
                if (isAdded()) {
                    Toast.makeText(getContext(), getString(R.string.failed_to_load_items_near_you), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Cập nhật phương thức này để nhận userLocation
    private void fetchPersonalizedItems(@Nullable android.location.Location userLocation) {
        // Only fetch personalized items if no category filter is active
        if (currentCategoryFilter != null) {
            return;
        }

        String userId = null;
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        final String finalUserId = userId;

        if (finalUserId == null) {
            Log.w(TAG, "User not logged in, cannot fetch personalized items.");
            requireActivity().runOnUiThread(() -> {
                fetchTrendingItemsForPersonalizationFallback(userLocation); // Truyền userLocation
            });
            return;
        }

        DatabaseReference userHistoryRef = FirebaseDatabase.getInstance().getReference("user_activity").child(finalUserId).child("viewed_categories");
        userHistoryRef.limitToLast(5).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                backgroundExecutor.execute(() -> {
                    Set<String> recentCategories = new HashSet<>();
                    for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                        recentCategories.add(categorySnapshot.getKey());
                    }

                    if (recentCategories.isEmpty()) {
                        Log.d(TAG, "No recent viewing history found for user " + finalUserId);
                        requireActivity().runOnUiThread(() -> {
                            if (isAdded()) {
                                Toast.makeText(getContext(), getString(R.string.no_browsing_history), Toast.LENGTH_SHORT).show();
                            }
                        });
                        fetchTrendingItemsForPersonalizationFallback(userLocation); // Truyền userLocation
                        return;
                    }

                    Log.d(TAG, "Recent categories for " + finalUserId + ": " + recentCategories.toString());
                    fetchItemsByCategories(new ArrayList<>(recentCategories), personalizedItemsAdapter, userLocation); // Truyền userLocation
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load user history: " + error.getMessage());
                if (isAdded()) {
                    Toast.makeText(getContext(), getString(R.string.failed_to_load_browsing_history), Toast.LENGTH_SHORT).show();
                }
                fetchTrendingItemsForPersonalizationFallback(userLocation); // Truyền userLocation
            }
        });
    }

    // Cập nhật phương thức này để nhận userLocation
    private void fetchTrendingItemsForPersonalizationFallback(@Nullable android.location.Location userLocation) {
        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("items");
        itemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                backgroundExecutor.execute(() -> {
                    List<Item> trendingItems = new ArrayList<>();
                    for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                        Item item = itemSnapshot.getValue(Item.class);
                        if (item != null && item.getStatus() != null && item.getStatus().equals("Available")) {
                            item.setId(itemSnapshot.getKey());
                            // Tính toán khoảng cách nếu userLocation có sẵn
                            if (userLocation != null && item.getLocation() != null && item.getLocation().getLat() != null && item.getLocation().getLng() != null) {
                                android.location.Location itemLocation = new android.location.Location("");
                                itemLocation.setLatitude(item.getLocation().getLat());
                                itemLocation.setLongitude(item.getLocation().getLng());
                                float distanceInKm = userLocation.distanceTo(itemLocation) / 1000;
                                item.setDistanceToUser(distanceInKm);
                            } else {
                                item.setDistanceToUser(-1); // Không hiển thị khoảng cách
                            }
                            trendingItems.add(item);
                        }
                    }

                    trendingItems.removeIf(item -> item.getRating_count() == null || item.getRating_count() <= 0);

                    Collections.sort(trendingItems, (item1, item2) -> {
                        int ratingComparison = Double.compare(item2.getAverage_rating(), item1.getAverage_rating());
                        if (ratingComparison != 0) {
                            return ratingComparison;
                        }
                        return Long.compare(item2.getRating_count(), item1.getRating_count());
                    });

                    List<Item> topTrendingItems = trendingItems.subList(0, Math.min(trendingItems.size(), 5));

                    requireActivity().runOnUiThread(() -> {
                        personalizedItemsAdapter.setItems(topTrendingItems);
                    });
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load fallback trending items for personalization: " + error.getMessage());
                requireActivity().runOnUiThread(() -> {
                    personalizedItemsAdapter.setItems(new ArrayList<>());
                });
            }
        });
    }

    // Cập nhật phương thức này để nhận userLocation
    private void fetchItemsByCategories(List<String> categories, ItemAdapter adapter, @Nullable android.location.Location userLocation) {
        if (categories.isEmpty()) {
            adapter.setItems(new ArrayList<>());
            return;
        }

        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("items");
        List<Item> resultItems = new ArrayList<>();
        AtomicInteger categoriesProcessed = new AtomicInteger(0);

        for (String category : categories) {
            itemsRef.orderByChild("category").equalTo(category).limitToFirst(5).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                        Item item = itemSnapshot.getValue(Item.class);
                        if (item != null && item.getStatus() != null && item.getStatus().equals("Available")) {
                            item.setId(itemSnapshot.getKey());
                            // Tính toán khoảng cách nếu userLocation có sẵn
                            if (userLocation != null && item.getLocation() != null && item.getLocation().getLat() != null && item.getLocation().getLng() != null) {
                                android.location.Location itemLocation = new android.location.Location("");
                                itemLocation.setLatitude(item.getLocation().getLat());
                                itemLocation.setLongitude(item.getLocation().getLng());
                                float distanceInKm = userLocation.distanceTo(itemLocation) / 1000;
                                item.setDistanceToUser(distanceInKm);
                            } else {
                                item.setDistanceToUser(-1); // Không hiển thị khoảng cách
                            }
                            resultItems.add(item);
                        }
                    }
                    if (categoriesProcessed.incrementAndGet() == categories.size()) {
                        backgroundExecutor.execute(() -> {
                            Set<Item> uniqueItems = new HashSet<>(resultItems);
                            List<Item> finalItems = new ArrayList<>(uniqueItems);
                            Collections.shuffle(finalItems);

                            requireActivity().runOnUiThread(() -> {
                                adapter.setItems(finalItems);
                            });
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load items for category " + category + ": " + error.getMessage());
                    if (categoriesProcessed.incrementAndGet() == categories.size()) {
                        backgroundExecutor.execute(() -> {
                            Set<Item> uniqueItems = new HashSet<>(resultItems);
                            List<Item> finalItems = new ArrayList<>(uniqueItems);
                            Collections.shuffle(finalItems);

                            requireActivity().runOnUiThread(() -> {
                                adapter.setItems(finalItems);
                            });
                        });
                    }
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchUserLocationAndItemsNearYou();
            } else {
                if (isAdded()) {
                    Toast.makeText(getContext(), getString(R.string.location_permission_denied), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (cancellationTokenSource != null) {
            cancellationTokenSource.cancel();
        }
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdownNow();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdownNow();
        }
    }

    // Add this method to the HomeFragment class
    private void executeIfPossible(Runnable task) {
        try {
            if (isExecutorActive && backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
                backgroundExecutor.execute(task);
            }
        } catch (RejectedExecutionException e) {
            Log.e(TAG, "Task rejected", e);
        }
    }
}
