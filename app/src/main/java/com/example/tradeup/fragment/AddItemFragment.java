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
import android.os.Build;
import android.os.Bundle;
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
import android.widget.TextView;

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
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import android.location.Location;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.CancellationToken;

public class AddItemFragment extends Fragment {

    // UI Elements
    private TextInputEditText etTitle, etDescription, etPrice, etLocation, etItemBehavior, etTags;
    private Spinner spinnerCategory, spinnerCondition;
    private Button btnGetLocationGps, btnPreviewItem, btnSubmitItem;
    private LinearLayout llAddImagePlaceholder; // Changed from ImageView to LinearLayout
    private LinearLayout llImagePreviews;
    private ImageView ivBackButton;

    // Helpers
    private FirebaseHelper firebaseHelper;
    private NavController navController;
    private OkHttpClient okHttpClient;

    // Image handling
    private List<Uri> selectedImageUris = new ArrayList<>();
    private Uri cameraImageUri;
    private static final int MAX_IMAGES = 10;
    private FusedLocationProviderClient fusedLocationClient;

    private static final String TAG = "AddItemFragment";

    // Biến để lưu trữ tọa độ GPS
    private double currentLat = 0.0;
    private double currentLng = 0.0;

    // ActivityResultLaunchers for permissions and results
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;

    public AddItemFragment() {
        // Required empty public constructor
    }

    public static AddItemFragment newInstance() {
        return new AddItemFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = new FirebaseHelper();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        okHttpClient = new OkHttpClient();
        setupActivityResultLaunchers();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_item, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        initViews(view);
        setupSpinners();
        setupListeners();
        updateImagePreviews(); // Call this to initially show the add image placeholder
    }

