// SearchFragment.java (đã cập nhật định vị + nhập địa chỉ thủ công + sắp xếp theo khoảng cách)

package com.example.tradeup.fragment;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location; // Ensure this is android.location.Location
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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.R;
import com.example.tradeup.adapter.ItemAdapter;
import com.example.tradeup.model.Item;
import com.google.android.gms.location.*; // For FusedLocationProviderClient, CurrentLocationRequest, Priority
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

    private NavController navController;

    // Nên lưu trữ vị trí hiện tại của người dùng (từ GPS hoặc nhập tay)
    // Các biến này sẽ được cập nhật khi lấy GPS hoặc khi địa chỉ tùy chỉnh được nhập
    private double currentLat = 0.0;
    private double currentLng = 0.0;

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
        // Khởi tạo vị trí mặc định ban đầu (có thể là vị trí trung tâm thành phố nếu không có GPS)
        // hoặc để 0,0 và chỉ cập nhật khi có GPS/nhập tay.
        // Tùy thuộc vào FR của bạn: nếu muốn ưu tiên gần người dùng, bạn cần vị trí mặc định hợp lý.
        // Để cho đơn giản trong ví dụ này, chúng ta sẽ dựa vào GPS hoặc nhập thủ công.
        // Nếu không có cả 2, currentLat/Lng sẽ là 0.0 và khoảng cách sẽ không được tính.
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        initViews(view);
        setupSpinners();
        setupRecyclerView();
        setupListeners();

        // Ban đầu, cố gắng lấy vị trí GPS để hiển thị
        requestLocationPermission(); // Cố gắng lấy vị trí ngay khi fragment được tạo
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

        // Listener cho spinner sắp xếp để tự động thực hiện tìm kiếm lại
        spinnerSortOption.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                performSearch(); // Gọi tìm kiếm lại khi tùy chọn sắp xếp thay đổi
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
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
        itemAdapter.setOnItemClickListener(itemId -> {
            Bundle bundle = new Bundle();
            bundle.putString("itemId", itemId);
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

        // Listener cho etLocation để cập nhật vị trí tìm kiếm khi người dùng nhập tay
        etLocation.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {
                // Khi người dùng nhập tay vào etLocation, cập nhật currentLat/Lng từ địa chỉ đó
                updateLocationFromAddressInput(s.toString());
                searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> performSearch();
                searchHandler.postDelayed(searchRunnable, 500); // Có thể tăng delay để Geocoder có thời gian phản hồi
            }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
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
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Thiếu quyền vị trí để lấy vị trí hiện tại.", Toast.LENGTH_SHORT).show();
            return;
        }

        CurrentLocationRequest currentLocationRequest = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(30000)
                .build();

        tvCurrentLocation.setText("Vị trí hiện tại: đang lấy GPS..."); // Cập nhật trạng thái
        fusedLocationClient.getCurrentLocation(currentLocationRequest, new CancellationTokenSource().getToken())
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        currentLat = location.getLatitude();
                        currentLng = location.getLongitude();
                        getAddressFromLocation(location);
                        Log.d("SearchFragment", "getCurrentLocation successful: Lat=" + location.getLatitude() + ", Lng=" + location.getLongitude());
                        performSearch(); // Tự động tìm kiếm lại sau khi lấy được vị trí GPS
                    } else {
                        Log.w("SearchFragment", "getCurrentLocation returned null location.");
                        tvCurrentLocation.setText("Vị trí hiện tại: không xác định."); // Cập nhật trạng thái
                        Toast.makeText(requireContext(), "Không thể lấy vị trí hiện tại. Đảm bảo GPS đã bật và thử lại.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SearchFragment", "Failed to get current location: " + e.getMessage());
                    tvCurrentLocation.setText("Vị trí hiện tại: lỗi GPS."); // Cập nhật trạng thái
                    Toast.makeText(requireContext(), "Lỗi khi lấy vị trí hiện tại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateLocationFromAddressInput(String addressText) {
        if (addressText.isEmpty()) {
            // Nếu người dùng xóa địa chỉ, có thể reset vị trí hoặc không làm gì
            currentLat = 0.0; // Reset về 0 hoặc một giá trị mặc định không hợp lệ
            currentLng = 0.0;
            tvCurrentLocation.setText("Vị trí hiện tại: chưa xác định.");
            return;
        }

        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(addressText, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address firstAddress = addresses.get(0);
                currentLat = firstAddress.getLatitude();
                currentLng = firstAddress.getLongitude();
                tvCurrentLocation.setText("Vị trí tìm kiếm: " + addressText);
                Log.d(TAG, "Vị trí cập nhật từ nhập tay: Lat=" + currentLat + ", Lng=" + currentLng);
            } else {
                tvCurrentLocation.setText("Vị trí tìm kiếm: Không tìm thấy địa chỉ.");
                currentLat = 0.0; // Nếu không tìm thấy, reset để không tính khoảng cách
                currentLng = 0.0;
            }
        } catch (IOException e) {
            Log.e(TAG, "Lỗi khi lấy tọa độ từ địa chỉ nhập tay: " + e.getMessage());
            tvCurrentLocation.setText("Vị trí tìm kiếm: Lỗi Geocoder.");
            currentLat = 0.0;
            currentLng = 0.0;
        }
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
                String coords = String.format(Locale.getDefault(), "Lat: %.6f, Lng: %.6f", location.getLatitude(), location.getLongitude());
                etLocation.setText(coords);
                tvCurrentLocation.setText("Vị trí hiện tại: " + coords + " (Không tìm thấy địa chỉ)");
                Toast.makeText(requireContext(), "Không tìm thấy địa chỉ cho vị trí này, hiển thị tọa độ.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e("Geocoder", "Lỗi lấy địa chỉ từ GPS", e);
            String coords = String.format(Locale.getDefault(), "Lat: %.6f, Lng: %.6f", location.getLatitude(), location.getLongitude());
            etLocation.setText(coords);
            tvCurrentLocation.setText("Vị trí hiện tại: " + coords + " (Lỗi Geocoder)");
            Toast.makeText(requireContext(), "Lỗi khi lấy địa chỉ từ GPS, hiển thị tọa độ. Vui lòng kiểm tra kết nối mạng.", Toast.LENGTH_LONG).show();
        }
    }

    private void performSearch() {
        if (!isAdded()) return; // Early exit if fragment is detached

        String keyword = Objects.requireNonNull(etSearchKeyword.getText()).toString().trim().toLowerCase();
        String selectedCategory = spinnerSearchCategory.getSelectedItem().toString();
        String selectedCondition = spinnerSearchCondition.getSelectedItem().toString();
        String selectedSort = spinnerSortOption.getSelectedItem().toString(); // Lấy tùy chọn sắp xếp

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

        final long finalMin = min;
        final long finalMax = max;
        final double finalDist = dist;
        final double searchLat = currentLat; // Sử dụng vị trí đã được cập nhật
        final double searchLng = currentLng; // Sử dụng vị trí đã được cập nhật

        Query query = selectedCategory.equals("Tất cả danh mục") ? itemsRef : itemsRef.orderByChild("category").equalTo(selectedCategory);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return; // Check if fragment is still added

                itemList.clear();
                List<Item> filtered = new ArrayList<>();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    Item item = snap.getValue(Item.class);
                    if (item == null) continue;

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
                    // Chỉ tính khoảng cách nếu có vị trí tìm kiếm hợp lệ (không phải 0,0) và khoảng cách tìm kiếm được chỉ định
                    if (finalDist != -1 && item.getLocation() != null && item.getLocation().getLat() != null && item.getLocation().getLng() != null
                            && (searchLat != 0.0 || searchLng != 0.0)) { // Kiểm tra vị trí tìm kiếm hợp lệ
                        float[] result = new float[1];
                        Location.distanceBetween(searchLat, searchLng, item.getLocation().getLat(), item.getLocation().getLng(), result);
                        float km = result[0] / 1000;
                        matchDistance = km <= finalDist;
                        item.setDistanceToUser(km); // Set distance for sorting
                    } else {
                        item.setDistanceToUser(Double.MAX_VALUE); // Đặt khoảng cách rất lớn nếu không tính được, để không ưu tiên khi sắp xếp theo khoảng cách
                    }


                    if (matchKeyword && matchCondition && matchPrice && matchDistance) {
                        filtered.add(item);
                    }
                }

                // Sắp xếp lại danh sách đã lọc dựa trên tùy chọn từ spinner
                switch (selectedSort) {
                    case "Mới nhất":
                        filtered.sort((a, b) -> {
                            if (a.getCreated_at() == null || b.getCreated_at() == null) return 0;
                            return b.getCreated_at().compareTo(a.getCreated_at());
                        });
                        break;
                    case "Giá tăng dần":
                        filtered.sort(Comparator.comparingLong(item -> item.getPrice() != null ? item.getPrice() : 0L));
                        break;
                    case "Giá giảm dần":
                        filtered.sort((a, b) -> {
                            Long priceA = a.getPrice() != null ? a.getPrice() : 0L;
                            Long priceB = b.getPrice() != null ? b.getPrice() : 0L;
                            return Long.compare(priceB, priceA);
                        });
                        break;
                    case "Khoảng cách (gần nhất)": // THÊM CASE NÀY
                        // Sắp xếp các mục theo khoảng cách đã tính toán (nhỏ nhất trước)
                        filtered.sort(Comparator.comparingDouble(Item::getDistanceToUser));
                        break;
                }

                itemList.addAll(filtered);
                itemAdapter.setItems(itemList);
                Toast.makeText(requireContext(), "Tìm thấy " + itemList.size() + " mặt hàng.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) { // Check if fragment is still added
                    Toast.makeText(requireContext(), "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
