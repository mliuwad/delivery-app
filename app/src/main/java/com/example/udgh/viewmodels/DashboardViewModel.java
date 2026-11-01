package com.example.udgh.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.udgh.repositories.DashboardRepository;

public class DashboardViewModel extends ViewModel {

    private final DashboardRepository repository = new DashboardRepository();

    private final MutableLiveData<DashboardRepository.CreatorDashboard> creatorDashboard =
            new MutableLiveData<>();
    private final MutableLiveData<DashboardRepository.ShipperDashboard> shipperDashboard =
            new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private int creatorRequestId;
    private int shipperRequestId;

    public LiveData<DashboardRepository.CreatorDashboard> getCreatorDashboard() {
        return creatorDashboard;
    }

    public LiveData<DashboardRepository.ShipperDashboard> getShipperDashboard() {
        return shipperDashboard;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadCreatorDashboard(String userId) {
        int requestId = ++creatorRequestId;
        errorMessage.setValue(null);
        repository.loadCreatorDashboard(userId, new DashboardRepository.Callback<DashboardRepository.CreatorDashboard>() {
            @Override
            public void onSuccess(DashboardRepository.CreatorDashboard data) {
                if (requestId == creatorRequestId) creatorDashboard.postValue(data);
            }

            @Override
            public void onError(Exception error) {
                if (requestId == creatorRequestId) {
                    errorMessage.postValue(error != null ? error.getMessage() : "Không thể tải dashboard Creator");
                }
            }
        });
    }

    public void loadShipperDashboard(String userId) {
        int requestId = ++shipperRequestId;
        errorMessage.setValue(null);
        repository.loadShipperDashboard(userId, new DashboardRepository.Callback<DashboardRepository.ShipperDashboard>() {
            @Override
            public void onSuccess(DashboardRepository.ShipperDashboard data) {
                if (requestId == shipperRequestId) shipperDashboard.postValue(data);
            }

            @Override
            public void onError(Exception error) {
                if (requestId == shipperRequestId) {
                    errorMessage.postValue(error != null ? error.getMessage() : "Không thể tải dashboard Shipper");
                }
            }
        });
    }
}
