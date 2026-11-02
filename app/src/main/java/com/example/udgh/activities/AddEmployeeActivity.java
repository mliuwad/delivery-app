package com.example.udgh.activities;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.udgh.utils.RoleAccess;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * Demo-only: Auth account must already be created by the project administrator
 * in Firebase Console. This screen links its UID to an employee profile.
 */
public class AddEmployeeActivity extends AppCompatActivity {
    private EditText uid, name, phone, email, citizenId, plate, address;
    private MaterialButton save;
    private String role;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        role = "SHIPPER".equals(getIntent().getStringExtra("role")) ? "SHIPPER" : "CREATOR";
        RoleAccess.requireRole(this, "ADMIN", this::initialize);
    }

    private EditText field(LinearLayout root, String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(0xff111111);
        e.setHintTextColor(0xff777777);
        e.setSingleLine(true);
        e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff1565c0));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = 16;
        root.addView(e, lp);
        return e;
    }

    private void initialize() {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackgroundColor(0xfff5f7fa);
        MaterialToolbar bar = new MaterialToolbar(this);
        bar.setBackgroundColor(0xff1565c0);
        bar.setTitle("Thêm " + (role.equals("SHIPPER") ? "Shipper" : "Creator"));
        bar.setTitleTextColor(-1);
        bar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        bar.setNavigationOnClickListener(v -> finish());
        outer.addView(bar);
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(20 * getResources().getDisplayMetrics().density);
        body.setPadding(pad, pad, pad, pad);
        TextView guide = new TextView(this);
        guide.setText("Bước 1: Tạo tài khoản Email/Password trong Firebase Console > Authentication > Users.\\nBước 2: Sao chép UID và điền thông tin bên dưới.");
        guide.setTextColor(0xff333333);
        guide.setTextSize(15);
        body.addView(guide);
        uid = field(body, "UID tài khoản Firebase Auth *");
        name = field(body, "Họ và tên *");
        phone = field(body, "Số điện thoại *");
        email = field(body, "Email đã tạo trong Firebase Auth *");
        email.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        if (role.equals("SHIPPER")) {
            citizenId = field(body, "CCCD");
            plate = field(body, "Biển số xe");
            address = field(body, "Địa chỉ");
        }
        save = new MaterialButton(this);
        save.setText("Lưu nhân viên");
        body.addView(save);
        save.setOnClickListener(v -> saveEmployee());
        scroll.addView(body);
        outer.addView(scroll);
        setContentView(outer);
    }

    private String value(EditText field) { return field.getText().toString().trim(); }

    private void saveEmployee() {
        String id = value(uid), fullName = value(name), mobile = value(phone), mail = value(email);
        if (TextUtils.isEmpty(id) || id.contains("/") || TextUtils.isEmpty(fullName)
                || !mobile.matches("0[0-9]{9}")
                || !android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            Toast.makeText(this, "Kiểm tra UID, họ tên, số điện thoại và email", Toast.LENGTH_LONG).show();
            return;
        }
        save.setEnabled(false);
        db.collection("USERS").document(id).get().addOnSuccessListener(existing -> {
            if (existing.exists()) {
                save.setEnabled(true);
                Toast.makeText(this, "UID đã có hồ sơ, không ghi đè", Toast.LENGTH_LONG).show();
                return;
            }
            Map<String, Object> profile = new HashMap<>();
            profile.put("fullName", fullName);
            profile.put("phone", mobile);
            profile.put("email", mail);
            profile.put("role", role);
            profile.put("status", "ACTIVE");
            profile.put("avatarUrl", "");
            profile.put("createdAt", FieldValue.serverTimestamp());
            com.google.firebase.firestore.WriteBatch batch = db.batch();
            batch.set(db.collection("USERS").document(id), profile);
            if (role.equals("SHIPPER")) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("shipperId", id);
                detail.put("citizenId", value(citizenId));
                detail.put("licensePlate", value(plate));
                detail.put("address", value(address));
                detail.put("joinDate", FieldValue.serverTimestamp());
                batch.set(db.collection("USERS").document(id)
                    .collection("SHIPPER_DETAILS").document(id), detail);
            }
            batch.commit().addOnSuccessListener(v -> {
                Toast.makeText(this, "Đã thêm " + role, Toast.LENGTH_LONG).show();
                finish();
            }).addOnFailureListener(e -> {
                save.setEnabled(true);
                Toast.makeText(this, "Không thể lưu: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }).addOnFailureListener(e -> {
            save.setEnabled(true);
            Toast.makeText(this, "Không thể kiểm tra UID: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}
