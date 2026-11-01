package com.example.udgh.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.udgh.repositories.LocationRepository;

public class LocationViewModel extends AndroidViewModel {
    private final LocationRepository repository;
    private final MutableLiveData<LocationRepository.LocationResult> searchResult = new MutableLiveData<>();
    private final MutableLiveData<LocationRepository.LocationResult> reverseResult = new MutableLiveData<>();
    private final MutableLiveData<String> searchError = new MutableLiveData<>();
    private final MutableLiveData<String> reverseError = new MutableLiveData<>();
    private int searchRequestId;
    private int reverseRequestId;

    public LocationViewModel(@NonNull Application application) {
        super(application);
        repository = new LocationRepository(application);
    }
    public LiveData<LocationRepository.LocationResult> getSearchResult() { return searchResult; }
    public LiveData<LocationRepository.LocationResult> getReverseResult() { return reverseResult; }
    public LiveData<String> getSearchError() { return searchError; }
    public LiveData<String> getReverseError() { return reverseError; }

    public void searchAddress(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchError.setValue("Vui lòng nhập địa chỉ cần tìm");
            return;
        }
        int requestId = ++searchRequestId;
        searchError.setValue(null);
        repository.searchAddress(query.trim(), new LocationRepository.Callback() {
            @Override public void onSuccess(LocationRepository.LocationResult result) {
                if (requestId == searchRequestId) searchResult.setValue(result);
            }
            @Override public void onError(Exception error) {
                if (requestId == searchRequestId) {
                    searchError.setValue(error != null && error.getMessage() != null
                            ? error.getMessage() : "Không tìm thấy địa chỉ");
                }
            }
        });
    }

    public void resolveAddress(double latitude, double longitude) {
        int requestId = ++reverseRequestId;
        reverseError.setValue(null);
        repository.reverseGeocode(latitude, longitude, new LocationRepository.Callback() {
            @Override public void onSuccess(LocationRepository.LocationResult result) {
                if (requestId == reverseRequestId) reverseResult.setValue(result);
            }
            @Override public void onError(Exception error) {
                if (requestId == reverseRequestId) {
                    reverseError.setValue(error != null && error.getMessage() != null
                            ? error.getMessage() : "Không thể lấy địa chỉ");
                }
            }
        });
    }
    @Override protected void onCleared() {
        repository.shutdown();
        super.onCleared();
    }
}