    private void initViews(View view) {
        ivBackButton = view.findViewById(R.id.iv_back_button_add_item);
        etTitle = view.findViewById(R.id.et_title);
        etDescription = view.findViewById(R.id.et_description);
        etPrice = view.findViewById(R.id.et_price);
        etLocation = view.findViewById(R.id.et_location);
        etItemBehavior = view.findViewById(R.id.et_item_behavior);
        etTags = view.findViewById(R.id.et_tags);

        spinnerCategory = view.findViewById(R.id.spinner_category);
        spinnerCondition = view.findViewById(R.id.spinner_condition);

        btnGetLocationGps = view.findViewById(R.id.btn_get_location_gps);
        btnPreviewItem = view.findViewById(R.id.btn_preview_item);
        btnSubmitItem = view.findViewById(R.id.btn_submit_item);

        llAddImagePlaceholder = view.findViewById(R.id.ll_add_image_placeholder); // Changed ID
        llImagePreviews = view.findViewById(R.id.ll_image_previews);
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
        ivBackButton.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigateUp();
            }
        });
        btnGetLocationGps.setOnClickListener(v -> requestLocationPermission());
        llAddImagePlaceholder.setOnClickListener(v -> showImagePickerDialog()); // Changed listener
        btnPreviewItem.setOnClickListener(v -> {
            if (validateInputs()) {
                previewItem();
            }
        });
        btnSubmitItem.setOnClickListener(v -> {
            if (validateInputs()) {
                uploadImagesAndSubmitItem();
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
            ((TextInputLayout)etTitle.getParent().getParent()).setError("Tiêu đề không được để trống");
            isValid = false;
        } else {
            ((TextInputLayout)etTitle.getParent().getParent()).setError(null);
        }

        if (description.isEmpty()) {
            ((TextInputLayout)etDescription.getParent().getParent()).setError("Mô tả không được để trống");
            isValid = false;
        } else {
            ((TextInputLayout)etDescription.getParent().getParent()).setError(null);
        }

        if (price.isEmpty()) {
            ((TextInputLayout)etPrice.getParent().getParent()).setError("Giá không được để trống");
            isValid = false;
        } else {
            try {
                long itemPrice = Long.parseLong(price);
                if (itemPrice <= 0) {
                    ((TextInputLayout)etPrice.getParent().getParent()).setError("Giá phải lớn hơn 0");
                    isValid = false;
                } else {
                    ((TextInputLayout)etPrice.getParent().getParent()).setError(null);
                }
            } catch (NumberFormatException e) {
                ((TextInputLayout)etPrice.getParent().getParent()).setError("Giá không hợp lệ");
                isValid = false;
            }
        }

        if (location.isEmpty()) {
            ((TextInputLayout)etLocation.getParent().getParent()).setError("Vị trí không được để trống");
            isValid = false;
        } else {
            ((TextInputLayout)etLocation.getParent().getParent()).setError(null);
        }

        if (selectedImageUris.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng thêm ít nhất một ảnh cho tin đăng.", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void setupActivityResultLaunchers() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                checkLocationEnabledAndGetLocation();
            } else {
                Toast.makeText(requireContext(), "Quyền vị trí bị từ chối.", Toast.LENGTH_SHORT).show();
            }
        });

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                if (result.getData().getClipData() != null) {
                    int count = result.getData().getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        if (selectedImageUris.size() < MAX_IMAGES) {
                            selectedImageUris.add(result.getData().getClipData().getItemAt(i).getUri());
                        }
                    }
                } else if (result.getData().getData() != null) {
                    if (selectedImageUris.size() < MAX_IMAGES) {
                        selectedImageUris.add(result.getData().getData());
                    }
                }
                updateImagePreviews();
            }
        });

        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success) {
                if (selectedImageUris.size() < MAX_IMAGES) {
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
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void checkLocationEnabledAndGetLocation() {
        LocationManager lm = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) { }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) { }

        if (!gps_enabled && !network_enabled) {
            new AlertDialog.Builder(requireContext())
                    .setMessage("GPS chưa được bật. Vui lòng bật GPS để lấy vị trí.")
                    .setPositiveButton("Mở cài đặt", (paramDialogInterface, paramInt) -> {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        } else {
            getCurrentAccurateLocation();
        }
    }

    private void getCurrentAccurateLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        CurrentLocationRequest currentLocationRequest = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(30000)
                .build();

        fusedLocationClient.getCurrentLocation(currentLocationRequest, new CancellationTokenSource().getToken())
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        Log.d(TAG, "getCurrentLocation successful: Lat=" + location.getLatitude() + ", Lng=" + location.getLongitude());
                        getAddressFromLocation(location);
                    } else {
                        Log.w(TAG, "getCurrentLocation returned null location.");
                        Toast.makeText(requireContext(), "Không thể lấy vị trí hiện tại. Đảm bảo GPS đã bật và thử lại.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get current location: " + e.getMessage());
                    Toast.makeText(requireContext(), "Lỗi khi lấy vị trí hiện tại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void getAddressFromLocation(Location location) {
        if (location == null) {
            Toast.makeText(requireContext(), "Không thể lấy vị trí chính xác.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("EditItemFragment", "Raw GPS Location received: Lat=" + location.getLatitude() + ", Lng=" + location.getLongitude());

        Geocoder geocoder = new Geocoder(requireContext(), new Locale("vi", "VN"));
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String fullAddress = address.getAddressLine(0);
                etLocation.setText(fullAddress);

                this.currentLat = location.getLatitude();
                this.currentLng = location.getLongitude();

                Log.d("AddItemFragment", "Geocoded address (from GPS): " + fullAddress);

            } else {
                this.currentLat = location.getLatitude();
                this.currentLng = location.getLongitude();
                etLocation.setText(String.format(Locale.getDefault(), "Lat: %.4f, Lng: %.4f", this.currentLat, this.currentLng));
                Toast.makeText(requireContext(), "Không tìm thấy địa chỉ, sử dụng tọa độ.", Toast.LENGTH_SHORT).show();
                Log.w("AddItemFragment", "Geocoder returned no address for Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude());
            }
        } catch (IOException e) {
            Log.e("AddItemFragment", "Geocoder failed: " + e.getMessage());
            this.currentLat = location.getLatitude();
            this.currentLng = location.getLongitude();
            etLocation.setText(String.format(Locale.getDefault(), "Lat: %.4f, Lng: %.4f", this.currentLat, this.currentLng));
            Toast.makeText(requireContext(), "Lỗi Geocoder, sử dụng tọa độ.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Chọn ảnh");
        String[] options = {"Chọn từ thư viện", "Chụp ảnh mới"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                openImagePicker();
            } else {
                openCamera();
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

        // Add dynamically selected image previews
        for (int i = 0; i < selectedImageUris.size(); i++) {
            Uri uri = selectedImageUris.get(i);
            ImageView imageView = new ImageView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) getResources().getDimension(R.dimen.preview_image_size),
                    (int) getResources().getDimension(R.dimen.preview_image_size));
            params.setMargins(0, 0, (int) getResources().getDimension(R.dimen.image_preview_margin), 0);
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundResource(R.drawable.border_image_preview); // Make sure this drawable exists
            imageView.setPadding(2, 2, 2, 2);

            Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .placeholder(R.drawable.img_placeholder)
                    .into(imageView);

            int finalI = i;
            imageView.setOnLongClickListener(v -> {
                showDeleteImageDialog(finalI);
                return true;
            });
            llImagePreviews.addView(imageView);
        }

        // Add the llAddImagePlaceholder if less than MAX_IMAGES
        if (selectedImageUris.size() < MAX_IMAGES) {
            // Ensure llAddImagePlaceholder is added back if it was removed
            if (llAddImagePlaceholder.getParent() == null) {
                // This part might need adjustment based on your XML structure.
                // If llAddImagePlaceholder is directly inside llImagePreviews in XML,
                // you might need to add it back to llImagePreviews.
                // If it's a separate element in XML, then its visibility is enough.
                // Assuming llAddImagePlaceholder is a direct child of llImagePreviews
                // and you removed all views with removeAllViews().
                // The current XML structure has ll_add_image_placeholder inside ll_image_previews
                // which is inside a HorizontalScrollView.
                // So, we need to add it back to llImagePreviews if it's not there.
                // However, the XML provided has ll_add_image_placeholder as a direct child
                // of ll_image_previews. So, removeAllViews() will remove it.
                // We need to re-add it.

                // To avoid re-creating the placeholder view,
                // we can just manage its visibility or ensure it's always the last child.
                // Let's re-add it if it's not present.
                llImagePreviews.addView(llAddImagePlaceholder);
            }
            llAddImagePlaceholder.setVisibility(View.VISIBLE);
        } else {
            llAddImagePlaceholder.setVisibility(View.GONE);
        }
    }


    private void showDeleteImageDialog(int index) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa ảnh")
                .setMessage("Bạn có muốn xóa ảnh này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    selectedImageUris.remove(index);
                    updateImagePreviews();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void uploadImagesAndSubmitItem() {
        if (selectedImageUris.isEmpty()) {
            submitItem(new ArrayList<>());
            return;
        }

        Toast.makeText(requireContext(), "Đang tải ảnh lên Cloudinary...", Toast.LENGTH_SHORT).show();
        List<String> uploadedImageUrls = new ArrayList<>();
        final int[] uploadCount = {0};
        final boolean[] uploadFailed = {false};

        for (Uri uri : selectedImageUris) {
            if (uploadFailed[0]) {
                break;
            }

            try {
                InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    Toast.makeText(getContext(), "Không thể mở luồng đầu vào từ URI ảnh.", Toast.LENGTH_SHORT).show();
                    uploadFailed[0] = true;
                    return;
                }

                byte[] imageBytes = IOUtils.toByteArray(inputStream);
                inputStream.close();

                String mimeType = requireContext().getContentResolver().getType(uri);
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }

                String fileExtension = getFileExtensionFromMimeType(mimeType);
                String fileName = "item_image_" + UUID.randomUUID().toString() + fileExtension;

                String cloudName = "dp6tzdsyt";
                String uploadPreset = "TradeUp";

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
                        Log.e("AddItemFragment", "Upload failed", e);
                        uploadFailed[0] = true;
                        checkAndProceedSubmit(uploadCount[0], selectedImageUris.size(), uploadedImageUrls, uploadFailed[0]);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            String errorBody = response.body() != null ? response.body().string() : "No error body";
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Lỗi tải ảnh: " + response.code() + " " + response.message() + " - " + errorBody, Toast.LENGTH_LONG).show();
                                Log.e("AddItemFragment", "Upload error: " + response.code() + " " + response.message() + " - " + errorBody);
                            });
                            uploadFailed[0] = true;
                            checkAndProceedSubmit(uploadCount[0], selectedImageUris.size(), uploadedImageUrls, uploadFailed[0]);
                            return;
                        }

                        String json = response.body().string();
                        try {
                            JSONObject jsonObject = new JSONObject(json);
                            String imageUrl = jsonObject.getString("secure_url");

                            synchronized (uploadedImageUrls) {
                                uploadedImageUrls.add(imageUrl);
                                uploadCount[0]++;
                            }
                            checkAndProceedSubmit(uploadCount[0], selectedImageUris.size(), uploadedImageUrls, uploadFailed[0]);

                        } catch (Exception e) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Lỗi xử lý phản hồi Cloudinary: " + e.getMessage(), Toast.LENGTH_LONG).show());
                            Log.e("AddItemFragment", "Error parsing Cloudinary response", e);
                            uploadFailed[0] = true;
                            checkAndProceedSubmit(uploadCount[0], selectedImageUris.size(), uploadedImageUrls, uploadFailed[0]);
                        } finally {
                            if (response.body() != null) {
                                response.body().close();
                            }
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Lỗi đọc ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("AddItemFragment", "Error reading image URI", e);
                uploadFailed[0] = true;
                checkAndProceedSubmit(uploadCount[0], selectedImageUris.size(), uploadedImageUrls, uploadFailed[0]);
            }
        }
    }

    private void checkAndProceedSubmit(int currentUploadCount, int totalImagesToUpload, List<String> uploadedImageUrls, boolean anyUploadFailed) {
        if (currentUploadCount == totalImagesToUpload || anyUploadFailed) {
            requireActivity().runOnUiThread(() -> {
                if (anyUploadFailed && uploadedImageUrls.size() < totalImagesToUpload) {
                    Toast.makeText(requireContext(), "Một số ảnh không tải lên được. Tin đăng sẽ được tạo với các ảnh đã tải thành công.", Toast.LENGTH_LONG).show();
                }
                submitItem(uploadedImageUrls);
            });
        }
    }

    private String getFileExtensionFromMimeType(String mimeType) {
        if (mimeType == null) {
            return ".bin";
        }
        switch (mimeType) {
            case "image/jpeg":
                return ".jpeg";
            case "image/png":
                return ".png";
            case "image/gif":
                return ".gif";
            case "image/webp":
                return ".webp";
            default:
                String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                return extension != null ? "." + extension : ".bin";
        }
    }

    private void previewItem() {
        String title = Objects.requireNonNull(etTitle.getText()).toString().trim();
        String description = Objects.requireNonNull(etDescription.getText()).toString().trim();
        long price = Long.parseLong(Objects.requireNonNull(etPrice.getText()).toString().trim());
        String category = spinnerCategory.getSelectedItem().toString();
        String condition = spinnerCondition.getSelectedItem().toString();
        String location = Objects.requireNonNull(etLocation.getText()).toString().trim();
        String itemBehavior = Objects.requireNonNull(etItemBehavior.getText()).toString().trim();
        String[] tags = {};
        if (!Objects.requireNonNull(etTags.getText()).toString().trim().isEmpty()) {
            tags = etTags.getText().toString().trim().split("\\s*,\\s*");
        }

        String[] imageUrls = new String[selectedImageUris.size()];
        for (int i = 0; i < selectedImageUris.size(); i++) {
            imageUrls[i] = selectedImageUris.get(i).toString();
        }

        AddItemFragmentDirections.ActionAddItemFragmentToPreviewItemFragment action =
                AddItemFragmentDirections.actionAddItemFragmentToPreviewItemFragment(
                        title,
                        description,
                        price,
                        category,
                        condition,
                        location,
                        itemBehavior,
                        tags,
                        imageUrls
                );
        navController.navigate(action);
    }

    private void submitItem(List<String> uploadedImageUrls) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Bạn cần đăng nhập để đăng tin.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
            return;
        }

        String userId = currentUser.getUid();
        String userDisplayName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Người dùng không xác định";
        String title = Objects.requireNonNull(etTitle.getText()).toString().trim();
        String description = Objects.requireNonNull(etDescription.getText()).toString().trim();
        Long price = Long.parseLong(Objects.requireNonNull(etPrice.getText()).toString().trim());
        String category = spinnerCategory.getSelectedItem().toString();
        String condition = spinnerCondition.getSelectedItem().toString();
        String itemBehavior = Objects.requireNonNull(etItemBehavior.getText()).toString().trim();
        List<String> tagsList = new ArrayList<>();
        if (!Objects.requireNonNull(etTags.getText()).toString().trim().isEmpty()) {
            tagsList = Arrays.asList(etTags.getText().toString().trim().split("\\s*,\\s*"));
        }

        com.example.tradeup.model.Location itemLocation = null;
        String locationText = Objects.requireNonNull(etLocation.getText()).toString().trim();

        if (!locationText.isEmpty()) {
            itemLocation = new com.example.tradeup.model.Location(this.currentLat, this.currentLng, locationText);
        } else {
            itemLocation = new com.example.tradeup.model.Location(0.0, 0.0, "Địa chỉ không xác định");
            Toast.makeText(requireContext(), "Vui lòng nhập hoặc lấy vị trí!", Toast.LENGTH_SHORT).show();
            return;
        }

        String itemId = UUID.randomUUID().toString();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        String currentTime = sdf.format(new Date());

        Item newItem = new Item(
                itemId,
                userId,
                title,
                description,
                price,
                category,
                condition,
                "Available",
                itemLocation,
                uploadedImageUrls,
                itemBehavior.isEmpty() ? null : itemBehavior,
                tagsList.isEmpty() ? null : tagsList,
                currentTime,
                currentTime,
                0L,
                0L,
                0.0
        );

        firebaseHelper.addItem(itemId, newItem, new FirebaseHelper.DbWriteCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), "Tin đăng đã được đăng thành công! ", Toast.LENGTH_LONG).show();
                navController.navigate(R.id.action_addItemFragment_to_myItemsFragment);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(requireContext(), "Đăng tin thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
                Log.e("AddItemFragment", "Failed to add item: " + errorMessage);
            }
        });
    }
}
