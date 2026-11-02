package com.example.udgh.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.udgh.R;
import com.example.udgh.models.User;
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
    private final List<User> creators = new ArrayList<>();
    private final List<User> filtered = new ArrayList<>();
    private SwipeRefreshLayout refresh;
    private SearchView searchView;
    private TextView total;
    private RecyclerView.Adapter<CreatorRow> adapter;
    private boolean initialized;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_creator_management);
        RoleAccess.requireRole(this, "ADMIN", this::init);
    }

    private void init() {
        if (initialized) return;
        initialized = true;
        MaterialToolbar bar = findViewById(R.id.toolbar);
        bar.setNavigationOnClickListener(v -> finish());
        bar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_add) { showCreate(); return true; }
            return false;
        });
        searchView = findViewById(R.id.searchView);
        total = findViewById(R.id.txtTotal);
        refresh = findViewById(R.id.swipeRefreshLayout);
        refresh.setColorSchemeResources(R.color.primary);
        refresh.setOnRefreshListener(this::load);
        RecyclerView list = findViewById(R.id.rvCreator);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerView.Adapter<CreatorRow>() {
            @Override public CreatorRow onCreateViewHolder(android.view.ViewGroup parent, int type) {
                return new CreatorRow(getLayoutInflater().inflate(R.layout.item_shipper, parent, false));
            }
            @Override public void onBindViewHolder(CreatorRow h, int position) {
                User user = filtered.get(position);
                h.name.setText(user.getFullName());
                h.phone.setText("SĐT: " + user.getPhone());
                h.email.setText("Email: " + user.getEmail());
                boolean active = "ACTIVE".equalsIgnoreCase(user.getStatus());
                h.status.setText(active ? "Hoạt động" : "Khóa");
                h.status.setTextColor(getColor(active ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
                h.avatar.setImageResource(R.drawable.delivery_man);
                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty())
                    com.bumptech.glide.Glide.with(CreatorManagementActivity.this)
                        .load(user.getAvatarUrl()).circleCrop().placeholder(R.drawable.delivery_man).into(h.avatar);
                h.itemView.setOnClickListener(v -> openDetail(user, false));
                h.edit.setOnClickListener(v -> openDetail(user, true));
                h.lock.setOnClickListener(v -> toggle(user));
            }
            @Override public int getItemCount() { return filtered.size(); }
        };
        list.setAdapter(adapter);
        searchView.setIconifiedByDefault(false);
        AutoCompleteTextView searchText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchText != null) {
            searchText.setTextColor(getColor(R.color.black));
            searchText.setHintTextColor(getColor(R.color.gray));
        }
        for (int id : new int[]{androidx.appcompat.R.id.search_button,
                androidx.appcompat.R.id.search_close_btn, androidx.appcompat.R.id.search_mag_icon}) {
            ImageView icon = searchView.findViewById(id);
            if (icon != null) icon.setColorFilter(getColor(R.color.black), PorterDuff.Mode.SRC_IN);
        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { render(); return true; }
            @Override public boolean onQueryTextChange(String query) { render(); return true; }
        });
        load();
    }

    static class CreatorRow extends RecyclerView.ViewHolder {
        TextView name, phone, email, status;
        ImageView avatar;
        ImageButton edit, lock;
        CreatorRow(View item) {
            super(item);
            name=item.findViewById(R.id.txtName);
            phone=item.findViewById(R.id.txtPhone);
            email=item.findViewById(R.id.txtEmail);
            status=item.findViewById(R.id.txtStatus);
            avatar=item.findViewById(R.id.imgAvatar);
            edit=item.findViewById(R.id.btnEdit);
            lock=item.findViewById(R.id.btnLock);
        }
    }

    private void load() {
        if (!initialized) return;
        refresh.setRefreshing(true);
        db.collection("USERS").whereEqualTo("role", "CREATOR").get()
            .addOnSuccessListener(snapshot -> {
                creators.clear();
                for (QueryDocumentSnapshot doc : snapshot) {
                    User user = doc.toObject(User.class);
                    user.setUserId(doc.getId());
                    creators.add(user);
                }
                refresh.setRefreshing(false);
                render();
            })
            .addOnFailureListener(error -> {
                refresh.setRefreshing(false);
                Toast.makeText(this, "Không tải được danh sách: " + error.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

    private void render() {
        if (!initialized) return;
        String query = searchView.getQuery().toString().trim().toLowerCase(Locale.ROOT);
        filtered.clear();
        for (User u : creators) {
            String haystack = (String.valueOf(u.getFullName())+" "+String.valueOf(u.getEmail())+" "+String.valueOf(u.getPhone())).toLowerCase(Locale.ROOT);
            if (haystack.contains(query)) filtered.add(u);
        }
        total.setText("Tổng số nhân viên: " + filtered.size());
        adapter.notifyDataSetChanged();
    }

    private void openDetail(User u, boolean edit) {
        Intent intent = new Intent(this, CreatorDetailActivity.class);
        intent.putExtra("creatorId", u.getUserId());
        if (edit) intent.putExtra("mode", "edit");
        startActivity(intent);
    }

    private void toggle(User u) {
        boolean active = "ACTIVE".equalsIgnoreCase(u.getStatus());
        new AlertDialog.Builder(this)
            .setMessage(active ? "Khóa tài khoản này?" : "Mở khóa tài khoản này?")
            .setNegativeButton("Hủy",null)
            .setPositiveButton("Xác nhận",(d,w)->db.collection("USERS").document(u.getUserId())
                .update("status",active ? "INACTIVE":"ACTIVE")
                .addOnSuccessListener(x->load())
                .addOnFailureListener(e->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show()))
            .show();
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density); }
    private EditText field(String hint, int type) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setInputType(type);
        return input;
    }
    @Override protected void onResume() {
        super.onResume();
        if (initialized) load();
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
            Map<String,Object> payload=new HashMap<>();
            payload.put("fullName",n);
            payload.put("phone",p);
            payload.put("email",e);
            payload.put("password",pw);
            com.google.firebase.functions.FirebaseFunctions.getInstance()
                .getHttpsCallable("createCreator").call(payload)
                .addOnSuccessListener(result->{dialog.dismiss();load();})
                .addOnFailureListener(err->{
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    Toast.makeText(this,"Không tạo được Creator: "+err.getMessage(),Toast.LENGTH_LONG).show();
                });
        }));
        dialog.show();
    }
}
