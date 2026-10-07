package com.example.udgh;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.udgh.models.User;

import java.util.List;

public class ShipperAdapter extends RecyclerView.Adapter<ShipperAdapter.ViewHolder> {

    private List<User> shipperList;
    private Context context;
    private OnShipperActionListener listener;

    public interface OnShipperActionListener {
        void onLock(User shipper);
    }

    public ShipperAdapter(Context context, List<User> shipperList, OnShipperActionListener listener) {
        this.context = context;
        this.shipperList = shipperList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shipper, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User shipper = shipperList.get(position);

        holder.txtName.setText(shipper.getFullName());
        holder.txtPhone.setText("SĐT: " + shipper.getPhone());
        holder.txtEmail.setText("Email: " + shipper.getEmail());

        String status = shipper.getStatus();
        if ("ACTIVE".equals(status)) {
            holder.txtStatus.setText("Hoạt động");
            holder.txtStatus.setTextColor(context.getColor(android.R.color.holo_green_dark));
        } else {
            holder.txtStatus.setText("Khóa");
            holder.txtStatus.setTextColor(context.getColor(android.R.color.holo_red_dark));
        }

        String avatarUrl = shipper.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(context)
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.delivery_man)
                    .error(R.drawable.delivery_man)
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.delivery_man);
        }

        // Click vào item → mở chi tiết (chế độ xem)
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ShipperDetailActivity.class);
            intent.putExtra("shipperId", shipper.getUserId());
            context.startActivity(intent);
        });

        // Click nút Sửa → mở chi tiết với chế độ sửa
        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, ShipperDetailActivity.class);
            intent.putExtra("shipperId", shipper.getUserId());
            intent.putExtra("mode", "edit");
            context.startActivity(intent);
        });

        // Click nút Khóa/Mở khóa → gọi listener
        holder.btnLock.setOnClickListener(v -> {
            if (listener != null) listener.onLock(shipper);
        });
    }

    @Override
    public int getItemCount() {
        return shipperList != null ? shipperList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtName, txtPhone, txtEmail, txtStatus;
        ImageButton btnEdit, btnLock;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            txtName = itemView.findViewById(R.id.txtName);
            txtPhone = itemView.findViewById(R.id.txtPhone);
            txtEmail = itemView.findViewById(R.id.txtEmail);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnLock = itemView.findViewById(R.id.btnLock);
        }
    }
}