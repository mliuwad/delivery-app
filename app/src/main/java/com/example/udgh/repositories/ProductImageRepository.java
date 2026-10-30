
package com.example.udgh.repositories;

import android.util.Base64;

import com.example.udgh.BuildConfig;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProductImageRepository {

    private final OkHttpClient client = new OkHttpClient();

    public interface ImageCallback {
        void onSuccess(String imageUrl);
        void onError(Exception e);
    }

    public void uploadImage(byte[] imageBytes, ImageCallback callback) {

        if (imageBytes == null || imageBytes.length == 0) {
            callback.onError(new Exception("Dữ liệu ảnh không hợp lệ"));
            return;
        }

        String apiKey = BuildConfig.IMGBB_API_KEY;

        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError(new Exception("Chưa cấu hình ImgBB API Key"));
            return;
        }

        String base64Image = Base64.encodeToString(
                imageBytes,
                Base64.NO_WRAP
        );

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("key", apiKey)
                .addFormDataPart("image", base64Image)
                .build();

        Request request = new Request.Builder()
                .url("https://api.imgbb.com/1/upload")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response closeableResponse = response) {

                    if (!closeableResponse.isSuccessful()
                            || closeableResponse.body() == null) {
                        callback.onError(
                                new Exception("Upload ảnh thất bại: HTTP "
                                        + closeableResponse.code())
                        );
                        return;
                    }

                    String responseBody =
                            closeableResponse.body().string();

                    JSONObject jsonObject = new JSONObject(responseBody);

                    if (!jsonObject.optBoolean("success", false)) {
                        callback.onError(
                                new Exception("ImgBB không thể upload ảnh")
                        );
                        return;
                    }

                    JSONObject data = jsonObject.getJSONObject("data");
                    String imageUrl = data.getString("url");

                    callback.onSuccess(imageUrl);

                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }
}
