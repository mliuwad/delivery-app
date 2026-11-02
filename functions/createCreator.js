const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {getAuth} = require("firebase-admin/auth");
const {getFirestore, FieldValue} = require("firebase-admin/firestore");

exports.createCreator = onCall(async request => {
  if (!request.auth) throw new HttpsError("unauthenticated", "Vui lòng đăng nhập");
  const db = getFirestore();
  const admin = await db.collection("USERS").doc(request.auth.uid).get();
  if (!admin.exists || admin.get("role") !== "ADMIN" || admin.get("status") !== "ACTIVE") {
    throw new HttpsError("permission-denied", "Chỉ Admin đang hoạt động được tạo tài khoản");
  }
  const data = request.data || {};
  const {fullName, phone, email, password} = data;
  if ([fullName, phone, email, password].some(v => typeof v !== "string") ||
      !fullName.trim() || fullName.length > 150 ||
      !/^0[0-9]{9}$/.test(phone) ||
      email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) ||
      password.length < 6 || password.length > 128) {
    throw new HttpsError("invalid-argument", "Thông tin tài khoản không hợp lệ");
  }
  const auth = getAuth();
  let user;
  try {
    user = await auth.createUser({email: email.trim(), password, displayName: fullName.trim()});
    await db.collection("USERS").doc(user.uid).set({
      fullName: fullName.trim(), phone, email: email.trim(), role: "CREATOR",
      status: "ACTIVE", avatarUrl: "", createdAt: FieldValue.serverTimestamp()
    });
    return {uid: user.uid};
  } catch (error) {
    if (user) {
      try { await auth.deleteUser(user.uid); }
      catch (cleanup) { console.error("Creator cleanup failed", cleanup.code); }
    }
    if (error.code === "auth/email-already-exists") {
      throw new HttpsError("already-exists", "Email đã tồn tại");
    }
    console.error("Creator creation failed", error.code);
    throw new HttpsError("internal", "Không thể tạo tài khoản Creator");
  }
});
