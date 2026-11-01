package com.example.udgh.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.udgh.models.StatisticsData;
import com.example.udgh.repositories.StatisticsRepository;

public class StatisticsViewModel extends ViewModel {
    private final StatisticsRepository repository = new StatisticsRepository();
    private final MutableLiveData<StatisticsData> data = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private int requestId;

    public LiveData<StatisticsData> getData() { return data; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    public void load(int month, int year) {
        int currentRequest = ++requestId;
        loading.setValue(true);
        error.setValue(null);
        repository.load(month, year, new StatisticsRepository.Callback() {
            @Override
            public void onSuccess(StatisticsData result) {
                if (currentRequest != requestId) return;
                data.postValue(result);
                loading.postValue(false);
            }

            @Override
            public void onError(Exception exception) {
                if (currentRequest != requestId) return;
                loading.postValue(false);
                error.postValue(exception != null && exception.getMessage() != null
                        ? exception.getMessage() : "Không thể tải dữ liệu thống kê");
            }
        });
    }
}
