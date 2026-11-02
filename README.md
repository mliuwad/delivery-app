# Delivery Management App (Android Java)

Ứng dụng demo quản lý giao hàng nội bộ cho **Admin**, **Creator** và **Shipper**. Dự án thực hành Android, **chưa phải ứng dụng production**.

## Chức năng

| Vai trò | Chức năng |
| --- | --- |
| Admin | Quản lý nhân viên, sản phẩm, đơn hàng, lương và thống kê |
| Creator | Tạo, theo dõi và hủy đơn đủ điều kiện |
| Shipper | Xem đơn chờ nhận, nhận đơn, cập nhật giao hàng và xem thu nhập |

**Thông báo:** màn hình thông báo trong ứng dụng đọc danh sách đơn đang chờ từ Firestore. **Không sử dụng FCM, push notification hay Cloud Functions.**

**Cấp tài khoản:** không có tự đăng ký. Người quản trị dự án tạo tài khoản trong Firebase Authentication và hồ sơ `USERS/{uid}` bằng Firebase Console. Nút thêm nhân viên trong app chỉ hiển thị hướng dẫn; không tạo tài khoản. Xem [hướng dẫn](docs/account-provisioning.md).

## Công nghệ và kiến trúc

- Java, Android XML, AndroidX, ViewModel/LiveData, Repository Pattern (đang được áp dụng từng phần)
- Firebase Authentication, Cloud Firestore
- Glide, OkHttp, osmdroid / OpenStreetMap
- JUnit, Mockito và Firebase Firestore Emulator rules tests

```text
Activity / Fragment (UI)
        |
     ViewModel
        |
     Repository
        |
 Firebase Auth / Firestore
```

Một số màn hình vẫn truy cập Firestore trực tiếp; kiến trúc chưa được chuẩn hóa hoàn toàn.

## Cấu trúc Firestore

```text
USERS/{uid}                            role, status, fullName, email, phone...
  SHIPPER_DETAILS/{uid}                 shipperId, licensePlate, address...
PRODUCTS/{productId}                   thông tin sản phẩm
ORDERS/{orderId}                       creatorId, shipperId, status, deliveryFee...
  ORDER_DETAILS/{detailId}             sản phẩm và số lượng
SALARIES/{salaryId}                    shipperID, month, year, totalOrders, totalSalary...
```

Trạng thái đơn chính: `PENDING` (hoặc dữ liệu cũ `WAITING`), `DELIVERING`, `COMPLETED`, `CANCELLED`.

## Chạy ứng dụng

1. Clone repo, mở thư mục bằng Android Studio.
2. Tạo hoặc chọn Firebase project thử nghiệm, bật **Email/Password Authentication** và Firestore.
3. Đặt file `google-services.json` đúng vị trí `app/google-services.json` theo Firebase project của bạn.
4. Nếu dùng chức năng upload ảnh ImgBB, thêm `IMGBB_API_KEY=YOUR_KEY` vào `local.properties` (không commit).
5. Tạo tài khoản thử nghiệm theo [hướng dẫn cấp tài khoản](docs/account-provisioning.md).
6. Gradle Sync, **Build > Make Project**, sau đó chạy app.

Không đưa thông tin đăng nhập, khóa dịch vụ hoặc giấy tờ cá nhân thật vào repo.

## Kiểm thử

**JUnit (10 bài test OrderViewModel):**
```powershell
.\gradlew.bat :app:testDebugUnitTest
```

**Firestore Emulator (11 bài test allow/deny):** yêu cầu Node.js, Java và Firebase CLI. Chạy ở thư mục gốc repo:

```powershell
npm --prefix tests/firestore install
firebase emulators:exec --only firestore "npm --prefix tests/firestore test"
```

Các test được thêm vào mã nguồn nhưng **chưa được thực thi/xác nhận đạt** trong môi trường Android/Firebase thật. Rules tại `firestore.rules` là **bản bảo thủ đang kiểm thử**, không deploy thẳng lên project đang sử dụng: cập nhật trạng thái đơn và ghi lương hiện bị từ chối theo mặc định. Cần hoàn thiện các quy tắc chuyển trạng thái, giao dịch lương và truy vấn trước khi triển khai.

## Giới hạn và bảo mật

- Firebase Spark có thể dùng cho demo theo giới hạn miễn phí; không cần Cloud Functions.
- Giao diện ẩn đăng ký không thay thế được Firestore Security Rules.
- Key ImgBB cũ từng nằm trong Git history; **chủ tài khoản phải revoke trực tiếp tại ImgBB**. Không thể xác nhận đã revoke chỉ từ repository. `BuildConfig` không bảo vệ bí mật khỏi việc trích xuất APK.
- Chưa xác nhận build và kiểm thử end-to-end sau những thay đổi gần đây.
- Khi Shipper hoàn thành đơn, logic demo cộng `deliveryFee` vào tổng lương tháng; không đại diện cho quy trình lương thực tế.

## Hình ảnh

Ảnh chụp màn hình chưa được thêm. Chỉ bổ sung ảnh chụp từ app đã chạy, không dùng ảnh giả lập tính năng chưa hoạt động.

## Mục tiêu dự án

Thực hành xây dựng ứng dụng Android đa vai trò, quản lý dữ liệu Firestore, tổ chức mã theo MVVM/Repository và kiểm thử phân quyền.
