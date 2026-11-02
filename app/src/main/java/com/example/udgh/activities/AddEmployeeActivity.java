package com.example.udgh.activities;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.ImageView;
import com.example.udgh.R;
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
        setContentView("SHIPPER".equals(role)
                ? R.layout.activity_shipper_detail : R.layout.activity_creator_detail);
        MaterialToolbar bar = findViewById(R.id.toolbar);
        bar.setTitle("Thêm nhân viên " + ("SHIPPER".equals(role) ? "giao hàng" : "tạo đơn"));
        bar.setNavigationOnClickListener(v -> finish());

        ImageView avatar = findViewById(R.id.imgAvatar);
        avatar.setClickable(false);
        avatar.setFocusable(false);
        TextView heading = findViewById(R.id.txtName);
        heading.setText("Nhân viên mới");
        TextView status = findViewById(R.id.txtStatus);
        status.setText("Hoạt động");

        name = findViewById(R.id.txtFullName);
        phone = findViewById(R.id.txtPhone);
        email = findViewById(R.id.txtEmail);
        name.setText(""); phone.setText(""); email.setText("");
        name.setHint("Nhập họ và tên");
        phone.setHint("Nhập số điện thoại");
        email.setHint("Nhập email đã tạo trong Firebase Auth");
        for (EditText e : new EditText[]{name, phone, email}) {
            e.setEnabled(true);
            e.setBackgroundResource(R.drawable.bg_edit_text);
        }
        if ("SHIPPER".equals(role)) {
            citizenId = findViewById(R.id.txtCitizenId);
            plate = findViewById(R.id.txtLicensePlate);
            address = findViewById(R.id.txtAddress);
            for (EditText e : new EditText[]{citizenId, plate, address}) {
                e.setText("");
                e.setEnabled(true);
                e.setBackgroundResource(R.drawable.bg_edit_text);
            }
            findViewById(R.id.txtTotalOrders).setVisibility(android.view.View.GONE);
            findViewById(R.id.txtSalary).setVisibility(android.view.View.GONE);
            findViewById(R.id.btnDelete).setVisibility(android.view.View.GONE);
        }
        findViewById(R.id.btnLock).setVisibility(android.view.View.GONE);
        save = findViewById(R.id.btnEdit);
        save.setText("Lưu");
        save.setOnClickListener(v -> saveEmployee());

        // Add a UID input in the same information card style as the detail screen.
        LinearLayout card = (LinearLayout) name.getParent();
        TextView label = new TextView(this);
        label.setText("UID Firebase Authentication");
        label.setTextColor(0xff111111);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        uid = new EditText(this);
        uid.setHint("Dán UID tài khoản đã tạo");
        uid.setTextColor(0xff111111);
        uid.setSingleLine(true);
        uid.setBackgroundResource(R.drawable.bg_edit_text);
        card.addView(label, 0);
        card.addView(uid, 1);
        name.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                heading.setText(s.length() == 0 ? "Nhân viên mới" : s);
            }
            public void afterTextChanged(android.text.Editable e) {}
        });
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
