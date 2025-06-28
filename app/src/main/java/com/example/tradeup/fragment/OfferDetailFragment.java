// Trong file OfferDetailFragment.java của bạn

package com.example.tradeup.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tradeup.R;
import com.example.tradeup.model.Offer;
import com.example.tradeup.model.Transaction; // Thêm import này cho model Transaction
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
// import java.util.HashMap; // Không cần HashMap nữa
import java.util.Locale;
// import java.util.Map; // Không cần Map nữa
import java.util.Objects;

public class OfferDetailFragment extends Fragment {

    private static final String TAG = "OfferDetailFragment";

    private TextView tvOfferAmount, tvBuyerName, tvOfferStatus, tvItemInfo;
    private Button btnAccept, btnReject, btnCounter;

    private String offerId;
    private DatabaseReference offerRef;
    private Offer currentOffer;
    private String currentUserId;

    public OfferDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        if (getArguments() != null) {
            offerId = getArguments().getString("offerId");
            if (offerId != null) {
                offerRef = FirebaseDatabase.getInstance().getReference("offers").child(offerId);
            } else {
                Log.e(TAG, "Offer ID is null in arguments.");
            }
        } else {
            Log.e(TAG, "Arguments are null.");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_offer_detail, container, false);
        tvOfferAmount = view.findViewById(R.id.tv_offer_amount);
        tvBuyerName = view.findViewById(R.id.tv_buyer_name);
        tvOfferStatus = view.findViewById(R.id.tv_offer_status);
        tvItemInfo = view.findViewById(R.id.tv_item_info);
        btnAccept = view.findViewById(R.id.btn_accept_offer);
        btnReject = view.findViewById(R.id.btn_reject_offer);
        btnCounter = view.findViewById(R.id.btn_counter_offer);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (offerRef != null) {
            fetchOfferDetails();
        } else {
            Toast.makeText(requireContext(), "Lỗi: Không thể tải chi tiết đề nghị.", Toast.LENGTH_SHORT).show();
        }

        btnAccept.setOnClickListener(v -> {
            if (currentOffer != null && currentOffer.getStatus().equals("pending") && currentOffer.getSeller_id().equals(currentUserId)) {
                handleOfferAction("accepted");
            } else {
                Toast.makeText(requireContext(), "Không thể chấp nhận đề nghị này.", Toast.LENGTH_SHORT).show();
            }
        });
        btnReject.setOnClickListener(v -> {
            if (currentOffer != null && currentOffer.getStatus().equals("pending") && currentOffer.getSeller_id().equals(currentUserId)) {
                handleOfferAction("rejected");
            } else {
                Toast.makeText(requireContext(), "Không thể từ chối đề nghị này.", Toast.LENGTH_SHORT).show();
            }
        });
        btnCounter.setOnClickListener(v -> {
            if (currentOffer != null && currentOffer.getStatus().equals("pending") && currentOffer.getSeller_id().equals(currentUserId)) {
                // Triển khai logic trả giá (mở dialog hoặc fragment mới để nhập giá)
                Toast.makeText(requireContext(), "Chức năng trả giá sắp ra mắt.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Không thể trả giá đề nghị này.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchOfferDetails() {
        offerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentOffer = snapshot.getValue(Offer.class);
                if (currentOffer != null) {
                    tvOfferAmount.setText("Số tiền đề nghị: " + (currentOffer.getOffer_price() != null ? String.valueOf(currentOffer.getOffer_price()) : "N/A"));
                    tvOfferStatus.setText("Trạng thái: " + (currentOffer.getStatus() != null ? currentOffer.getStatus() : "N/A"));
                    tvItemInfo.setText("ID Tin đăng: " + (currentOffer.getItem_id() != null ? currentOffer.getItem_id() : "N/A"));

                    tvBuyerName.setText("ID Người mua: " + (currentOffer.getBuyer_id() != null ? currentOffer.getBuyer_id() : "N/A"));

                    updateButtonState(currentOffer.getStatus(), currentOffer.getSeller_id());

                } else {
                    Toast.makeText(requireContext(), "Không tìm thấy chi tiết đề nghị.", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Offer data is null for ID: " + offerId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Lỗi tải đề nghị: " + error.getMessage());
                Toast.makeText(requireContext(), "Lỗi tải đề nghị: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateButtonState(String status, String sellerId) {
        boolean isSeller = currentUserId.equals(sellerId);
        boolean canInteract = isSeller && "pending".equals(status);

        btnAccept.setEnabled(canInteract);
        btnReject.setEnabled(canInteract);
        btnCounter.setEnabled(canInteract);
    }

    private void handleOfferAction(String action) {
        if (offerRef == null || currentOffer == null) {
            Toast.makeText(requireContext(), "Lỗi: Không thể thực hiện hành động.", Toast.LENGTH_SHORT).show();
            return;
        }

        offerRef.child("status").setValue(action)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Đề nghị đã được " + action + ".", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Offer " + offerId + " status updated to " + action);

                    if ("accepted".equals(action)) {
                        markItemAsSold(currentOffer.getItem_id());
                        createOrUpdateTransactionRecord(currentOffer);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi cập nhật đề nghị " + offerId + ": " + e.getMessage());
                    Toast.makeText(requireContext(), "Lỗi khi cập nhật đề nghị: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void markItemAsSold(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            Log.e(TAG, "Cannot mark item as sold: item ID is null or empty.");
            return;
        }

        DatabaseReference itemRef = FirebaseDatabase.getInstance().getReference("items").child(itemId);
        itemRef.child("status").setValue("sold")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Item " + itemId + " marked as sold.");
                    Toast.makeText(requireContext(), "Tin đăng đã được đánh dấu là 'Đã bán'.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to mark item " + itemId + " as sold: " + e.getMessage());
                    Toast.makeText(requireContext(), "Lỗi khi đánh dấu tin đăng là 'Đã bán'.", Toast.LENGTH_SHORT).show();
                });
    }

    // FR-5.2.2: Archive sold items in user history.
    // Đã cập nhật để sử dụng model Transaction
    private void createOrUpdateTransactionRecord(Offer offer) {
        if (offer == null || offerId == null) {
            Log.e(TAG, "Cannot create transaction record: offer or offerId is null.");
            return;
        }

        DatabaseReference transactionsRef = FirebaseDatabase.getInstance().getReference("transactions");
        String transactionId = transactionsRef.push().getKey();

        // Tạo đối tượng Transaction sử dụng model của bạn
        Transaction transaction = new Transaction();
        transaction.setItem_id(offer.getItem_id());
        transaction.setBuyer_id(offer.getBuyer_id());
        transaction.setSeller_id(offer.getSeller_id());
        transaction.setFinal_price(offer.getOffer_price());
        transaction.setOffer_id(offerId);
        transaction.setTransaction_date(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));
        transaction.setArchived(false); // Mặc định là chưa lưu trữ

        if (transactionId != null) {
            transactionsRef.child(transactionId).setValue(transaction) // Ghi đối tượng Transaction vào Firebase
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Transaction record created: " + transactionId);
                        Toast.makeText(requireContext(), "Giao dịch đã được ghi nhận.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to create transaction record: " + e.getMessage());
                        Toast.makeText(requireContext(), "Lỗi khi ghi nhận giao dịch.", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}