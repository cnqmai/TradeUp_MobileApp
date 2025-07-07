package com.example.tradeup.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.R;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private Context context;
    private List<String> categoryList;
    private OnCategoryClickListener listener;
    private String selectedCategory; // Để theo dõi danh mục được chọn

    // Interface cho sự kiện click danh mục
    public interface OnCategoryClickListener {
        void onCategoryClick(String category);
    }

    // Setter cho listener
    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public CategoryAdapter(Context context, List<String> categoryList) {
        this.context = context;
        this.categoryList = categoryList;
        // Khởi tạo selectedCategory nếu cần một giá trị mặc định, ví dụ: categoryList.get(0)
        // Hoặc để null và không có danh mục nào được chọn ban đầu.
        this.selectedCategory = null; // hoặc categoryList.isEmpty() ? null : categoryList.get(0);
    }

    // Phương thức để cập nhật danh sách danh mục
    public void setCategories(List<String> newCategoryList) {
        this.categoryList.clear();
        this.categoryList.addAll(newCategoryList);
        notifyDataSetChanged();
    }

    // Phương thức để đặt danh mục được chọn và cập nhật UI
    public void setSelectedCategory(String category) {
        if (!category.equals(this.selectedCategory)) {
            String previousSelected = this.selectedCategory;
            this.selectedCategory = category;

            // Tìm vị trí của danh mục đã chọn trước đó và danh mục mới để cập nhật hiệu quả hơn
            int previousPos = -1;
            if (previousSelected != null) {
                previousPos = categoryList.indexOf(previousSelected);
            }
            int newPos = categoryList.indexOf(selectedCategory);

            if (previousPos != -1) {
                notifyItemChanged(previousPos);
            }
            if (newPos != -1) {
                notifyItemChanged(newPos);
            }
        }
    }


    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Đảm bảo bạn đã có item_category_chip.xml trong thư mục res/layout
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_chip, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = categoryList.get(position);
        holder.tvCategoryName.setText(category);

        // Đánh dấu danh mục được chọn (nếu bạn có style cho trạng thái "selected")
        // Bạn cần định nghĩa `android:state_selected="true"` trong selector drawable cho background của item.
        holder.itemView.setSelected(category.equals(selectedCategory));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                // THAY ĐỔI: Sử dụng setSelectedCategory để cập nhật và thông báo thay đổi
                setSelectedCategory(category);
                listener.onCategoryClick(category); // Gọi listener
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            // Đảm bảo ID này khớp với TextView trong item_category_chip.xml của bạn
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
        }
    }
}