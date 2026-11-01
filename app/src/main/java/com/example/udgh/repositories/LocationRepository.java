package com.example.udgh.repositories;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationRepository {
    public static class LocationResult {
        public final double latitude;
        public final double longitude;
        public final String address;
        public LocationResult(double latitude, double longitude, String address) {
            this.latitude = latitude;
            this.longitude = longitude == 0 ? 0 : longitude;
            this.address = address == null ? "" : address;
        }
    }
    public interface Callback {
        void onSuccess(LocationResult result);
        void onError(Exception error);
    }
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public LocationRepository(Context context) {
        this.context = context.getApplicationContext();
    }
    public void searchAddress(String query, Callback callback) {
        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> results = geocoder.getFromLocationName(query, 1);
                if (results == null || results.isEmpty()) {
                    postError(callback, new IllegalArgumentException("Không tìm thấy địa chỉ"));
                    return;
                }
                Address address = results.get(0);
                String line = address.getMaxAddressLineIndex() >= 0
                        ? address.getAddressLine(0) : query;
                LocationResult result = new LocationResult(
                        address.getLatitude(), address.getLongitude(), line);
                main.post(() -> callback.onSuccess(result));
            } catch (IOException | IllegalArgumentException error) {
                postError(callback, error);
            }
        });
    }
    public void reverseGeocode(double latitude, double longitude, Callback callback) {
        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> results = geocoder.getFromLocation(latitude, longitude, 1);
                String line = results != null && !results.isEmpty()
                        && results.get(0).getMaxAddressLineIndex() >= 0
                        ? results.get(0).getAddressLine(0) : "";
                LocationResult result = new LocationResult(latitude, longitude, line);
                main.post(() -> callback.onSuccess(result));
            } catch (IOException | IllegalArgumentException error) {
                postError(callback, error);
            }
        });
    }
    private void postError(Callback callback, Exception error) {
        main.post(() -> callback.onError(error));
    }
    public void shutdown() {
        executor.shutdownNow();
    }
}