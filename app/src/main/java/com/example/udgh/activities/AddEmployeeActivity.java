package com.example.udgh.activities;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.view.View;
import android.graphics.Color;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.textfield.TextInputLayout;
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
    private EditText name, phone, email, citizenId, plate, address, password;
    private FirebaseApp secondaryApp;
    private FirebaseAuth secondaryAuth;
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
        email.setHint("Email đăng nhập");
        for (EditText e : new EditText[]{name, phone, email}) {
            e.setEnabled(true);
            styleInput(e);
        }
        if ("SHIPPER".equals(role)) {
            citizenId = findViewById(R.id.txtCitizenId);
            plate = findViewById(R.id.txtLicensePlate);
            address = findViewById(R.id.txtAddress);
            for (EditText e : new EditText[]{citizenId, plate, address}) {
                e.setText("");
                e.setEnabled(true);
                styleInput(e);
            }
            findViewById(R.id.txtTotalOrders).setVisibility(android.view.View.GONE);
            findViewById(R.id.txtSalary).setVisibility(android.view.View.GONE);
            findViewById(R.id.btnDelete).setVisibility(android.view.View.GONE);
        }
        findViewById(R.id.btnLock).setVisibility(android.view.View.GONE);
        save = findViewById(R.id.btnEdit);
        save.setText("Lưu");
        save.setOnClickListener(v -> saveEmployee());

        LinearLayout card = (LinearLayout) name.getParent();
        TextView passwordLabel = new TextView(this);
        passwordLabel.setText("Mật khẩu đăng nhập");
        passwordLabel.setTextColor(Color.BLACK);
        passwordLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        password = new EditText(this);
        password.setHint("Tối thiểu 6 ký tự");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        styleInput(password);
        int insertAt = card.indexOfChild(email) + 1;
        card.addView(passwordLabel, insertAt);
        card.addView(password, insertAt + 1);
        name.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                heading.setText(s.length() == 0 ? "Nhân viên mới" : s);
            }
            public void afterTextChanged(android.text.Editable e) {}
        });
    }

    private void styleInput(EditText e) {
        e.setTextColor(Color.BLACK);
        e.setHintTextColor(0xff888888);
        e.setTextSize(15);
        e.setSingleLine(true);
        e.setPadding(dp(12), dp(12), dp(12), dp(12));
        e.setBackgroundResource(R.drawable.bg_edit_text);
        e.setSelectAllOnFocus(false);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private String value(EditText e) { return e.getText().toString().trim(); }

    private void saveEmployee() {
        String fullName = value(name), mobile = value(phone), mail = value(email);
        String pass = password.getText().toString();
        if (TextUtils.isEmpty(fullName) || !mobile.matches("0[0-9]{9}")
                || !android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()
                || pass.length() < 6) {
            Toast.makeText(this, "Nhập đủ họ tên, SĐT, email và mật khẩu từ 6 ký tự",
                    Toast.LENGTH_LONG).show();
            return;
        }
        save.setEnabled(false);
        FirebaseApp mainApp = FirebaseApp.getInstance();
        String appName = "employee-provisioning";
        secondaryApp = FirebaseApp.getApps(this).stream()
                .filter(app -> appName.equals(app.getName()))
                .findFirst().orElse(null);
        if (secondaryApp == null) {
            secondaryApp = FirebaseApp.initializeApp(this, mainApp.getOptions(), appName);
        }
        secondaryAuth = FirebaseAuth.getInstance(secondaryApp);
        secondaryAuth.createUserWithEmailAndPassword(mail, pass)
                .addOnSuccessListener(result -> {
                    FirebaseUser created = result.getUser();
                    if (created == null) {
                        finishProvisioning("Không lấy được UID tài khoản mới");
                        return;
                    }
                    saveProfile(created, fullName, mobile, mail);
                })
                .addOnFailureListener(e -> finishProvisioning(
                        "Không tạo được tài khoản: " + e.getMessage()));
    }

    private void saveProfile(FirebaseUser created, String fullName, String mobile, String mail) {
        String id = created.getUid();
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
        if ("SHIPPER".equals(role)) {
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
            Toast.makeText(this, "Đã thêm nhân viên thành công", Toast.LENGTH_LONG).show();
            secondaryAuth.signOut();
            finish();
        }).addOnFailureListener(e ->
            created.delete().addOnCompleteListener(rollback -> {
                String message = "Không lưu được hồ sơ: " + e.getMessage();
                if (!rollback.isSuccessful()) {
                    message += ". Tài khoản Auth đã tạo, cần xử lý thủ công.";
                }
                finishProvisioning(message);
            }));
    }

    private void finishProvisioning(String message) {
        save.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        if (secondaryAuth != null) secondaryAuth.signOut();
    }
}
