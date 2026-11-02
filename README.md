# Delivery Management Android App

Ứng dụng Android quản lý giao hàng nội bộ, xây dựng bằng Java/XML và Firebase.

## Vai trò
- **Admin:** quản lý nhân viên, sản phẩm, đơn hàng, lương và thống kê.
- **Creator:** tạo và theo dõi đơn giao hàng.
- **Shipper:** nhận đơn, cập nhật trạng thái giao hàng, xem thu nhập và hồ sơ.

## Công nghệ
Android (Java, XML), Firebase Authentication, Cloud Firestore, Firebase Cloud Messaging, Glide, OkHttp, OpenStreetMap (osmdroid).

## Chạy dự án
1. Clone repository và mở bằng Android Studio.
2. Cấu hình Firebase cho dự án của riêng bạn; không dùng tài khoản production để thử nghiệm.
3. Thêm `IMGBB_API_KEY=YOUR_NEW_KEY` vào `local.properties` ở thư mục gốc (file này đã được gitignore).
4. Sync Gradle, build và chạy trên thiết bị/emulator.

**Bảo mật:** API key ImgBB từng xuất hiện trong lịch sử Git; cần thu hồi key cũ trên ImgBB. Đọc key qua `BuildConfig` chỉ loại bỏ hardcode khỏi mã nguồn, **không** giấu được key trong APK. Cân nhắc upload qua backend trước khi phát hành. Không tự triển khai Firestore Rules khi chưa thử bằng Firebase Emulator và xác nhận tất cả luồng nghiệp vụ.

## Dữ liệu chính
`USERS`, `PRODUCTS`, `ORDERS` (chi tiết tại `ORDERS/{orderId}/ORDER_DETAILS`), `SALARIES`.

## Quy tắc tính lương hiện tại
Khi Shipper hoàn thành đơn, luồng hiện tại cộng phí giao hàng (`deliveryFee`) vào dữ liệu lương theo tháng. Đây là quy tắc của bản demo, không phải bảng lương doanh nghiệp thực tế.

## Kiểm thử cần thực hiện
- Admin tạo/khóa/mở khóa tài khoản và kiểm tra đăng nhập.
- Creator tạo/hủy đơn; kiểm tra tổng tiền, COD và danh sách chi tiết.
- Hai Shipper đồng thời nhận cùng một đơn: chỉ một người thành công.
- Shipper hoàn thành đơn: chỉ cộng lương một lần.
- Kiểm tra phân quyền Firestore từ client không đáng tin cậy.
- Thử giao diện khi bàn phím mở, đặc biệt thanh điều hướng dưới.

## Trạng thái
Dự án thực hành, đang tiếp tục hoàn thiện phân quyền Firestore, kiểm thử tự động, phân trang và cấu hình phát hành. Chưa có xác nhận build/kiểm thử end-to-end cho mọi thay đổi mới nhất.
