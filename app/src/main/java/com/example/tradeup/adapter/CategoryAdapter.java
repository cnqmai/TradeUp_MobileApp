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
    private String selectedCategory;

    public interface OnCategoryClickListener {
        void onCategoryClick(String category);
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public CategoryAdapter(Context context, List<String> categoryList) {
        this.context = context;
        this.categoryList = categoryList;
    }

    public void setCategories(List<String> newCategoryList) {
        this.categoryList.clear();
        this.categoryList.addAll(newCategoryList);
        notifyDataSetChanged();
    }

    public void setSelectedCategory(String category) {
        this.selectedCategory = category;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // You might want a simpler layout for categories, e.g., a TextView inside a CardView
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_chip, parent, false); // Create item_category_chip.xml
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = categoryList.get(position);
        holder.tvCategoryName.setText(category);

        // Highlight selected category
        holder.itemView.setSelected(category.equals(selectedCategory));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                String previousSelected = selectedCategory;
                selectedCategory = category;
                listener.onCategoryClick(category);

                // Update the views
                notifyItemChanged(categoryList.indexOf(previousSelected));
                notifyItemChanged(position);
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
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName); // Ensure this ID matches your item_category_chip.xml
        }
    }
}