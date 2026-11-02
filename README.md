# Ứng dụng quản lý giao hàng

Ứng dụng Android hỗ trợ quản lý sản phẩm, tạo đơn và theo dõi quá trình giao hàng. Hệ thống được xây dựng với ba vai trò: quản trị viên (Admin), người tạo đơn (Creator) và nhân viên giao hàng (Shipper).

Dự án sử dụng **Java và Firebase**, tập trung vào xử lý nghiệp vụ theo vai trò, lưu trữ dữ liệu trên Firestore và tổ chức mã nguồn theo hướng MVVM.

## Chức năng chính

| Vai trò | Chức năng |
| --- | --- |
| **Admin** | Quản lý tài khoản Creator/Shipper, sản phẩm, đơn hàng; theo dõi thống kê và xác nhận thanh toán lương |
| **Creator** | Quản lý sản phẩm của mình, tạo đơn, xem trạng thái và hủy đơn khi chưa được nhận |
| **Shipper** | Xem đơn đang chờ, nhận đơn, xác nhận giao thành công, xem thu nhập và cập nhật hồ sơ |

Đơn hàng đi qua các trạng thái chính: **Chờ nhận → Đang giao → Hoàn thành**. Creator có thể hủy đơn khi đơn vẫn đang chờ nhận.

Màn hình thông báo hiện hiển thị dữ liệu đơn hàng từ Firestore, chưa tích hợp thông báo đẩy FCM.

## Công nghệ sử dụng

- **Ngôn ngữ:** Java
- **Giao diện:** Android XML, Material Components, RecyclerView
- **Kiến trúc:** ViewModel, LiveData, Repository (áp dụng cho các phần đã tái cấu trúc)
- **Dữ liệu và xác thực:** Firebase Authentication, Cloud Firestore
- **Thư viện:** Glide, OkHttp, osmdroid (OpenStreetMap)
- **Kiểm thử:** JUnit, Mockito, Firebase Emulator

## Cấu trúc dữ liệu

Các collection chính trên Cloud Firestore:

| Collection | Mục đích |
| --- | --- |
| `USERS` | Thông tin tài khoản và vai trò |
| `USERS/{uid}/SHIPPER_DETAILS` | Hồ sơ bổ sung của Shipper |
| `PRODUCTS` | Sản phẩm |
| `ORDERS` | Đơn hàng và trạng thái giao |
| `ORDERS/{orderId}/ORDER_DETAILS` | Chi tiết sản phẩm trong đơn |
| `SALARIES` | Tổng hợp thu nhập theo tháng của Shipper |

## Cài đặt và chạy thử

**Yêu cầu:** Android Studio, môi trường Android SDK phù hợp và một dự án Firebase dùng để thử nghiệm.

1. Sao chép repository và mở bằng Android Studio.
2. Bật **Authentication (Email/Password)** và **Cloud Firestore** trong Firebase Console.
3. Tải `google-services.json` từ dự án Firebase của bạn và đặt tại `app/google-services.json`.
4. Nếu sử dụng chức năng tải ảnh lên ImgBB, thêm dòng sau vào `local.properties`:

   ```properties
   IMGBB_API_KEY=YOUR_API_KEY
   ```

5. Khởi tạo tài khoản quản trị theo [hướng dẫn cấp tài khoản](docs/account-provisioning.md).
6. Đồng bộ Gradle, build và chạy ứng dụng trên thiết bị hoặc máy ảo Android.

Không sử dụng dữ liệu cá nhân thật khi thử nghiệm.

## Kiểm thử

Chạy unit test trên Windows:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Kiểm thử Firestore Security Rules bằng Emulator (cần Node.js, Java và Firebase CLI):

```powershell
npm --prefix tests/firestore install
firebase emulators:exec --only firestore "npm --prefix tests/firestore test"
```

Các bài kiểm thử có trong repository nhưng **chưa được xác nhận chạy thành công sau những thay đổi gần đây**.

## Tình trạng dự án

Dự án phục vụ học tập và xây dựng portfolio, chưa triển khai cho người dùng thực tế. Một số màn hình vẫn thao tác trực tiếp với Firestore thay vì đi qua Repository.

Quy tắc phân quyền được lưu trong [`firestore.rules`](firestore.rules), nhưng cần kiểm thử bằng Emulator trước khi áp dụng. Phần tính và cập nhật lương hiện thực hiện từ ứng dụng Android, chưa có backend đáng tin cậy để xác minh toàn bộ giao dịch.

## Ghi chú

- Admin đầu tiên cần được tạo thủ công trong Firebase Console; các tài khoản Creator/Shipper được cấp từ ứng dụng.
- API key ImgBB từng xuất hiện trong lịch sử Git. Key cũ cần được thu hồi tại ImgBB; không nên tái sử dụng.
- Ảnh chụp các màn hình ứng dụng sẽ được bổ sung sau khi hoàn tất kiểm thử giao diện.
