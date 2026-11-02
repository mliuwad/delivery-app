# Cấp tài khoản Creator và Shipper (demo Firebase Spark)

Ứng dụng không cho người dùng tự đăng ký trong giao diện đăng nhập. **Admin đang hoạt động** có thể mở Quản lý Creator/Shipper → Thêm, nhập tên, số điện thoại, email, mật khẩu ban đầu và trạng thái (Hoạt động/Khóa), rồi bấm **Lưu**.

Ứng dụng dùng Firebase Authentication thứ hai (secondary FirebaseApp) để tạo tài khoản mới mà không thay đổi phiên đăng nhập Admin chính. Firebase Auth tự sinh UID; ứng dụng ghi hồ sơ `USERS/{uid}`, và với Shipper ghi thêm `USERS/{uid}/SHIPPER_DETAILS/{uid}`. Nếu ghi Firestore thất bại, ứng dụng cố gắng xóa tài khoản Auth mới; việc hoàn tác không được đảm bảo. Hiện Admin nhập mật khẩu ban đầu, chưa có luồng email đặt mật khẩu riêng.

## Khởi tạo Admin đầu tiên

Chỉ tài khoản Admin đầu tiên cần tạo thủ công trong Firebase Console:

1. Firebase Console → Authentication → Users → thêm tài khoản Email/Password, sao chép UID.
2. Firestore → `USERS/{uid}` với `role: "ADMIN"`, `status: "ACTIVE"`, `email`, `fullName`, `phone`, `avatarUrl` và `createdAt`.
3. Đăng nhập bằng Admin này trong ứng dụng.

Không tạo tài khoản ADMIN từ giao diện Thêm nhân viên. Không lưu mật khẩu trong Firestore.

## Trạng thái

Tài khoản mới lưu `ACTIVE` (Hoạt động) hoặc `INACTIVE` (Khóa). Nếu dữ liệu demo cũ đang lưu `LOCKED`, chuyển sang `INACTIVE` trước khi áp dụng quy tắc mới.

## Giới hạn và cảnh báo

- Đây là demo phía client, **không phải dịch vụ cấp tài khoản đặc quyền an toàn cho production**. Firebase Auth cho phép tạo tài khoản theo cấu hình Authentication độc lập với Firestore Rules; Rules chỉ giới hạn thao tác với Firestore.
- `firestore.rules` trong repo **chưa hoàn thiện**: hiện từ chối cập nhật đơn hàng và ghi lương. Không deploy lên dự án đang chạy cho đến khi đã hoàn thiện và kiểm thử Emulator.
- Chưa xác nhận build Android và kiểm thử tích hợp thực tế.
- Chỉ dùng tài khoản và dữ liệu giả lập; không đưa CCCD hoặc thông tin nhạy cảm thật vào repo.
- Chủ sở hữu cần thu hồi API key ImgBB cũ tại nhà cung cấp; sửa lịch sử Git không làm key cũ mất hiệu lực.
