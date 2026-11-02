# Ứng dụng quản lý giao hàng (Android Java)

Ứng dụng trình diễn quản lý giao hàng nội bộ cho **Admin**, **Creator** và **Shipper**. Dự án thực hành Android, **chưa phải ứng dụng môi trường thực tế**.

## Chức năng

| Vai trò | Chức năng |
| --- | --- |
| Admin | Quản lý nhân viên, sản phẩm, đơn hàng, lương và thống kê |
| Creator | Tạo, theo dõi và hủy đơn đủ điều kiện |
| Shipper | Xem đơn chờ nhận, nhận đơn, cập nhật giao hàng và xem thu nhập |

**Thông báo:** màn hình thông báo trong ứng dụng đọc danh sách đơn đang chờ từ Firestore. **Không sử dụng FCM, thông báo đẩy hay Cloud Functions.**

**Cấp tài khoản:** không có tự đăng ký. Admin đầu tiên được khởi tạo thủ công bằng Firebase Console. Sau đó Admin có thể dùng màn hình Thêm Creator/Shipper để tạo tài khoản Firebase Authentication qua phiên Auth phụ và ghi hồ sơ `USERS/{uid}` vào Firestore. Đây chỉ là luồng trình diễn phía client, không phải giải pháp cấp quyền môi trường thực tế. Xem [hướng dẫn](docs/account-provisioning.md).

## Công nghệ và kiến trúc

- Java, Android XML, AndroidX, ViewModel/LiveData, mẫu Repository (đang được áp dụng từng phần)
- Firebase Authentication, Cloud Firestore
- Glide, OkHttp, osmdroid / OpenStreetMap
- JUnit, Mockito và Firebase Firestore Emulator kiểm thử quy tắc bảo mật

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

Trạng thái tài khoản: `ACTIVE` / `INACTIVE` (dữ liệu cũ `LOCKED` cần được chuyển đổi trước khi áp dụng Rules mới).\n\nTrạng thái đơn chính: `PENDING` (hoặc dữ liệu cũ `WAITING`), `DELIVERING`, `COMPLETED`, `CANCELLED`.

## Chạy ứng dụng

1. Sao chép kho mã nguồn, mở thư mục bằng Android Studio.
2. Tạo hoặc chọn dự án Firebase thử nghiệm, bật **Email/Password Authentication** và Firestore.
3. Đặt file `google-services.json` đúng vị trí `app/google-services.json` theo dự án Firebase của bạn.
4. Nếu dùng chức năng tải ảnh lên ImgBB, thêm `IMGBB_API_KEY=YOUR_KEY` vào `local.properties` (không commit).
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

Các test được thêm vào mã nguồn nhưng **chưa được thực thi/xác nhận đạt** trong môi trường Android/Firebase thật. Rules tại `firestore.rules` đã bổ sung quyền cho luồng đơn hàng, lương, hồ sơ và sản phẩm, nhưng **chưa được kiểm thử đầy đủ**. Không áp dụng trực tiếp lên dự án Firebase đang sử dụng trước khi chạy Emulator và kiểm thử tích hợp. Việc tính lương ở máy khách còn giới hạn bảo mật, không thể xác thực hoàn toàn chỉ bằng Rules.

## Giới hạn và bảo mật

- Firebase Spark có thể dùng cho trình diễn theo giới hạn miễn phí; không cần Cloud Functions.
- Giao diện ẩn đăng ký không thay thế được Firestore Security Rules.
- Key ImgBB cũ từng nằm trong Git history; **chủ tài khoản phải thu hồi trực tiếp tại ImgBB**. Không thể xác nhận đã thu hồi chỉ từ repository. `BuildConfig` không bảo vệ bí mật khỏi việc trích xuất APK.
- Chưa xác nhận build và kiểm thử từ đầu đến cuối sau những thay đổi gần đây.
- Khi Shipper hoàn thành đơn, logic trình diễn cộng `deliveryFee` vào tổng lương tháng; không đại diện cho quy trình lương thực tế.

## Hình ảnh

Ảnh chụp màn hình chưa được thêm. Chỉ bổ sung ảnh chụp từ app đã chạy, không dùng ảnh giả lập tính năng chưa hoạt động.

## Mục tiêu dự án

Thực hành xây dựng ứng dụng Android đa vai trò, quản lý dữ liệu Firestore, tổ chức mã theo MVVM/Repository và kiểm thử phân quyền.
