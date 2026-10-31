package com.example.udgh.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.udgh.repository.OrderActionRepository;

public class OrderActionViewModel extends ViewModel {

    private final OrderActionRepository repository = new OrderActionRepository();

    private final MutableLiveData<String> userRole = new MutableLiveData<>();
    private final MutableLiveData<ActionResult> actionResult = new MutableLiveData<>();

    public LiveData<String> getUserRole() {
        return userRole;
    }

    public LiveData<ActionResult> getActionResult() {
        return actionResult;
    }

    public void loadUserRole(String userId) {
        repository.loadUserRole(userId, (role, error) -> {
            if (error == null) {
                userRole.postValue(role);
            } else {
                actionResult.postValue(new ActionResult(false, null, error));
            }
        });
    }

    public void updateOrderStatus(String orderId, String userId, String newStatus) {
        repository.updateOrderStatus(orderId, userId, newStatus,
                (success, status, error) ->
                        actionResult.postValue(new ActionResult(success, status, error)));
    }

    public static class ActionResult {
        private final boolean success;
        private final String status;
        private final Exception error;

        public ActionResult(boolean success, String status, Exception error) {
            this.success = success;
            this.status = status;
            this.error = error;
        }

        public boolean isSuccess() { return success; }
        public String getStatus() { return status; }
        public Exception getError() { return error; }
    }
}
