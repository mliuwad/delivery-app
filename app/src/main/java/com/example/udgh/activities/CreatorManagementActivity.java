package com.example.udgh.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.udgh.R;
import com.example.udgh.utils.RoleAccess;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.*;

public class CreatorManagementActivity extends AppCompatActivity {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private LinearLayout list;
    private ProgressBar loading;
    private EditText search;
    private final List<com.example.udgh.models.User> creators = new ArrayList<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        RoleAccess.requireRole(this, "ADMIN", this::init);
    }

    private void init() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.background));
        MaterialToolbar bar = new MaterialToolbar(this);
        bar.setBackgroundColor(getColor(R.color.primary));
        bar.setTitleTextColor(getColor(R.color.white));
        bar.setTitle("Nhân viên tạo đơn");
        bar.setNavigationIcon(R.drawable.sgv_back);
        bar.setNavigationOnClickListener(v -> finish());
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(56)));
        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Tìm theo tên, email, số điện thoại");
        search.setPadding(dp(16), 0, dp(16), 0);
        root.addView(search, new LinearLayout.LayoutParams(-1, dp(54)));
        Button add = new Button(this);
        add.setText("Thêm nhân viên tạo đơn");
        root.addView(add);
        loading = new ProgressBar(this);
        root.addView(loading);
        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(12), dp(8), dp(12), dp(16));
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int count,int after) {}
            public void onTextChanged(CharSequence s,int st,int before,int count) { render(); }
            public void afterTextChanged(android.text.Editable e) {}
        });
        add.setOnClickListener(v -> showCreate());
        load();
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density); }
    private EditText field(String hint, int type) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setInputType(type);
        return input;
    }
    private void load() {
        loading.setVisibility(View.VISIBLE);
        db.collection("USERS").whereEqualTo("role","CREATOR").get()
          .addOnSuccessListener(s -> {
              creators.clear();
              for (QueryDocumentSnapshot d : s) {
                  com.example.udgh.models.User u = d.toObject(com.example.udgh.models.User.class);
                  u.setUserId(d.getId());
                  creators.add(u);
              }
              loading.setVisibility(View.GONE);
              render();
          }).addOnFailureListener(e -> {
              loading.setVisibility(View.GONE);
              Toast.makeText(this,"Không tải được danh sách: "+e.getMessage(),Toast.LENGTH_LONG).show();
          });
    }
    private void render() {
        if (list == null) return;
        list.removeAllViews();
        String q = search.getText().toString().toLowerCase(Locale.ROOT).trim();
        int shown = 0;
        for (com.example.udgh.models.User u : creators) {
            String text = (String.valueOf(u.getFullName())+" "+String.valueOf(u.getEmail())+" "+String.valueOf(u.getPhone())).toLowerCase(Locale.ROOT);
            if (!text.contains(q)) continue;
            shown++;
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(14),dp(12),dp(14),dp(12));
            box.setBackgroundColor(getColor(R.color.white));
            TextView label = new TextView(this);
            label.setText(String.valueOf(u.getFullName())+"\n"+String.valueOf(u.getEmail())+"\n"+String.valueOf(u.getPhone())+" · "+String.valueOf(u.getStatus()));
            label.setTextColor(getColor(R.color.black));
            label.setTextSize(16);
            box.addView(label);
            Button edit = new Button(this);
            edit.setText("Sửa thông tin");
            edit.setOnClickListener(v -> showEdit(u));
            box.addView(edit);
            Button toggle = new Button(this);
            boolean active = "ACTIVE".equalsIgnoreCase(u.getStatus());
            toggle.setText(active ? "Khóa tài khoản" : "Mở khóa");
            toggle.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setMessage(active ? "Khóa tài khoản này?" : "Mở khóa tài khoản này?")
                .setNegativeButton("Hủy",null)
                .setPositiveButton("Xác nhận",(d,w) -> db.collection("USERS").document(u.getUserId())
                    .update("status",active ? "INACTIVE":"ACTIVE")
                    .addOnSuccessListener(x -> load())
                    .addOnFailureListener(e -> Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show())).show());
            box.addView(toggle);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
            lp.bottomMargin=dp(10);
            list.addView(box,lp);
        }
        if (shown == 0) {
            TextView empty = new TextView(this);
            empty.setText("Không có nhân viên phù hợp");
            list.addView(empty);
        }
    }
    private void showEdit(com.example.udgh.models.User u) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20),0,dp(20),0);
        EditText name=field("Họ tên",InputType.TYPE_CLASS_TEXT);
        EditText phone=field("Số điện thoại",InputType.TYPE_CLASS_PHONE);
        name.setText(u.getFullName()); phone.setText(u.getPhone());
        form.addView(name);form.addView(phone);
        new AlertDialog.Builder(this).setTitle("Sửa nhân viên tạo đơn").setView(form)
          .setNegativeButton("Hủy",null).setPositiveButton("Lưu",(d,w)->{
              String n=name.getText().toString().trim(),p=phone.getText().toString().trim();
              if(n.isEmpty() || !p.matches("0[0-9]{9}")) {Toast.makeText(this,"Tên hoặc số điện thoại không hợp lệ",Toast.LENGTH_LONG).show();return;}
              Map<String,Object> updates=new HashMap<>();
              updates.put("fullName",n);updates.put("phone",p);
              db.collection("USERS").document(u.getUserId()).update(updates)
                .addOnSuccessListener(x->load())
                .addOnFailureListener(e->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());
          }).show();
    }
    private void showCreate() {
        LinearLayout form=new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20),0,dp(20),0);
        EditText name=field("Họ tên",InputType.TYPE_CLASS_TEXT);
        EditText phone=field("Số điện thoại",InputType.TYPE_CLASS_PHONE);
        EditText email=field("Email",InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText pass=field("Mật khẩu (ít nhất 6 ký tự)",InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(name);form.addView(phone);form.addView(email);form.addView(pass);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Tạo tài khoản Creator")
            .setView(form).setNegativeButton("Hủy",null).setPositiveButton("Tạo",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String n=name.getText().toString().trim(),p=phone.getText().toString().trim(),e=email.getText().toString().trim(),pw=pass.getText().toString();
            if(n.isEmpty() || !p.matches("0[0-9]{9}") || !android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches() || pw.length()<6){
                Toast.makeText(this,"Kiểm tra lại tên, SĐT, email và mật khẩu",Toast.LENGTH_LONG).show();return;
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            FirebaseApp app=FirebaseApp.initializeApp(getApplicationContext(),FirebaseApp.getInstance().getOptions(),"creator-"+UUID.randomUUID());
            FirebaseAuth secondary=FirebaseAuth.getInstance(app);
            secondary.createUserWithEmailAndPassword(e,pw).addOnSuccessListener(auth->{
                String uid=auth.getUser().getUid();
                Map<String,Object> data=new HashMap<>();
                data.put("fullName",n);data.put("phone",p);data.put("email",e);
                data.put("role","CREATOR");data.put("status","ACTIVE");
                data.put("avatarUrl","");data.put("createdAt",Timestamp.now());
                // Ghi bằng phiên Admin chính, không đổi session Admin.
                db.collection("USERS").document(uid).set(data).addOnSuccessListener(z->{
                    secondary.signOut();app.delete();dialog.dismiss();load();
                }).addOnFailureListener(err->auth.getUser().delete().addOnCompleteListener(z->{
                    secondary.signOut();app.delete();
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    Toast.makeText(this,"Không lưu được Creator: "+err.getMessage(),Toast.LENGTH_LONG).show();
                }));
            }).addOnFailureListener(err->{
                secondary.signOut();app.delete();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                Toast.makeText(this,"Không tạo được tài khoản: "+err.getMessage(),Toast.LENGTH_LONG).show();
            });
        }));
        dialog.show();
    }
}
