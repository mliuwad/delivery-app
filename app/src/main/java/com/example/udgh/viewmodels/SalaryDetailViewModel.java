package com.example.udgh.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.udgh.repositories.SalaryDetailRepository;

public class SalaryDetailViewModel extends ViewModel {
    private final SalaryDetailRepository repository = new SalaryDetailRepository();
    private final MutableLiveData<SalaryDetailRepository.DetailData> detail = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> paymentSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private String salaryDocumentId;
    private int loadRequest;

    public LiveData<SalaryDetailRepository.DetailData> getDetail() { return detail; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Boolean> getPaymentSuccess() { return paymentSuccess; }
    public LiveData<String> getMessage() { return message; }

    public void load(String shipperId, int month, int year) {
        int request = ++loadRequest;
        loading.setValue(true);
        message.setValue(null);
        repository.load(shipperId, month, year, new SalaryDetailRepository.Callback<SalaryDetailRepository.DetailData>() {
            @Override public void onSuccess(SalaryDetailRepository.DetailData value) {
                if (request != loadRequest) return;
                salaryDocumentId = value.salaryDocumentId;
                detail.postValue(value);
                loading.postValue(false);
            }
            @Override public void onError(Exception error) {
                if (request != loadRequest) return;
                message.postValue(error != null ? error.getMessage() : "Không thể tải chi tiết lương");
                loading.postValue(false);
            }
        });
    }

    public void confirmPayment() {
        paymentSuccess.setValue(null);
        repository.confirmPayment(salaryDocumentId, new SalaryDetailRepository.Callback<Void>() {
            @Override public void onSuccess(Void ignored) {
                paymentSuccess.postValue(true);
                message.postValue("Đã xác nhận thanh toán!");
            }
            @Override public void onError(Exception error) {
                paymentSuccess.postValue(false);
                message.postValue(error != null ? error.getMessage() : "Không thể xác nhận thanh toán");
            }
        });
    }
}
