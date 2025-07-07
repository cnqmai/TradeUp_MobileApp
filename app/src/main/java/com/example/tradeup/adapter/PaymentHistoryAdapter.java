package com.example.tradeup.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.R;
import com.example.tradeup.model.Payment;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Date;

public class PaymentHistoryAdapter extends RecyclerView.Adapter<PaymentHistoryAdapter.PaymentViewHolder> {

    private static final String TAG = "PaymentHistoryAdapter";
    private List<Payment> paymentList;
    private Context context;
    private String currentUserId; // To determine if the user is payer or payee

    public PaymentHistoryAdapter(Context context, List<Payment> paymentList, String currentUserId) {
        this.context = context;
        this.paymentList = paymentList;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment_history, parent, false);
        return new PaymentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        Payment payment = paymentList.get(position);

        // Set payment method and icon
        holder.tvPaymentMethod.setText(payment.getMethod());
        switch (payment.getMethod().toLowerCase(Locale.ROOT)) {
            case "credit card":
            case "debit card":
                holder.ivPaymentMethodIcon.setImageResource(R.drawable.ic_credit_card);
                break;
            case "upi":
            case "ví": // Assuming 'Ví' refers to a wallet
                holder.ivPaymentMethodIcon.setImageResource(R.drawable.ic_wallet); // You need to have ic_upi drawable
                break;
            case "cash":
                holder.ivPaymentMethodIcon.setImageResource(R.drawable.ic_attach_money); // You need to have ic_cash drawable
                break;
            default:
                holder.ivPaymentMethodIcon.setImageResource(R.drawable.ic_credit_card); // Default icon
                break;
        }

        // Format amount
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String formattedAmount = currencyFormat.format(payment.getAmount());

        // Determine if user is payer or payee and set amount color/sign
        if (currentUserId != null && currentUserId.equals(payment.getBuyer_id())) {
            // User is the payer (money going out)
            holder.tvPaymentAmount.setText("-" + formattedAmount);
            holder.tvPaymentAmount.setTextColor(Color.RED);
        } else if (currentUserId != null && currentUserId.equals(payment.getSeller_id())) {
            // User is the payee (money coming in)
            holder.tvPaymentAmount.setText("+" + formattedAmount);
            holder.tvPaymentAmount.setTextColor(context.getResources().getColor(R.color.green_bold, null));
        } else {
            // Fallback if currentUserId is not payer or payee (shouldn't happen in normal flow)
            holder.tvPaymentAmount.setText(formattedAmount);
            holder.tvPaymentAmount.setTextColor(context.getResources().getColor(R.color.black, null));
        }


        // Set status
        holder.tvPaymentStatus.setText("Trạng thái: " + payment.getStatus());
        // You can add color logic based on status if needed (e.g., green for completed, red for failed)

        // Format timestamp
        try {
            SimpleDateFormat firebaseDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            Date date = firebaseDateFormat.parse(payment.getTimestamp());
            SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.tvPaymentDate.setText("Ngày: " + displayDateFormat.format(date));
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing payment timestamp: " + e.getMessage());
            holder.tvPaymentDate.setText("Ngày: N/A");
        }

        holder.tvTransactionId.setText("Mã giao dịch: " + payment.getTransaction_id());

        // Show escrow status if enabled
        Boolean isEscrowEnabled = payment.getIs_escrow();

        if (isEscrowEnabled != null && isEscrowEnabled) { // Dòng 106 mới sẽ là dòng này hoặc tương tự
            holder.tvEscrowStatus.setVisibility(View.VISIBLE);
            holder.tvEscrowStatus.setText("Ký quỹ: " + payment.getEscrow_status());
        } else {
            // Nếu isEscrowEnabled là null hoặc false, ẩn View đi
            holder.tvEscrowStatus.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return paymentList.size();
    }

    public void updatePaymentList(List<Payment> newList) {
        this.paymentList.clear();
        this.paymentList.addAll(newList);
        notifyDataSetChanged();
    }

    static class PaymentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPaymentMethodIcon;
        TextView tvPaymentMethod, tvPaymentAmount, tvPaymentStatus, tvPaymentDate, tvTransactionId, tvEscrowStatus;

        public PaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPaymentMethodIcon = itemView.findViewById(R.id.iv_payment_method_icon);
            tvPaymentMethod = itemView.findViewById(R.id.tv_payment_method);
            tvPaymentAmount = itemView.findViewById(R.id.tv_payment_amount);
            tvPaymentStatus = itemView.findViewById(R.id.tv_payment_status);
            tvPaymentDate = itemView.findViewById(R.id.tv_payment_date);
            tvTransactionId = itemView.findViewById(R.id.tv_transaction_id);
            tvEscrowStatus = itemView.findViewById(R.id.tv_escrow_status);
        }
    }
}
