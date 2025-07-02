package com.example.tradeup.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.R;
import com.example.tradeup.adapter.PaymentHistoryAdapter;
import com.example.tradeup.model.Payment;
import com.example.tradeup.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PaymentHistoryFragment extends Fragment {

    private static final String TAG = "PaymentHistoryFragment";

    private RecyclerView recyclerView;
    private PaymentHistoryAdapter adapter;
    private TextView textNoPayments;
    private ImageView ivBackButton;
    private NavController navController;

    private FirebaseHelper firebaseHelper;
    private String currentUserId;
    private DatabaseReference paymentHistoryRef;
    private ValueEventListener paymentHistoryListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = new FirebaseHelper(requireContext());
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        if (currentUserId == null) {
            Log.e(TAG, "Current user ID is null. Cannot load payment history.");
            if (isAdded()) {
                Toast.makeText(requireContext(), "Lỗi: Không tìm thấy người dùng hiện tại.", Toast.LENGTH_SHORT).show();
                // Optionally navigate back to login
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment_history, container, false);

        recyclerView = view.findViewById(R.id.recycler_payment_history);
        textNoPayments = view.findViewById(R.id.text_no_payments);
        ivBackButton = view.findViewById(R.id.iv_back_button_payment_history);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PaymentHistoryAdapter(requireContext(), new ArrayList<>(), currentUserId);
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        ivBackButton.setOnClickListener(v -> {
            if (navController != null) {
                navController.popBackStack();
            }
        });

        loadPaymentHistory();
    }

    private void loadPaymentHistory() {
        if (currentUserId == null) {
            textNoPayments.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        paymentHistoryRef = FirebaseDatabase.getInstance().getReference("payment_history").child(currentUserId);
        paymentHistoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> paymentIds = new ArrayList<>();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    // Assuming the structure is payment_history -> userId -> paymentId: true
                    paymentIds.add(childSnapshot.getKey());
                }
                Log.d(TAG, "Fetched payment IDs: " + paymentIds.size());
                fetchPaymentDetails(paymentIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load payment history IDs: " + error.getMessage());
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Lỗi tải lịch sử thanh toán: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
                textNoPayments.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        };
        paymentHistoryRef.addValueEventListener(paymentHistoryListener);
    }

    private void fetchPaymentDetails(List<String> paymentIds) {
        if (paymentIds.isEmpty()) {
            textNoPayments.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            adapter.updatePaymentList(new ArrayList<>()); // Clear adapter
            return;
        }

        List<Payment> fetchedPayments = new ArrayList<>();
        DatabaseReference paymentsRef = FirebaseDatabase.getInstance().getReference("payments");

        // Use a counter to know when all payments are fetched
        final int[] paymentsToFetch = {paymentIds.size()};

        for (String paymentId : paymentIds) {
            paymentsRef.child(paymentId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Payment payment = snapshot.getValue(Payment.class);
                        if (payment != null) {
                            payment.setPayment_id(snapshot.getKey()); // Set the ID from the key
                            fetchedPayments.add(payment);
                        }
                    }
                    paymentsToFetch[0]--;
                    if (paymentsToFetch[0] == 0) {
                        // All payments fetched, sort and update UI
                        Collections.sort(fetchedPayments, (p1, p2) -> {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                                Date date1 = sdf.parse(p1.getTimestamp());
                                Date date2 = sdf.parse(p2.getTimestamp());
                                return date2.compareTo(date1); // Sort descending by date
                            } catch (ParseException e) {
                                Log.e(TAG, "Error parsing date for sorting: " + e.getMessage());
                                return 0;
                            }
                        });
                        adapter.updatePaymentList(fetchedPayments);
                        textNoPayments.setVisibility(fetchedPayments.isEmpty() ? View.VISIBLE : View.GONE);
                        recyclerView.setVisibility(fetchedPayments.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to fetch payment details for ID " + paymentId + ": " + error.getMessage());
                    paymentsToFetch[0]--; // Decrement even on error
                    if (paymentsToFetch[0] == 0) {
                        // All payments processed (even if some failed), update UI
                        adapter.updatePaymentList(fetchedPayments);
                        textNoPayments.setVisibility(fetchedPayments.isEmpty() ? View.VISIBLE : View.GONE);
                        recyclerView.setVisibility(fetchedPayments.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (paymentHistoryRef != null && paymentHistoryListener != null) {
            paymentHistoryRef.removeEventListener(paymentHistoryListener);
            Log.d(TAG, "Payment history Firebase listener removed.");
        }
    }
}