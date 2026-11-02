# Rà soát bảo mật Cloud Firestore

Tài liệu ghi nhận cách phân quyền dữ liệu của ứng dụng quản lý giao hàng, những điểm cần kiểm thử và các giới hạn còn tồn tại. Quy tắc hiện tại nằm tại [`firestore.rules`](../firestore.rules).

**Trạng thái:** đã có quy tắc cho các nghiệp vụ chính, nhưng chưa xác nhận toàn bộ bằng Firebase Emulator hoặc kiểm thử tích hợp. Việc cập nhật mã nguồn trên GitHub không đồng nghĩa với triển khai quy tắc lên Firebase.

## Phân quyền theo vai trò

| Dữ liệu | Quyền truy cập dự kiến |
| --- | --- |
| `USERS/{uid}` | Người dùng xem hồ sơ của mình; Admin quản lý tài khoản theo các trường được cho phép |
| `USERS/{uid}/SHIPPER_DETAILS/{uid}` | Admin và Shipper sở hữu hồ sơ được truy cập theo quy tắc |
| `PRODUCTS/{productId}` | Creator quản lý sản phẩm do mình tạo; Admin có quyền quản trị theo quy tắc hiện tại |
| `ORDERS/{orderId}` | Creator tạo/hủy đơn của mình; Shipper nhận đơn chờ và cập nhật đơn được giao; Admin xem và quản lý theo quyền hiện có |
| `ORDERS/{orderId}/ORDER_DETAILS/{detailId}` | Chỉ người có quyền xem đơn hàng tương ứng được đọc chi tiết |
| `SALARIES/{salaryId}` | Shipper xem lương của mình; Admin xem và xác nhận thanh toán |

Các điều kiện cấp quyền dựa trên `request.auth.uid`, vai trò và trạng thái tài khoản trong Firestore. Trường `role` không được phép tự ý sửa từ hồ sơ cá nhân.

## Các luồng cần kiểm thử

### Tài khoản

- Admin đầu tiên được khởi tạo thủ công trong Firebase Console.
- Admin tạo tài khoản Creator/Shipper từ ứng dụng bằng phiên Firebase Authentication phụ.
- Kiểm tra việc tạo hồ sơ `USERS` và `SHIPPER_DETAILS`, đặc biệt khi thao tác ghi bị từ chối hoặc thực hiện không đầy đủ.
- Tài khoản bị khóa không được thực hiện các thao tác yêu cầu trạng thái `ACTIVE`.

**Giới hạn:** việc cấp tài khoản đặc quyền từ ứng dụng phía máy khách không thay thế được quy trình xác thực và cấp quyền ở máy chủ. Không cho phép người dùng tự tạo hồ sơ `ADMIN` để giải quyết lỗi phân quyền.

### Sản phẩm

- Creator chỉ cập nhật sản phẩm của mình.
- Trường xác định người tạo sản phẩm không được thay đổi sau khi tạo.
- Kiểm tra truy vấn danh sách sản phẩm có lọc theo chủ sở hữu.
- Kiểm tra thao tác thêm, sửa và ẩn sản phẩm.

### Đơn hàng

- Creator tạo đơn ở trạng thái chờ và chỉ hủy khi đơn chưa được nhận.
- Shipper nhận đơn chờ, sau đó chỉ hoàn thành đơn đã được gán cho mình.
- Kiểm tra các trạng thái `PENDING`, `WAITING` (dữ liệu cũ), `DELIVERING`, `COMPLETED` và `CANCELLED`.
- Kiểm tra trường nào được thay đổi ở mỗi bước chuyển trạng thái.
- Kiểm tra việc ghi `ORDER_DETAILS` cùng đơn hàng bằng thao tác ghi theo lô.
- Kiểm tra hai Shipper nhận cùng một đơn tại cùng thời điểm.

### Lương

- Shipper chỉ đọc bản ghi lương của mình.
- Khi hoàn thành đơn, ứng dụng cập nhật trạng thái đơn và dữ liệu lương trong giao dịch Firestore.
- Admin xác nhận thanh toán; Shipper không được tự đánh dấu đã thanh toán.
- Kiểm tra truy vấn lương theo `shipperID`, tháng và năm.

**Giới hạn quan trọng:** số tiền lương hiện được tính và ghi từ ứng dụng Android. Firestore Rules có thể giới hạn trường và điều kiện cập nhật, nhưng chưa đủ để xác minh tuyệt đối một khoản lương có tương ứng với một đơn giao thành công hay không. Muốn bảo vệ nghiệp vụ này ở mức triển khai thực tế cần xử lý tại máy chủ đáng tin cậy.

## Danh sách kiểm tra trước khi triển khai

- [ ] Chạy bộ kiểm thử Firestore Emulator và xác nhận các trường hợp cho phép/từ chối.
- [ ] Kiểm thử truy cập chéo giữa hai tài khoản Creator hoặc Shipper.
- [ ] Kiểm thử tài khoản chưa đăng nhập và tài khoản bị khóa.
- [ ] Kiểm thử tạo tài khoản, sản phẩm, nhận đơn, hoàn thành đơn và xác nhận lương trên ứng dụng.
- [ ] Kiểm tra dữ liệu cũ dùng trạng thái `LOCKED` hoặc `WAITING`.
- [ ] Xác nhận API key ImgBB cũ đã được thu hồi tại nhà cung cấp.
- [ ] Chỉ triển khai quy tắc lên dự án Firebase đang sử dụng sau khi các luồng cần thiết đã được kiểm thử.

## Tài liệu liên quan

- [Hướng dẫn cấp tài khoản](account-provisioning.md)
- [Quy tắc Firestore hiện tại](../firestore.rules)
- [README của dự án](../README.md)
