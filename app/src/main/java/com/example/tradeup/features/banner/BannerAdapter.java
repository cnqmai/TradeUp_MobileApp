package com.example.tradeup.features.banner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;

import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {
    private List<BannerItem> bannerList;
    private Context context;

    public BannerAdapter(List<BannerItem> bannerList, Context context) {
        this.bannerList = bannerList;
        this.context = context;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        BannerItem item = bannerList.get(position);
        holder.button.setText(item.getButtonText());

        String imageUrl = item.getImageUrl();
        if (imageUrl != null && imageUrl.startsWith("http")) {
            // Ảnh từ Internet
            Glide.with(context)
                    .load(imageUrl)
                    .into(holder.image);
        } else if (imageUrl != null) {
            // Ảnh local drawable, lấy resource id từ tên
            int resId = context.getResources().getIdentifier(imageUrl, "drawable", context.getPackageName());
            if (resId != 0) {
                holder.image.setImageResource(resId);
            } else {
                // Nếu không tìm thấy resource, bạn có thể set ảnh mặc định hoặc để trống
                holder.image.setImageResource(R.drawable.placeholder_image);
            }
        } else {
            // imageUrl null => ảnh mặc định
            holder.image.setImageResource(R.drawable.placeholder_image);
        }
    }

    @Override
    public int getItemCount() {
        return bannerList.size();
    }

    public static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;
        Button button;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.bannerImage);
            button = itemView.findViewById(R.id.bannerButton);
        }
    }
}
