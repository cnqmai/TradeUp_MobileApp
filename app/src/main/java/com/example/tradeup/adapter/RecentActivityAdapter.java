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
import com.example.tradeup.model.ActivityLog;
import com.example.tradeup.utils.AppDateUtils; // Assuming you have this utility

import java.util.List;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ActivityViewHolder> {

    private final Context context;
    private List<ActivityLog> activityList;
    private OnActivityClickListener listener;

    public interface OnActivityClickListener {
        void onActivityClick(ActivityLog activity);
    }

    public RecentActivityAdapter(Context context, List<ActivityLog> activityList, OnActivityClickListener listener) {
        this.context = context;
        this.activityList = activityList;
        this.listener = listener;
    }

    public void updateActivityList(List<ActivityLog> newActivityList) {
        this.activityList = newActivityList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        ActivityLog activity = activityList.get(position);
        holder.bind(activity, context, listener);
    }

    @Override
    public int getItemCount() {
        return activityList.size();
    }

    public static class ActivityViewHolder extends RecyclerView.ViewHolder {
        ImageView ivActivityIcon;
        TextView tvActivityTitle, tvActivityDescription, tvActivityTime;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            ivActivityIcon = itemView.findViewById(R.id.iv_activity_icon);
            tvActivityTitle = itemView.findViewById(R.id.tv_activity_title);
            tvActivityDescription = itemView.findViewById(R.id.tv_activity_description);
            tvActivityTime = itemView.findViewById(R.id.tv_activity_time);
        }

        public void bind(final ActivityLog activity, final Context context, final OnActivityClickListener listener) {
            tvActivityTitle.setText(getActivityTitle(activity.getType(), context));
            tvActivityDescription.setText(getActivityDescription(activity, context));
            ivActivityIcon.setImageResource(getIconForActivityType(activity.getType()));

            // SỬA ĐỔI TẠI ĐÂY: Dùng getTimestamp()
            if (activity.getTimestamp() != null) {
                tvActivityTime.setText(AppDateUtils.getTimeAgo(activity.getTimestamp()));
            } else {
                tvActivityTime.setText("");
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onActivityClick(activity);
                }
            });
        }

        private String getActivityTitle(String type, Context context) {
            if (type == null) return context.getString(R.string.activity_type_unknown);
            switch (type) {
                case "new_user":
                    return context.getString(R.string.activity_type_new_user);
                case "new_item":
                    return context.getString(R.string.activity_type_new_item);
                case "new_offer":
                    return context.getString(R.string.activity_type_new_offer);
                case "transaction_completed":
                    return context.getString(R.string.activity_type_transaction_completed);
                case "report_filed":
                    return context.getString(R.string.activity_type_report_filed);
                case "offer_accepted":
                    return context.getString(R.string.activity_type_offer_accepted);
                case "offer_rejected":
                    return context.getString(R.string.activity_type_offer_rejected);
                case "item_updated":
                    return context.getString(R.string.activity_type_item_updated);
                case "user_banned":
                    return context.getString(R.string.activity_type_user_banned);
                case "user_unbanned":
                    return context.getString(R.string.activity_type_user_unbanned);
                default:
                    return context.getString(R.string.activity_type_unknown);
            }
        }

        private String getActivityDescription(ActivityLog activity, Context context) {
            // SỬA ĐỔI TẠI ĐÂY: Sử dụng các ID và hiển thị chúng, hoặc bạn sẽ cần tra cứu bất đồng bộ
            // Để đơn giản, tôi sẽ hiển thị ID hoặc một placeholder.
            // Nếu bạn muốn tên thật, bạn sẽ cần FirebaseHelper ở đây và logic bất đồng bộ.
            String primaryActorId = activity.getPrimary_actor_id() != null ? activity.getPrimary_actor_id() : "Unknown ID";
            String secondaryObjectId = activity.getSecondary_object_id() != null ? activity.getSecondary_object_id() : "Unknown ID";
            String details = activity.getDetails() != null ? activity.getDetails() : "";

            if (activity.getType() == null) return context.getString(R.string.activity_description_unknown);

            switch (activity.getType()) {
                case "new_user":
                    return String.format(context.getString(R.string.activity_description_new_user_id), primaryActorId);
                case "new_item":
                    return String.format(context.getString(R.string.activity_description_new_item_id), primaryActorId, secondaryObjectId);
                case "new_offer":
                    return String.format(context.getString(R.string.activity_description_new_offer_id), primaryActorId, secondaryObjectId);
                case "transaction_completed":
                    return String.format(context.getString(R.string.activity_description_transaction_completed_id), secondaryObjectId);
                case "report_filed":
                    return String.format(context.getString(R.string.activity_description_report_filed_id), primaryActorId, secondaryObjectId, details);
                case "offer_accepted":
                    return String.format(context.getString(R.string.activity_description_offer_accepted_id), primaryActorId, secondaryObjectId);
                case "offer_rejected":
                    return String.format(context.getString(R.string.activity_description_offer_rejected_id), primaryActorId, secondaryObjectId);
                case "item_updated":
                    return String.format(context.getString(R.string.activity_description_item_updated_id), primaryActorId, secondaryObjectId);
                case "user_banned":
                    return String.format(context.getString(R.string.activity_description_user_banned_id), primaryActorId);
                case "user_unbanned":
                    return String.format(context.getString(R.string.activity_description_user_unbanned_id), primaryActorId);
                default:
                    return context.getString(R.string.activity_description_unknown);
            }
        }

        private int getIconForActivityType(String type) {
            if (type == null) return R.drawable.ic_notification;

            switch (type) {
                case "new_user":
                    return R.drawable.ic_new_user;
                case "new_item":
                    return R.drawable.ic_add_item;
                case "new_offer":
                    return R.drawable.ic_attach_money;
                case "transaction_completed":
                    return R.drawable.ic_check_circle;
                case "report_filed":
                    return R.drawable.ic_report;
                case "offer_accepted":
                    return R.drawable.ic_check_circle;
                case "offer_rejected":
                    return R.drawable.ic_cancel_circle;
                case "item_updated":
                    return R.drawable.ic_edit;
                case "user_banned":
                    return R.drawable.ic_ban_user;
                case "user_unbanned":
                    return R.drawable.ic_unban_user;
                default:
                    return R.drawable.ic_notification;
            }
        }
    }
}
