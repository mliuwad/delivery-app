package com.example.udgh.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.bumptech.glide.Glide;
import com.example.udgh.R;
import com.example.udgh.utils.RoleAccess;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class CreatorDetailActivity extends AppCompatActivity {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String creatorId;
    private boolean viewOnly, editing;
    private EditText name, phone, email;
    private TextView heading, status;
    private ImageView avatar;
    private MaterialButton edit, lock;
    private String currentStatus;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_creator_detail);
        RoleAccess.requireRole(this, "ADMIN", this::init);
    }

    private void init() {
        creatorId = getIntent().getStringExtra("creatorId");
        if (TextUtils.isEmpty(creatorId)) { finish(); return; }
        viewOnly = !"edit".equals(getIntent().getStringExtra("mode"));
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(viewOnly ? "Chi tiết nhân viên tạo đơn" : "Sửa nhân viên tạo đơn");
        toolbar.setNavigationOnClickListener(v -> finish());
        name = findViewById(R.id.txtFullName);
        phone = findViewById(R.id.txtPhone);
        email = findViewById(R.id.txtEmail);
        heading = findViewById(R.id.txtName);
        status = findViewById(R.id.txtStatus);
        avatar = findViewById(R.id.imgAvatar);
        edit = findViewById(R.id.btnEdit);
        lock = findViewById(R.id.btnLock);
        edit.setVisibility(viewOnly ? View.GONE : View.VISIBLE);
        lock.setVisibility(viewOnly ? View.GONE : View.VISIBLE);
        email.setEnabled(false); // Email belongs to Firebase Auth; cannot be edited by updating Firestore alone.
        avatar.setEnabled(false);
        edit.setOnClickListener(v -> { if (editing) save(); else enableEditing(); });
        lock.setOnClickListener(v -> toggle());
        load();
    }

    private void load() {
        db.collection("USERS").document(creatorId).get().addOnSuccessListener(doc -> {
            if (!doc.exists() || !"CREATOR".equalsIgnoreCase(doc.getString("role"))) {
                Toast.makeText(this, "Không tìm thấy nhân viên tạo đơn", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            String fullName=doc.getString("fullName"), mobile=doc.getString("phone");
            name.setText(fullName); phone.setText(mobile); email.setText(doc.getString("email"));
            heading.setText(fullName);
            currentStatus=doc.getString("status");
            boolean active="ACTIVE".equalsIgnoreCase(currentStatus);
            status.setText(active ? "Hoạt động" : "Đã khóa");
            status.setTextColor(getColor(active ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
            lock.setText(active ? "Khóa" : "Mở khóa");
            String url=doc.getString("avatarUrl");
            if (!TextUtils.isEmpty(url)) Glide.with(this).load(url).circleCrop()
                    .placeholder(R.drawable.delivery_man).into(avatar);
            else avatar.setImageResource(R.drawable.delivery_man);
            if (!editing) setEditable(false);
        }).addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải: "+e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void enableEditing() {
        editing=true;
        edit.setText("Lưu");
        setEditable(true);
    }
    private void setEditable(boolean enabled) {
        name.setEnabled(enabled);
        phone.setEnabled(enabled);
        name.setBackgroundResource(enabled ? R.drawable.bg_edit_text : 0);
        phone.setBackgroundResource(enabled ? R.drawable.bg_edit_text : 0);
        email.setEnabled(false);
    }

    private void save() {
        String n=name.getText().toString().trim(), p=phone.getText().toString().trim();
        if (n.isEmpty()) { name.setError("Nhập họ tên"); return; }
        if (!p.matches("0[0-9]{9}")) { phone.setError("Số điện thoại không hợp lệ"); return; }
        edit.setEnabled(false);
        edit.setText("Đang lưu...");
        Map<String,Object> values=new HashMap<>();
        values.put("fullName",n); values.put("phone",p);
        db.collection("USERS").document(creatorId).update(values)
            .addOnSuccessListener(v -> {
                editing=false; edit.setEnabled(true); edit.setText("Sửa");
                setEditable(false); heading.setText(n);
                Toast.makeText(this,"Đã lưu thông tin",Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                edit.setEnabled(true); edit.setText("Lưu");
                Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
            });
    }

    private void toggle() {
        if (currentStatus == null) return;
        boolean active="ACTIVE".equalsIgnoreCase(currentStatus);
        new AlertDialog.Builder(this).setTitle(active ? "Khóa tài khoản" : "Mở khóa tài khoản")
            .setMessage(active ? "Xác nhận khóa nhân viên tạo đơn?" : "Xác nhận mở khóa nhân viên tạo đơn?")
            .setNegativeButton("Hủy",null)
            .setPositiveButton("Xác nhận",(dialog,which) -> {
                lock.setEnabled(false);
                db.collection("USERS").document(creatorId).update("status",active ? "INACTIVE":"ACTIVE")
                    .addOnSuccessListener(v -> { lock.setEnabled(true); load(); })
                    .addOnFailureListener(e -> {
                        lock.setEnabled(true);
                        Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
                    });
            }).show();
    }
}
