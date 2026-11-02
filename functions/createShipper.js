const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {getFirestore, FieldValue} = require("firebase-admin/firestore");
const {getAuth} = require("firebase-admin/auth");

exports.createShipper = onCall(async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Vui lòng đăng nhập");
  const db = getFirestore();
  const caller = await db.collection("USERS").doc(request.auth.uid).get();
  if (!caller.exists || caller.get("role") !== "ADMIN" || caller.get("status") !== "ACTIVE") {
    throw new HttpsError("permission-denied", "Chỉ quản trị viên đang hoạt động được cấp tài khoản");
  }
  const input = request.data || {};
  const required = ["fullName", "phone", "email", "password", "status", "citizenId", "licensePlate", "address"];
  if (required.some(k => typeof input[k] !== "string" || !input[k].trim())) {
    throw new HttpsError("invalid-argument", "Thiếu thông tin nhân viên");
  }
  if (!["ACTIVE", "INACTIVE"].includes(input.status) ||
      input.password.length < 6 || input.password.length > 128 ||
      input.email.length > 254 || input.fullName.length > 150 ||
      input.phone.length > 30 || input.citizenId.length > 50 ||
      input.licensePlate.length > 50 || input.address.length > 500 ||
      (input.avatarUrl != null && (typeof input.avatarUrl !== "string" || input.avatarUrl.length > 2048))) {
    throw new HttpsError("invalid-argument", "Thông tin nhân viên không hợp lệ");
  }

  const auth = getAuth();
  let created;
  try {
    created = await auth.createUser({
      email: input.email.trim(), password: input.password,
      displayName: input.fullName.trim(), disabled: input.status !== "ACTIVE"
    });
    const uid = created.uid;
    const ref = db.collection("USERS").doc(uid);
    const batch = db.batch();
    batch.set(ref, {
      fullName: input.fullName.trim(), phone: input.phone.trim(),
      email: input.email.trim(), role: "SHIPPER", status: input.status,
      avatarUrl: input.avatarUrl || "", createdAt: FieldValue.serverTimestamp()
    });
    batch.set(ref.collection("SHIPPER_DETAILS").doc(uid), {
      shipperId: uid, citizenId: input.citizenId.trim(),
      licensePlate: input.licensePlate.trim(), address: input.address.trim(),
      joinDate: FieldValue.serverTimestamp()
    });
    await batch.commit();
    return {uid};
  } catch (err) {
    if (created) {
      try { await auth.deleteUser(created.uid); }
      catch (cleanupError) { console.error("Cleanup failed", cleanupError.code); }
    }
    if (err.code === "auth/email-already-exists") {
      throw new HttpsError("already-exists", "Email đã tồn tại");
    }
    console.error("Create shipper failed", err.code);
    throw new HttpsError("internal", "Không thể cấp tài khoản");
  }
});
