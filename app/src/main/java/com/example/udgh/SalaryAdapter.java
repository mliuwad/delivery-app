package com.example.udgh;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.udgh.models.Salary;
import com.example.udgh.models.User;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class SalaryAdapter extends RecyclerView.Adapter<SalaryAdapter.ViewHolder> {

    private List<Salary> salaryList;
    private List<User> userList;
    private OnSalaryActionListener listener;

    public interface OnSalaryActionListener {
        void onDetailClick(Salary salary);
    }

    public SalaryAdapter(List<Salary> salaryList, List<User> userList, OnSalaryActionListener listener) {
        this.salaryList = salaryList;
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_salary_shipper, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Salary salary = salaryList.get(position);
        // Tìm user tương ứng
        User user = findUserByShipperId(salary.getShipperId());

        if (user != null) {
            holder.txtName.setText(user.getFullName());
            // Avatar
            String avatarUrl = user.getAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(avatarUrl)
                        .circleCrop()
                        .placeholder(R.drawable.delivery_man)
                        .error(R.drawable.delivery_man)
                        .into(holder.imgAvatar);
            } else {
                holder.imgAvatar.setImageResource(R.drawable.delivery_man);
            }
        } else {
            holder.txtName.setText("Không xác định");
            holder.imgAvatar.setImageResource(R.drawable.delivery_man);
        }

        holder.txtCompleted.setText("Đơn hoàn thành: " + salary.getTotalOrders());
        holder.txtSalary.setText(formatCurrency((int) salary.getTotalSalary()));

        String status = salary.getPaymentStatus();
        if ("PAID".equals(status)) {
            holder.txtPaymentStatus.setText("Đã thanh toán");
            holder.txtPaymentStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_green_dark));
        } else {
            holder.txtPaymentStatus.setText("Chưa thanh toán");
            holder.txtPaymentStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_orange_dark));
        }

        holder.btnDetail.setOnClickListener(v -> {
            if (listener != null) listener.onDetailClick(salary);
        });
    }

    private User findUserByShipperId(String shipperId) {
        for (User user : userList) {
            if (user.getUserId().equals(shipperId)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return salaryList != null ? salaryList.size() : 0;
    }

    private String formatCurrency(int price) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return format.format(price);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtName, txtCompleted, txtSalary, txtPaymentStatus;
        TextView btnDetail; // dùng TextView với style button, hoặc MaterialButton

        public ViewHolder(@NonNull View itemView) {
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