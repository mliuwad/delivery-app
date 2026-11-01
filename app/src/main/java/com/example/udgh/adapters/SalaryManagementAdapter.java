package com.example.udgh.adapters;

import com.example.udgh.activities.*;
import com.example.udgh.fragments.*;
import com.example.udgh.utils.*;
import com.example.udgh.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.udgh.R;
import com.example.udgh.repositories.SalaryManagementRepository;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class SalaryManagementAdapter extends RecyclerView.Adapter<SalaryManagementAdapter.SalaryViewHolder> {
    public interface OnSalaryClickListener {
        void onDetailClick(SalaryManagementRepository.SalaryRow row);
    }

    private final List<SalaryManagementRepository.SalaryRow> items;
    private final OnSalaryClickListener listener;

    public SalaryManagementAdapter(List<SalaryManagementRepository.SalaryRow> items,
                                   OnSalaryClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SalaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_salary_shipper, parent, false);
        return new SalaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SalaryViewHolder holder, int position) {
        SalaryManagementRepository.SalaryRow row = items.get(position);
        String name = row.user.getFullName();
        holder.txtName.setText(name == null || name.trim().isEmpty() ? "Không tên" : name);
        holder.txtCompleted.setText("Đơn hoàn thành: " + row.totalOrders);
        holder.txtSalary.setText(formatCurrency(row.totalSalary));

        boolean paid = "PAID".equals(row.paymentStatus);
        holder.txtPaymentStatus.setText(paid ? "Đã thanh toán" : "Chưa thanh toán");
        holder.txtPaymentStatus.setTextColor(holder.itemView.getContext().getColor(
                paid ? R.color.success : R.color.warning));

        String avatarUrl = row.user.getAvatarUrl();
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            holder.imgAvatar.setImageResource(R.drawable.delivery_man);
        } else {
            Glide.with(holder.itemView.getContext())
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.delivery_man)
                    .error(R.drawable.delivery_man)
                    .into(holder.imgAvatar);
        }

        holder.btnDetail.setOnClickListener(v -> {
            if (listener != null) listener.onDetailClick(row);
        });
    }

    private String formatCurrency(double amount) {
        return NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(amount);
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class SalaryViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgAvatar;
        final TextView txtName, txtCompleted, txtSalary, txtPaymentStatus;
        final MaterialButton btnDetail;

        SalaryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            txtName = itemView.findViewById(R.id.txtName);
            txtCompleted = itemView.findViewById(R.id.txtCompleted);
            txtSalary = itemView.findViewById(R.id.txtSalary);
            txtPaymentStatus = itemView.findViewById(R.id.txtPaymentStatus);
            btnDetail = itemView.findViewById(R.id.btnDetail);
        }
    }
}
