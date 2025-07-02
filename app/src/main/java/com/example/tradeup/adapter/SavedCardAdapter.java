package com.example.tradeup.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.R;
import com.example.tradeup.model.SavedCard;

import java.util.List;
import java.util.Locale;

public class SavedCardAdapter extends RecyclerView.Adapter<SavedCardAdapter.SavedCardViewHolder> {

    private Context context;
    private List<SavedCard> savedCardList;

    public SavedCardAdapter(Context context, List<SavedCard> savedCardList) {
        this.context = context;
        this.savedCardList = savedCardList;
    }

    @NonNull
    @Override
    public SavedCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_card, parent, false);
        return new SavedCardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SavedCardViewHolder holder, int position) {
        SavedCard card = savedCardList.get(position);

        holder.tvCardNumber.setText("**** **** **** " + card.getLast4()); // Use getLast4()
        holder.tvCardExpiry.setText(card.getExpiry_month() + "/" + card.getExpiry_year().substring(2)); // Display YY
        holder.tvCardHolderName.setText(card.getCard_holder_name());

        // Set card icon based on card brand
        String cardBrand = card.getBrand(); // FIX: Use getBrand()
        if (cardBrand != null) {
            switch (cardBrand.toLowerCase(Locale.getDefault())) {
                case "visa":
                    holder.ivCardTypeIcon.setImageResource(R.drawable.ic_visa_logo);
                    break;
                case "mastercard":
                    holder.ivCardTypeIcon.setImageResource(R.drawable.ic_mastercard_logo);
                    break;
                // Add more cases for other card brands (e.g., "amex", "discover")
                default:
                    holder.ivCardTypeIcon.setImageResource(R.drawable.ic_credit_card); // Default icon
                    break;
            }
        } else {
            holder.ivCardTypeIcon.setImageResource(R.drawable.ic_credit_card); // Default icon if cardBrand is null
        }

        // Show/hide default badge
        if (card.getIs_default()) { // FIX: Use getIs_default()
            holder.tvDefaultBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvDefaultBadge.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return savedCardList.size();
    }

    public void updateSavedCardList(List<SavedCard> newList) {
        this.savedCardList.clear();
        this.savedCardList.addAll(newList);
        notifyDataSetChanged();
    }

    static class SavedCardViewHolder extends RecyclerView.ViewHolder {
        TextView tvCardNumber, tvCardExpiry, tvCardHolderName, tvDefaultBadge;
        ImageView ivCardTypeIcon;

        public SavedCardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCardNumber = itemView.findViewById(R.id.tv_card_number);
            tvCardExpiry = itemView.findViewById(R.id.tv_card_expiry);
            tvCardHolderName = itemView.findViewById(R.id.tv_card_holder_name);
            tvDefaultBadge = itemView.findViewById(R.id.tv_default_badge);
            ivCardTypeIcon = itemView.findViewById(R.id.iv_card_type_icon);
        }
    }
}
