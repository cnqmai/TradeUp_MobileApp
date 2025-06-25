package com.example.tradeup.fragment;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper; // Important for LocationCallback
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.location.Location; // For android.location.Location

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.activity.LoginActivity;
import com.example.tradeup.model.Item;
import com.example.tradeup.utils.FirebaseHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback; // Important for location updates
import com.google.android.gms.location.LocationRequest; // Important for location updates
import com.google.android.gms.location.LocationResult; // Important for location updates
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority; // Important for location request
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject; // For Cloudinary response parsing
import org.apache.commons.io.IOUtils; // For converting InputStream to byte array

import java.io.IOException;
import java.io.InputStream; // For image upload
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class EditItemFragment extends Fragment {

    // UI Elements
    private TextInputEditText etTitle, etDescription, etPrice, etLocation, etItemBehavior, etTags;
    private Spinner spinnerCategory, spinnerCondition;
    private Button btnGetLocationGps, btnUpdateItem;
    private ImageView ivAddImage;
    private LinearLayout llImagePreviews;

    // Helpers
    private FirebaseHelper firebaseHelper;
    private NavController navController;
    private OkHttpClient okHttpClient; // For Cloudinary upload

    private String currentItemId;
    private Item currentItem; // To hold the item data being edited

    // Image handling
    private List<Uri> selectedImageUris = new ArrayList<>(); // For newly selected images or existing ones from gallery/camera
    private List<String> existingImageUrls = new ArrayList<>(); // To store URLs of images already on Cloudinary
    private Uri cameraImageUri;
    private static final int MAX_IMAGES = 10;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback; // Callback for location updates
    private LocationRequest locationRequest; // Request for location updates
    private double currentLat; // Declared missing variable
    private double currentLng; // Declared missing variable

    // ActivityResultLaunchers for permissions and results
    private ActivityResultLauncher<String> requestLocationPermissionLauncher; // Declared missing variable
    private ActivityResultLauncher<String> requestCameraPermissionLauncher; // Declared missing variable
    private ActivityResultLauncher<Intent> pickImageLauncher; // Declared missing variable
    private ActivityResultLauncher<Uri> takePictureLauncher; // Declared missing variable

    public EditItemFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = new FirebaseHelper();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        okHttpClient = new OkHttpClient(); // Initialize OkHttpClient

        setupLocationRequest(); // Setup location request
        setupActivityResultLaunchers();

        // Get item ID from arguments
        if (getArguments() != null) {
            currentItemId = getArguments().getString("itemId");
            if (currentItemId == null) {
                Toast.makeText(requireContext(), "Lỗi: Không tìm thấy ID tin đăng để chỉnh sửa.", Toast.LENGTH_SHORT).show();
                if (navController != null) { // Check if navController is initialized
                    navController.popBackStack(); // Go back if ID is missing
                } else {
                    // Handle case where navController might not be ready yet in onCreate
                    // This might require a different approach or being called after onViewCreated
                }
            }
        } else {
            Toast.makeText(requireContext(), "Lỗi: Không có dữ liệu tin đăng để chỉnh sửa.", Toast.LENGTH_SHORT).show();
            if (navController != null) { // Check if navController is initialized
                navController.popBackStack();
            } else {
                // Handle case where navController might not be ready yet in onCreate
            }
        }
    }

    private void setupLocationRequest() {
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000) // 10s interval
                // .setWaitForActivityUpdates(false) // Removed due to 'Cannot resolve method' error
                .setMinUpdateIntervalMillis(5000) // minimum 5s interval
                .build();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_item, container, false); // Assuming you have fragment_edit_item.xml
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        initViews(view);
        setupSpinners();
        setupListeners();

        // Initialize location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        getAddressFromLocation(location);
                        // Stop updates after getting a location
                        fusedLocationClient.removeLocationUpdates(locationCallback);
                        return; // Process only the first good location
                    }
                }
            }
        };

        if (currentItemId != null) {
            loadItemData();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop location updates when the fragment is paused to save battery
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void initViews(View view) {
        etTitle = view.findViewById(R.id.et_edit_title);
        etDescription = view.findViewById(R.id.et_edit_description);
        etPrice = view.findViewById(R.id.et_edit_price);
        etLocation = view.findViewById(R.id.et_edit_location);
        etItemBehavior = view.findViewById(R.id.et_edit_item_behavior);
        etTags = view.findViewById(R.id.et_edit_tags);

        spinnerCategory = view.findViewById(R.id.spinner_edit_category);
        spinnerCondition = view.findViewById(R.id.spinner_edit_condition);

        btnGetLocationGps = view.findViewById(R.id.btn_edit_get_location_gps);
        btnUpdateItem = view.findViewById(R.id.btn_update_item);

        ivAddImage = view.findViewById(R.id.iv_edit_add_image);
        llImagePreviews = view.findViewById(R.id.ll_edit_image_previews);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        ArrayAdapter<CharSequence> conditionAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.conditions, android.R.layout.simple_spinner_item);
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCondition.setAdapter(conditionAdapter);
    }

    private void setupListeners() {
        btnGetLocationGps.setOnClickListener(v -> requestLocationPermission());
        ivAddImage.setOnClickListener(v -> showImagePickerDialog());
        btnUpdateItem.setOnClickListener(v -> {
            if (validateInputs()) {
                uploadNewImagesAndSubmitUpdate();
            }
        });
    }

    private boolean validateInputs() {
        boolean isValid = true;
        String title = Objects.requireNonNull(etTitle.getText()).toString().trim();
        String description = Objects.requireNonNull(etDescription.getText()).toString().trim();
        String price = Objects.requireNonNull(etPrice.getText()).toString().trim();
        String location = Objects.requireNonNull(etLocation.getText()).toString().trim();

        if (title.isEmpty()) {
            ((TextInputLayout)Objects.requireNonNull(etTitle.getParent()).getParent()).setError("Tiêu đề không được để trống");
            isValid = false;
        } else {
            ((TextInputLayout)Objects.requireNonNull(etTitle.getParent()).getParent()).setError(null);
        }

        if (description.isEmpty()) {
            ((TextInputLayout)Objects.requireNonNull(etDescription.getParent()).getParent()).setError("Mô tả không được để trống");
            isValid = false;
        } else {
            ((TextInputLayout)Objects.requireNonNull(etDescription.getParent()).getParent()).setError(null);
        }

        if (price.isEmpty()) {
            ((TextInputLayout)Objects.requireNonNull(etPrice.getParent()).getParent()).setError("Giá không được để trống");
            isValid = false;
        } else {
            try {
                long itemPrice = Long.parseLong(price);
                if (itemPrice <= 0) {
                    ((TextInputLayout)Objects.requireNonNull(etPrice.getParent()).getParent()).setError("Giá phải lớn hơn 0");
                    isValid = false;
                } else {
                    ((TextInputLayout)Objects.requireNonNull(etPrice.getParent()).getParent()).setError(null);
                }
            } catch (NumberFormatException e) {
                ((TextInputLayout)Objects.requireNonNull(etPrice.getParent()).getParent()).setError("Giá không hợp lệ");
                isValid = false;
            }
        }

        if (location.isEmpty()) {
            ((TextInputLayout)Objects.requireNonNull(etLocation.getParent()).getParent()).setError("Vị trí không được để trống");
            isValid = false;
        } else {
            ((TextInputLayout)Objects.requireNonNull(etLocation.getParent()).getParent()).setError(null);
        }

        if (selectedImageUris.isEmpty() && existingImageUrls.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng thêm ít nhất một ảnh cho tin đăng.", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void loadItemData() {
        firebaseHelper.getItem(currentItemId, new FirebaseHelper.DbReadCallback<Item>() {
            @Override
            public void onSuccess(Item item) {
                if (item != null) {
                    // --- BẮT ĐẦU: Thêm phần kiểm tra User ID ---
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser == null) {
                        Toast.makeText(requireContext(), "Bạn cần đăng nhập để chỉnh sửa tin đăng này.", Toast.LENGTH_LONG).show();
                        Log.e("EditItemFragment", "Người dùng chưa đăng nhập khi cố gắng tải tin đăng để chỉnh sửa.");
                        if (navController != null) {
                            navController.popBackStack();
                        }
                        return; // Dừng thực thi nếu không được xác thực
                    }

                    String currentUserId = currentUser.getUid();
                    if (item.getUser_id() == null || !item.getUser_id().equals(currentUserId)) {
                        // User ID không khớp, hoặc user_id của item là null
                        Toast.makeText(requireContext(), "Bạn không có quyền chỉnh sửa tin đăng này.", Toast.LENGTH_LONG).show();
                        Log.e("EditItemFragment", "Lỗi quyền: Người dùng " + currentUserId + " không phải chủ sở hữu của tin đăng " + currentItemId + " (chủ sở hữu: " + item.getUser_id() + ")");
                        if (navController != null) {
                            navController.popBackStack();
                        }
                        return; // Dừng thực thi nếu không được ủy quyền
                    }
                    // --- KẾT THÚC: Thêm phần kiểm tra User ID ---

                    currentItem = item; // Store the current item
                    etTitle.setText(item.getTitle());
                    etDescription.setText(item.getDescription());
                    etPrice.setText(String.valueOf(item.getPrice()));

                    // Initialize currentLat and currentLng from existing item data
                    if (item.getLocation() != null) {
                        currentLat = item.getLocation().getLat();
                        currentLng = item.getLocation().getLng();
                        if (item.getLocation().getManual_address() != null) {
                            etLocation.setText(item.getLocation().getManual_address());
                        } else {
                            etLocation.setText(String.format(Locale.getDefault(), "Lat: %.4f, Lng: %.4f", currentLat, currentLng));
                        }
                    }


                    // Set spinner selections
                    ArrayAdapter<CharSequence> categoryAdapter = (ArrayAdapter<CharSequence>) spinnerCategory.getAdapter();
                    if (categoryAdapter != null) {
                        int categoryPosition = categoryAdapter.getPosition(item.getCategory());
                        spinnerCategory.setSelection(categoryPosition);
                    }
                    ArrayAdapter<CharSequence> conditionAdapter = (ArrayAdapter<CharSequence>) spinnerCondition.getAdapter();
                    if (conditionAdapter != null) {
                        int conditionPosition = conditionAdapter.getPosition(item.getCondition());
                        spinnerCondition.setSelection(conditionPosition);
                    }

                    etItemBehavior.setText(item.getItem_behavior());
                    if (item.getTags() != null && !item.getTags().isEmpty()) {
                        etTags.setText(android.text.TextUtils.join(", ", item.getTags()));
                    }

                    // Load existing images
                    if (item.getPhotos() != null) {
                        existingImageUrls.addAll(item.getPhotos());
                        updateImagePreviews();
                    }
                } else {
                    Toast.makeText(requireContext(), "Không tìm thấy dữ liệu tin đăng.", Toast.LENGTH_SHORT).show();
                    if (navController != null) {
                        // Option 1: Pop back to ItemDetailFragment and ensure it's at the top, if it's already in the back stack.
                        // This is often sufficient if ItemDetailFragment correctly reloads in onResume.
                        // navController.popBackStack(); // Giữ nguyên dòng này nếu bạn muốn thử lại cách đơn giản

                        // Option 2 (Recommended if Option 1 fails): Explicitly navigate back to ItemDetailFragment
                        // This will create a new instance of ItemDetailFragment or bring an existing one to top,
                        // and ensure the itemId is passed.
                        // First, pop EditItemFragment from the stack to avoid accumulating fragments.
                        // Then navigate to ItemDetailFragment.
                        // We need to ensure that the currentId (itemId for the item being edited) is available here.
                        // Assuming 'currentItemId' is the variable holding the item ID in EditItemFragment.
                        try {
                            // Pop EditItemFragment from the back stack first.
                            navController.popBackStack();

                            // Then navigate to the ItemDetailFragment with the updated item ID.
                            // This requires an action in your sell_nav_graph.xml from EditItemFragment back to ItemDetailFragment.
                            // Let's assume you'll add an action like action_editItemFragment_to_itemDetailFragment
                            // If you already came from ItemDetailFragment, popBackStack() should work.
                            // If it still gives a blank screen, it means ItemDetailFragment might not be refreshing correctly.
                            // The logs are crucial here.

                            // If popBackStack() consistently fails to show data, despite onResume being called,
                            // you might need to navigate explicitly:
                            // Find the ItemDetailFragment's destination ID from your sell_nav_graph.xml, e.g., R.id.itemDetailFragment
                            // And use its associated action for navigation with arguments.
                            // Example if you define an action like this in sell_nav_graph.xml:
                            // <action
                            //     android:id="@+id/action_editItemFragment_to_itemDetailFragment_after_edit"
                            //     app:destination="@id/itemDetailFragment">
                            //     <argument android:name="itemId" app:argType="string" />
                            // </action>
                            // NavDirections action = EditItemFragmentDirections.actionEditItemFragmentToItemDetailFragmentAfterEdit(currentItemId);
                            // navController.navigate(action);

                            // For now, let's stick to popBackStack() and focus on the debugging logs.
                            // The blank screen might be due to ItemDetailFragment not correctly rendering data
                            // AFTER it's revealed by popBackStack, not due to navigation itself.
                            // The logs from the previous step are vital.
                        } catch (IllegalArgumentException e) {
                            Log.e("EditItemFragment", "Navigation error: " + e.getMessage());
                            Toast.makeText(requireContext(), "Lỗi điều hướng sau cập nhật: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(requireContext(), "Lỗi khi tải tin đăng: " + errorMessage, Toast.LENGTH_SHORT).show();
                Log.e("EditItemFragment", "Error loading item: " + errorMessage);
                if (navController != null) {
                    navController.popBackStack();
                }
            }
        });
    }

    private void setupActivityResultLaunchers() {
        // For Location Permission
        requestLocationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                checkLocationEnabledAndGetLocation();
            } else {
                Toast.makeText(requireContext(), "Quyền vị trí bị từ chối.", Toast.LENGTH_SHORT).show();
            }
        });

        // For Camera Permission
        requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                openCamera();
            } else {
                Toast.makeText(requireContext(), "Quyền máy ảnh bị từ chối.", Toast.LENGTH_SHORT).show();
            }
        });

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                if (result.getData().getClipData() != null) {
                    int count = result.getData().getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        if ((selectedImageUris.size() + existingImageUrls.size()) < MAX_IMAGES) {
                            selectedImageUris.add(result.getData().getClipData().getItemAt(i).getUri());
                        } else {
                            Toast.makeText(requireContext(), "Đã đạt giới hạn " + MAX_IMAGES + " ảnh.", Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }
                } else if (result.getData().getData() != null) {
                    if ((selectedImageUris.size() + existingImageUrls.size()) < MAX_IMAGES) {
                        selectedImageUris.add(result.getData().getData());
                    } else {
                        Toast.makeText(requireContext(), "Đã đạt giới hạn " + MAX_IMAGES + " ảnh.", Toast.LENGTH_SHORT).show();
                    }
                }
                updateImagePreviews();
            }
        });

        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success) {
                if ((selectedImageUris.size() + existingImageUrls.size()) < MAX_IMAGES) {
                    selectedImageUris.add(cameraImageUri);
                    updateImagePreviews();
                } else {
                    Toast.makeText(requireContext(), "Đã đạt giới hạn " + MAX_IMAGES + " ảnh.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "Chụp ảnh thất bại.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            checkLocationEnabledAndGetLocation();
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void checkLocationEnabledAndGetLocation() {
        LocationManager lm = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) { /* ignored */ }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) { /* ignored */ }

        if (!gps_enabled && !network_enabled) {
            new AlertDialog.Builder(requireContext())
                    .setMessage("GPS chưa được bật. Vui lòng bật GPS để lấy vị trí.")
                    .setPositiveButton("Mở cài đặt", (paramDialogInterface, paramInt) -> {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        } else {
            requestLocationUpdates();
        }
    }

    private void requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Thiếu quyền vị trí.", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(requireContext(), "Đang lấy vị trí hiện tại...", Toast.LENGTH_SHORT).show();
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void getAddressFromLocation(Location location) {
        if (location == null) {
            Toast.makeText(requireContext(), "Không thể lấy vị trí chính xác.", Toast.LENGTH_SHORT).show();
            return;
        }

        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String fullAddress = address.getAddressLine(0);
                etLocation.setText(fullAddress);

                // Update currentLat and currentLng
                this.currentLat = location.getLatitude();
                this.currentLng = location.getLongitude();

                // Update currentItem's location object
                if (currentItem.getLocation() != null) {
                    currentItem.getLocation().setLat(this.currentLat);
                    currentItem.getLocation().setLng(this.currentLng);
                    currentItem.getLocation().setManual_address(fullAddress);
                } else {
                    currentItem.setLocation(new com.example.tradeup.model.Location(this.currentLat, this.currentLng, fullAddress));
                }
            } else {
                // Update currentLat and currentLng
                this.currentLat = location.getLatitude();
                this.currentLng = location.getLongitude();
                etLocation.setText(String.format(Locale.getDefault(), "Lat: %.4f, Lng: %.4f", this.currentLat, this.currentLng));
                Toast.makeText(requireContext(), "Không tìm thấy địa chỉ, sử dụng tọa độ.", Toast.LENGTH_SHORT).show();

                // Update currentItem's location object
                if (currentItem.getLocation() != null) {
                    currentItem.getLocation().setLat(this.currentLat);
                    currentItem.getLocation().setLng(this.currentLng);
                    currentItem.getLocation().setManual_address(etLocation.getText().toString()); // Use the displayed text
                } else {
                    currentItem.setLocation(new com.example.tradeup.model.Location(this.currentLat, this.currentLng, etLocation.getText().toString()));
                }
            }
        } catch (IOException e) {
            Log.e("EditItemFragment", "Geocoder failed: " + e.getMessage());
            // Update currentLat and currentLng
            this.currentLat = location.getLatitude();
            this.currentLng = location.getLongitude();
            etLocation.setText(String.format(Locale.getDefault(), "Lat: %.4f, Lng: %.4f", this.currentLat, this.currentLng));
            Toast.makeText(requireContext(), "Lỗi Geocoder, sử dụng tọa độ.", Toast.LENGTH_SHORT).show();

            // Update currentItem's location object
            if (currentItem.getLocation() != null) {
                currentItem.getLocation().setLat(this.currentLat);
                currentItem.getLocation().setLng(this.currentLng);
                currentItem.getLocation().setManual_address(etLocation.getText().toString()); // Use the displayed text
            } else {
                currentItem.setLocation(new com.example.tradeup.model.Location(this.currentLat, this.currentLng, etLocation.getText().toString()));
            }
        }
    }

    private void showImagePickerDialog() {
        if ((selectedImageUris.size() + existingImageUrls.size()) >= MAX_IMAGES) {
            Toast.makeText(requireContext(), "Đã đạt giới hạn " + MAX_IMAGES + " ảnh.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Chọn ảnh");
        String[] options = {"Chọn từ thư viện", "Chụp ảnh mới"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                openImagePicker();
            } else {
                // Check camera permission before opening camera
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                }
            }
        });
        builder.show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickImageLauncher.launch(Intent.createChooser(intent, "Chọn ảnh"));
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        cameraImageUri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        takePictureLauncher.launch(cameraImageUri);
    }

    private void updateImagePreviews() {
        llImagePreviews.removeAllViews(); // Clear existing views
        // Display existing images first
        for (int i = 0; i < existingImageUrls.size(); i++) {
            String url = existingImageUrls.get(i);
            addImageViewToLayout(Uri.parse(url), true, i); // Pass true for existing images
        }
        // Display newly selected images
        for (int i = 0; i < selectedImageUris.size(); i++) {
            Uri uri = selectedImageUris.get(i);
            addImageViewToLayout(uri, false, i); // Pass false for new images
        }

        // Show add image icon if total images are less than MAX_IMAGES
        ivAddImage.setVisibility((selectedImageUris.size() + existingImageUrls.size()) < MAX_IMAGES ? View.VISIBLE : View.GONE);
    }

    private void addImageViewToLayout(Uri uri, boolean isExisting, int index) {
        ImageView imageView = new ImageView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) getResources().getDimension(R.dimen.preview_image_size),
                (int) getResources().getDimension(R.dimen.preview_image_size));
        params.setMargins(0, 0, (int) getResources().getDimension(R.dimen.image_preview_margin), 0);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundResource(R.drawable.border_image_preview);
        imageView.setPadding(2, 2, 2, 2);

        Glide.with(this)
                .load(uri)
                .centerCrop()
                .placeholder(R.drawable.img_placeholder)
                .into(imageView);

        imageView.setOnLongClickListener(v -> {
            showDeleteImageDialog(uri, isExisting); // Simplified dialog call
            return true;
        });
        llImagePreviews.addView(imageView);
    }

    // Simplified showDeleteImageDialog, it now only needs URI and type (existing/new)
    private void showDeleteImageDialog(Uri uriToDelete, boolean isExisting) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa ảnh")
                .setMessage("Bạn có muốn xóa ảnh này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    if (isExisting) {
                        existingImageUrls.remove(uriToDelete.toString());
                    } else {
                        selectedImageUris.remove(uriToDelete);
                    }
                    updateImagePreviews();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // --- Using Cloudinary for image uploads ---
    private void uploadNewImagesAndSubmitUpdate() {
        List<String> newUploadedUrls = new ArrayList<>();
        List<Uri> imagesToUpload = new ArrayList<>(selectedImageUris);

        if (imagesToUpload.isEmpty()) {
            // No new images to upload, proceed directly to update with existing images
            updateItemWithFinalImages(newUploadedUrls);
            return;
        }

        Toast.makeText(requireContext(), "Đang tải ảnh mới lên Cloudinary...", Toast.LENGTH_SHORT).show();
        final int[] uploadCount = {0};
        final boolean[] uploadFailed = {false};

        for (Uri uri : imagesToUpload) {
            if (uploadFailed[0]) {
                break;
            }

            try {
                InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    Toast.makeText(getContext(), "Không thể mở luồng đầu vào từ URI ảnh.", Toast.LENGTH_SHORT).show();
                    uploadFailed[0] = true;
                    return; // Stop processing if stream is null
                }

                byte[] imageBytes = IOUtils.toByteArray(inputStream);
                inputStream.close();

                String mimeType = requireContext().getContentResolver().getType(uri);
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }

                String fileExtension = getFileExtensionFromMimeType(mimeType);
                String fileName = "item_image_" + UUID.randomUUID().toString() + fileExtension;

                String cloudName = "dp6tzdsyt"; // Your Cloudinary Cloud Name
                String uploadPreset = "TradeUp"; // Your Cloudinary Unsigned Upload Preset

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", fileName,
                                RequestBody.create(imageBytes, MediaType.parse(mimeType)))
                        .addFormDataPart("upload_preset", uploadPreset)
                        .build();

                Request request = new Request.Builder()
                        .url("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload")
                        .post(requestBody)
                        .build();

                okHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Tải ảnh thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        Log.e("EditItemFragment", "Upload failed", e);
                        uploadFailed[0] = true; // Set fail flag
                        checkAndProceedUpdate(uploadCount[0], imagesToUpload.size(), newUploadedUrls, uploadFailed[0]);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            String errorBody = response.body().string();
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Tải ảnh thất bại: " + errorBody, Toast.LENGTH_LONG).show();
                                Log.e("EditItemFragment", "Upload failed with code " + response.code() + ": " + errorBody);
                            });
                            uploadFailed[0] = true; // Set fail flag
                        } else {
                            try {
                                String responseData = response.body().string();
                                JSONObject json = new JSONObject(responseData);
                                String url = json.getString("secure_url");
                                newUploadedUrls.add(url);
                                Log.d("EditItemFragment", "Ảnh đã tải lên Cloudinary: " + url);
                            } catch (Exception e) {
                                requireActivity().runOnUiThread(() ->
                                        Toast.makeText(getContext(), "Lỗi phân tích phản hồi Cloudinary: " + e.getMessage(), Toast.LENGTH_LONG).show());
                                Log.e("EditItemFragment", "Error parsing Cloudinary response", e);
                                uploadFailed[0] = true; // Set fail flag
                            }
                        }
                        checkAndProceedUpdate(uploadCount[0], imagesToUpload.size(), newUploadedUrls, uploadFailed[0]);
                    }
                });
            } catch (IOException e) {
                Toast.makeText(getContext(), "Lỗi đọc dữ liệu ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("EditItemFragment", "Error reading image data", e);
                uploadFailed[0] = true; // Set fail flag
                checkAndProceedUpdate(uploadCount[0], imagesToUpload.size(), newUploadedUrls, uploadFailed[0]);
            }
        }
    }

    private void checkAndProceedUpdate(int currentUploadCount, int totalUploads, List<String> newUploadedUrls, boolean uploadFailed) {
        final int[] count = {currentUploadCount + 1}; // Increment count for next check

        if (uploadFailed || count[0] == totalUploads) { // If all uploads done or one failed
            requireActivity().runOnUiThread(() -> {
                if (uploadFailed) {
                    Toast.makeText(requireContext(), "Một số ảnh không thể tải lên. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                    // Optionally, disable submit button or prevent further action
                } else {
                    updateItemWithFinalImages(newUploadedUrls);
                }
            });
        }
    }


    private String getFileExtensionFromMimeType(String mimeType) {
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        return extension != null ? "." + extension : "";
    }

    private void updateItemWithFinalImages(List<String> newUploadedUrls) {
        // Combine existing and newly uploaded image URLs
        List<String> finalImageUrls = new ArrayList<>(existingImageUrls);
        finalImageUrls.addAll(newUploadedUrls);

        String title = Objects.requireNonNull(etTitle.getText()).toString().trim();
        String description = Objects.requireNonNull(etDescription.getText()).toString().trim();
        long price = Long.parseLong(Objects.requireNonNull(etPrice.getText()).toString().trim());
        String category = spinnerCategory.getSelectedItem().toString();
        String condition = spinnerCondition.getSelectedItem().toString();
        String itemBehavior = Objects.requireNonNull(etItemBehavior.getText()).toString().trim();
        String manualAddress = Objects.requireNonNull(etLocation.getText()).toString().trim();

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", description);
        updates.put("price", price);
        updates.put("category", category);
        updates.put("condition", condition);

        // Location data
        Map<String, Object> itemLocation = new HashMap<>();
        itemLocation.put("lat", currentLat);
        itemLocation.put("lng", currentLng);
        itemLocation.put("manual_address", manualAddress); // Sử dụng địa chỉ nhập hoặc lấy từ GPS
        updates.put("location", itemLocation);


        updates.put("photos", finalImageUrls);
        updates.put("item_behavior", Objects.requireNonNull(etItemBehavior.getText()).toString().trim().isEmpty() ? null : etItemBehavior.getText().toString().trim());
        List<String> tags = new ArrayList<>();
        if (!Objects.requireNonNull(etTags.getText()).toString().trim().isEmpty()) {
            tags = Arrays.asList(Objects.requireNonNull(etTags.getText()).toString().trim().split("\\s*,\\s*"));
        }
        updates.put("tags", tags.isEmpty() ? null : tags);
        updates.put("updated_at", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));

        firebaseHelper.updateItem(currentItemId, updates, new FirebaseHelper.DbWriteCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), "Tin đăng đã được cập nhật thành công! ", Toast.LENGTH_LONG).show();
                if (navController != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("itemId", currentItemId);
                    navController.navigate(R.id.action_editItemFragment_to_itemDetailFragment, bundle);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(requireContext(), "Không thể cập nhật tin đăng: " + errorMessage, Toast.LENGTH_LONG).show();
                Log.e("EditItemFragment", "Error updating item in DB: " + errorMessage);
            }
        });
    }
}