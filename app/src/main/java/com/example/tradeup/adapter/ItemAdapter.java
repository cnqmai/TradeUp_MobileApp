package com.example.tradeup.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.model.Item; // Đảm bảo import đúng class Item của bạn

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    private Context context;
    private List<Item> itemList;
    private OnItemClickListener listener; // Thêm listener

    // Interface để xử lý sự kiện click vào item
    public interface OnItemClickListener {
        void onItemClick(String itemId);
    }

    // Setter cho listener
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public ItemAdapter(Context context, List<Item> itemList) {
        this.context = context;
        this.itemList = itemList;
    }

    public void setItems(List<Item> newItems) {
        this.itemList = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_list_layout, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = itemList.get(position);

        holder.tvTitle.setText(item.getTitle());

        // Định dạng giá sản phẩm
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        currencyFormat.setMaximumFractionDigits(0);
        String formattedPrice = currencyFormat.format(item.getPrice());
        holder.tvPrice.setText("Giá: " + formattedPrice);

        // Lấy và đặt địa chỉ từ đối tượng Location
        if (item.getLocation() != null && item.getLocation().getManual_address() != null) {
            holder.tvLocation.setText("Vị trí: " + item.getLocation().getManual_address());
        } else {
            holder.tvLocation.setText("Vị trí: Không xác định");
        }

        // Tải ảnh sản phẩm bằng Glide
        if (item.getPhotos() != null && !item.getPhotos().isEmpty()) {
            Glide.with(context)
                    .load(item.getPhotos().get(0)) // Tải ảnh đầu tiên từ danh sách photos
                    .placeholder(R.drawable.img_placeholder)
                    .error(R.drawable.img_placeholder)
                    .centerCrop()
                    .into(holder.ivItemImage);
        } else {
            holder.ivItemImage.setImageResource(R.drawable.img_placeholder);
        }

        // Thêm OnClickListener cho item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && item.getId() != null) {
                listener.onItemClick(item.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItemImage;
        TextView tvTitle;
        TextView tvPrice;
        TextView tvLocation;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemImage = itemView.findViewById(R.id.iv_item_image);
            tvTitle = itemView.findViewById(R.id.tv_item_title);
            tvPrice = itemView.findViewById(R.id.tv_item_price);
            tvLocation = itemView.findViewById(R.id.tv_item_location);
        }
    }
}