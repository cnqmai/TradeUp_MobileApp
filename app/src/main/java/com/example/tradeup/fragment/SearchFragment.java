// SearchFragment.java (đã cập nhật định vị + nhập địa chỉ thủ công)

package com.example.tradeup.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController; // Thêm import này
import androidx.navigation.Navigation; // Thêm import này
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.R;
import com.example.tradeup.adapter.ItemAdapter;
import com.example.tradeup.model.Item;
import com.google.android.gms.location.*;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;

import java.io.IOException;
import java.util.*;

public class SearchFragment extends Fragment {

    private TextInputEditText etSearchKeyword, etMinPrice, etMaxPrice, etSearchDistance, etLocation;
    private Spinner spinnerSearchCategory, spinnerSearchCondition, spinnerSortOption;
    private RecyclerView rvSearchResults;
    private TextView tvCurrentLocation;
    private View advancedFilterSection;
    private ImageView ivSearchIcon, ivToggleAdvanced;
    private View btnPerformSearch;
    private Button btnGetLocationGps;

    private NavController navController; // KHAI BÁO NavController

    private final double defaultLat = 10.8275, defaultLng = 106.7000;
    private double currentLat = defaultLat, currentLng = defaultLng;

    private final List<Item> itemList = new ArrayList<>();
    private ItemAdapter itemAdapter;
    private final Handler searchHandler = new Handler();
    private Runnable searchRunnable;
    private DatabaseReference itemsRef;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        itemsRef = FirebaseDatabase.getInstance().getReference("items");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view); // KHỞI TẠO NavController

        initViews(view);
        setupSpinners();
        setupRecyclerView();
        setupListeners();

        tvCurrentLocation.setText("Vị trí hiện tại: đang lấy...");
    }

    private void initViews(View view) {
        etSearchKeyword = view.findViewById(R.id.et_search_keyword);
        spinnerSearchCategory = view.findViewById(R.id.spinner_search_category);
        spinnerSearchCondition = view.findViewById(R.id.spinner_search_condition);
        spinnerSortOption = view.findViewById(R.id.spinner_sort_option);
        etMinPrice = view.findViewById(R.id.et_min_price);
        etMaxPrice = view.findViewById(R.id.et_max_price);
        etSearchDistance = view.findViewById(R.id.et_search_distance);
        etLocation = view.findViewById(R.id.et_location);
        rvSearchResults = view.findViewById(R.id.rv_search_results);
        tvCurrentLocation = view.findViewById(R.id.tv_current_location);
        advancedFilterSection = view.findViewById(R.id.advanced_filter_section);
        ivSearchIcon = view.findViewById(R.id.iv_search_icon);
        ivToggleAdvanced = view.findViewById(R.id.iv_toggle_advanced);
        btnPerformSearch = view.findViewById(R.id.btn_perform_search);
        btnGetLocationGps = view.findViewById(R.id.btn_get_location_gps);
    }

    private void setupSpinners() {
        spinnerSearchCategory.setAdapter(ArrayAdapter.createFromResource(requireContext(), R.array.search_categories, android.R.layout.simple_spinner_item));
        spinnerSearchCondition.setAdapter(ArrayAdapter.createFromResource(requireContext(), R.array.search_conditions, android.R.layout.simple_spinner_item));
        spinnerSortOption.setAdapter(ArrayAdapter.createFromResource(requireContext(), R.array.search_sort_options, android.R.layout.simple_spinner_item));
    }

    private void setupRecyclerView() {
        rvSearchResults.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        itemAdapter = new ItemAdapter(requireContext(), itemList);
        rvSearchResults.setAdapter(itemAdapter);
        rvSearchResults.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.bottom = 24;
                outRect.right = 16;
            }
        });
        // THIẾT LẬP CLICK LISTENER CHO ITEM
        itemAdapter.setOnItemClickListener(itemId -> {
            Bundle bundle = new Bundle();
            bundle.putString("itemId", itemId);
            // Đảm bảo ID action này đúng trong nav_graph của bạn
            navController.navigate(R.id.action_searchFragment_to_itemDetailFragment, bundle);
        });
    }

    private void setupListeners() {
        ivToggleAdvanced.setVisibility(View.VISIBLE);

        ivToggleAdvanced.setOnClickListener(v -> {
            boolean expanded = advancedFilterSection.getVisibility() == View.VISIBLE;
            advancedFilterSection.setVisibility(expanded ? View.GONE : View.VISIBLE);
            ivToggleAdvanced.setImageResource(expanded ? R.drawable.ic_expand_more : R.drawable.ic_expand_less);
        });

        btnPerformSearch.setOnClickListener(v -> performSearch());
        btnGetLocationGps.setOnClickListener(v -> requestLocationPermission());

        etSearchKeyword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> performSearch();
                searchHandler.postDelayed(searchRunnable, 200);
            }
        });
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentAccurateLocation();
        } else {
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) getCurrentAccurateLocation();
                else Toast.makeText(requireContext(), "Bạn cần cấp quyền vị trí để sử dụng chức năng này.", Toast.LENGTH_SHORT).show();
            }).launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void getCurrentAccurateLocation() {
        // Thêm kiểm tra quyền ngay tại đây để đảm bảo an toàn, dù đã kiểm tra ở requestLocationPermission()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Thiếu quyền vị trí để lấy vị trí hiện tại.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cấu hình yêu cầu vị trí hiện tại với độ chính xác cao
        CurrentLocationRequest currentLocationRequest = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(30000) // Đặt thời gian chờ tối đa 30 giây
                .build();

        // Yêu cầu vị trí hiện tại
        fusedLocationClient.getCurrentLocation(currentLocationRequest, new CancellationTokenSource().getToken())
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        currentLat = location.getLatitude();
                        currentLng = location.getLongitude();
                        getAddressFromLocation(location);
                        Log.d("SearchFragment", "getCurrentLocation successful: Lat=" + location.getLatitude() + ", Lng=" + location.getLongitude());
                    } else {
                        Log.w("SearchFragment", "getCurrentLocation returned null location.");
                        Toast.makeText(requireContext(), "Không thể lấy vị trí hiện tại. Đảm bảo GPS đã bật và thử lại.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SearchFragment", "Failed to get current location: " + e.getMessage());
                    Toast.makeText(requireContext(), "Lỗi khi lấy vị trí hiện tại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void getAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                String address = addresses.get(0).getAddressLine(0);
                etLocation.setText(address);
                tvCurrentLocation.setText("Vị trí hiện tại: " + address);
            } else {
                // Nếu không tìm thấy địa chỉ, hiển thị tọa độ
                String coords = String.format(Locale.getDefault(), "Lat: %.6f, Lng: %.6f", location.getLatitude(), location.getLongitude());
                etLocation.setText(coords);
                tvCurrentLocation.setText("Vị trí hiện tại: " + coords + " (Không tìm thấy địa chỉ)");
                Toast.makeText(requireContext(), "Không tìm thấy địa chỉ cho vị trí này, hiển thị tọa độ.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e("Geocoder", "Lỗi lấy địa chỉ từ GPS", e);
            // Khi Geocoder lỗi, hiển thị tọa độ thay vì để trống
            String coords = String.format(Locale.getDefault(), "Lat: %.6f, Lng: %.6f", location.getLatitude(), location.getLongitude());
            etLocation.setText(coords);
            tvCurrentLocation.setText("Vị trí hiện tại: " + coords + " (Lỗi Geocoder)");
            Toast.makeText(requireContext(), "Lỗi khi lấy địa chỉ từ GPS, hiển thị tọa độ. Vui lòng kiểm tra kết nối mạng.", Toast.LENGTH_LONG).show();
        }
    }

    private void performSearch() {
        String keyword = Objects.requireNonNull(etSearchKeyword.getText()).toString().trim().toLowerCase();
        String locationText = Objects.requireNonNull(etLocation.getText()).toString().trim();
        String selectedCategory = spinnerSearchCategory.getSelectedItem().toString();
        String selectedCondition = spinnerSearchCondition.getSelectedItem().toString();
        String selectedSort = spinnerSortOption.getSelectedItem().toString();

        long min = -1, max = -1;
        double dist = -1;

        try {
            if (!etMinPrice.getText().toString().trim().isEmpty()) min = Long.parseLong(etMinPrice.getText().toString().trim());
            if (!etMaxPrice.getText().toString().trim().isEmpty()) max = Long.parseLong(etMaxPrice.getText().toString().trim());
            if (!etSearchDistance.getText().toString().trim().isEmpty()) dist = Double.parseDouble(etSearchDistance.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Vui lòng nhập giá/khoảng cách hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!locationText.isEmpty()) {
            try {
                List<Address> addresses = new Geocoder(requireContext(), Locale.getDefault()).getFromLocationName(locationText, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    currentLat = addresses.get(0).getLatitude();
                    currentLng = addresses.get(0).getLongitude();
                }
            } catch (IOException e) {
                Log.e("Geocode", "Không tìm thấy địa chỉ", e);
            }
        }

        final long finalMin = min;
        final long finalMax = max;
        final double finalDist = dist;

        Query query = selectedCategory.equals("Tất cả danh mục") ? itemsRef : itemsRef.orderByChild("category").equalTo(selectedCategory);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemList.clear();
                List<Item> filtered = new ArrayList<>();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    Item item = snap.getValue(Item.class);
                    if (item == null) continue;

                    // THÊM DÒNG NÀY ĐỂ ĐẢM BẢO ID ĐƯỢC GÁN
                    item.setId(snap.getKey());

                    boolean matchKeyword = keyword.isEmpty() ||
                            (item.getTitle() != null && item.getTitle().toLowerCase().contains(keyword)) ||
                            (item.getDescription() != null && item.getDescription().toLowerCase().contains(keyword));

                    boolean matchCondition = selectedCondition.equals("Tất cả tình trạng") ||
                            (item.getCondition() != null && item.getCondition().equals(selectedCondition));

                    boolean matchPrice = item.getPrice() != null &&
                            (finalMin == -1 || item.getPrice() >= finalMin) &&
                            (finalMax == -1 || item.getPrice() <= finalMax);

                    boolean matchDistance = true;
                    if (finalDist != -1 && item.getLocation() != null) {
                        float[] result = new float[1];
                        Location.distanceBetween(currentLat, currentLng, item.getLocation().getLat(), item.getLocation().getLng(), result);
                        float km = result[0] / 1000;
                        matchDistance = km <= finalDist;
                    }

                    if (matchKeyword && matchCondition && matchPrice && matchDistance) {
                        filtered.add(item);
                    }
                }

                switch (selectedSort) {
                    case "Mới nhất":
                        filtered.sort((a, b) -> b.getCreated_at().compareTo(a.getCreated_at()));
                        break;
                    case "Giá tăng dần":
                        filtered.sort(Comparator.comparingLong(Item::getPrice));
                        break;
                    case "Giá giảm dần":
                        filtered.sort((a, b) -> Long.compare(b.getPrice(), a.getPrice()));
                        break;
                }

                itemList.addAll(filtered);
                itemAdapter.setItems(itemList);
                Toast.makeText(requireContext(), "Tìm thấy " + itemList.size() + " mặt hàng.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}