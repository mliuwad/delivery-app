package com.example.udgh.activities;

import com.example.udgh.R;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.udgh.viewmodels.LocationViewModel;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;


public class LocationPickerActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private SearchView searchView;
    private MaterialButton btnConfirmLocation;
    private MapView mapView;
    private LocationViewModel locationViewModel;

    private Marker selectedMarker;
    private GeoPoint selectedLocation;
    private String selectedAddress = "";
    private String type = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_location_picker);
        locationViewModel = new ViewModelProvider(this).get(LocationViewModel.class);
        observeLocationViewModel();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ✅ THIẾT LẬP USER-AGENT VÀ CACHE (QUAN TRỌNG ĐỂ TRÁNH LỖI 403)
        Configuration.getInstance().setUserAgentValue("MyAndroidApp/1.0 (Android)");
        System.setProperty("http.agent", "MyAndroidApp/1.0");
        Configuration.getInstance().setOsmdroidBasePath(getCacheDir());
        Configuration.getInstance().setOsmdroidTileCache(getCacheDir());

        type = getIntent().getStringExtra("type");

        // Ánh xạ view
        toolbar = findViewById(R.id.toolbar);
        searchView = findViewById(R.id.searchView);
        customizeSearchView();
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        mapView = findViewById(R.id.mapView);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Chọn vị trí");
        toolbar.setNavigationOnClickListener(v -> finish());

        setupMap();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchLocation(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        btnConfirmLocation.setOnClickListener(v -> {
            if (selectedLocation != null && !selectedAddress.isEmpty()) {
                Intent result = new Intent();
                result.putExtra("lat", selectedLocation.getLatitude());
                result.putExtra("lng", selectedLocation.getLongitude());
                result.putExtra("address", selectedAddress);
                result.putExtra("type", type);
                setResult(RESULT_OK, result);
                finish();
            } else {
                Toast.makeText(this, "Vui lòng chọn vị trí trên bản đồ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ================== SETUP MAP ==================

    private void setupMap() {
        // ✅ SỬ DỤNG TILE SOURCE VỚI HTTPS ĐỂ TRÁNH LỖI 403
        XYTileSource tileSource = new XYTileSource(
                "OpenStreetMap",
                0, 18, 256,
                ".png",
                new String[]{
                        "https://a.tile.openstreetmap.org/",
                        "https://b.tile.openstreetmap.org/",
                        "https://c.tile.openstreetmap.org/"
                }
        );
        mapView.setTileSource(tileSource);

        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        // Vị trí mặc định: TP.HCM
        GeoPoint defaultLocation = new GeoPoint(10.8231, 106.6297);
        IMapController mapController = mapView.getController();
        mapController.setZoom(12.0);
        mapController.setCenter(defaultLocation);

        // Hiển thị vị trí hiện tại
        MyLocationNewOverlay myLocationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);

        // Xử lý click trên bản đồ
        Overlay clickOverlay = new Overlay() {
            @Override
            public boolean onSingleTapUp(MotionEvent e, MapView mapView) {
                GeoPoint point = (GeoPoint) mapView.getProjection().fromPixels(
                        (int) e.getX(),
                        (int) e.getY()
                );

                selectedLocation = point;
                selectedAddress = "";
                if (selectedMarker != null) {
                    mapView.getOverlays().remove(selectedMarker);
                }
                selectedMarker = new Marker(mapView);
                selectedMarker.setPosition(point);
                selectedMarker.setTitle("Vị trí đã chọn");
                mapView.getOverlays().add(selectedMarker);
                mapView.invalidate();

                getAddressFromLocation(point);
                return true;
            }
        };

        mapView.getOverlays().add(clickOverlay);
    }

    // ================== TÌM KIẾM ĐỊA CHỈ ==================

    private void searchLocation(String query) {
        locationViewModel.searchAddress(query);
    }

    private void getAddressFromLocation(GeoPoint point) {
        locationViewModel.resolveAddress(point.getLatitude(), point.getLongitude());
    }

    private void observeLocationViewModel() {
        locationViewModel.getSearchResult().observe(this, result -> {
            if (result == null) return;
            GeoPoint point = new GeoPoint(result.latitude, result.longitude);
            selectedLocation = point;
            selectedAddress = result.address;
            updateMarker(point, selectedAddress, true);
            Toast.makeText(this, "Đã tìm thấy: " + selectedAddress, Toast.LENGTH_SHORT).show();
        });
        locationViewModel.getReverseResult().observe(this, result -> {
            if (result == null) return;
            selectedAddress = result.address.isEmpty()
                    ? "Địa chỉ: " + result.latitude + ", " + result.longitude : result.address;
            if (selectedMarker != null) selectedMarker.setTitle(selectedAddress);
        });
        locationViewModel.getSearchError().observe(this, message -> {
            if (message != null && !message.isEmpty())
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
        locationViewModel.getReverseError().observe(this, message -> {
            if (selectedLocation != null)
                selectedAddress = "Địa chỉ: " + selectedLocation.getLatitude()
                        + ", " + selectedLocation.getLongitude();
        });
    }

    private void updateMarker(GeoPoint point, String title, boolean zoomIn) {
        if (zoomIn) {
            IMapController controller = mapView.getController();
            controller.setZoom(15.0);
            controller.setCenter(point);
        }
        if (selectedMarker != null) mapView.getOverlays().remove(selectedMarker);
        selectedMarker = new Marker(mapView);
        selectedMarker.setPosition(point);
        selectedMarker.setTitle(title);
        mapView.getOverlays().add(selectedMarker);
        mapView.invalidate();
    }

    private void customizeSearchView() {

        // Background toàn bộ ô tìm kiếm
        searchView.setBackgroundResource(
                R.drawable.bg_search
        );

        // Hint
        searchView.setQueryHint("Tìm kiếm địa chỉ...");

        // Không tự mở rộng toàn màn hình
        searchView.setIconifiedByDefault(false);

        // Cho phép nút X xuất hiện khi có nội dung
        searchView.setSubmitButtonEnabled(false);

        // Lấy ô nhập text bên trong SearchView
        android.widget.EditText searchText =
                searchView.findViewById(
                        androidx.appcompat.R.id.search_src_text
                );

        if (searchText != null) {

            searchText.setTextSize(15);

            searchText.setTextColor(
                    getColor(android.R.color.black)
            );

            searchText.setHintTextColor(
                    android.graphics.Color.rgb(
                            140, 140, 140
                    )
            );

            searchText.setSingleLine(true);

            searchText.setPadding(
                    4,
                    0,
                    4,
                    0
            );

            searchText.setHint(
                    "Tìm kiếm địa chỉ..."
            );
        }

        // Icon kính lúp
        android.widget.ImageView searchIcon =
                searchView.findViewById(
                        androidx.appcompat.R.id.search_button
                );

        if (searchIcon != null) {

            searchIcon.setImageResource(
                    androidx.appcompat.R.drawable.abc_ic_search_api_material
            );

            searchIcon.setColorFilter(
                    android.graphics.Color.rgb(
                            100, 100, 100
                    ),
                    android.graphics.PorterDuff.Mode.SRC_IN
            );
        }

        // Nút X xóa nội dung
        android.widget.ImageView closeIcon =
                searchView.findViewById(
                        androidx.appcompat.R.id.search_close_btn
                );

        if (closeIcon != null) {

            closeIcon.setColorFilter(
                    android.graphics.Color.rgb(
                            100, 100, 100
                    ),
                    android.graphics.PorterDuff.Mode.SRC_IN
            );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

}