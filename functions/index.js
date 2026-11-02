const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();

// Only the trusted backend sends FCM. Do not put service-account keys in Android.
exports.notifyShippersOnNewOrder = onDocumentCreated("ORDERS/{orderId}", async event => {
  const order = event.data?.data();
  if (!order || !["PENDING", "WAITING"].includes(order.status)) return;
  if (order.shipperId) return;
  const orderId = event.params.orderId;
  const snapshot = await getFirestore().collection("USERS")
    .where("role", "==", "SHIPPER").where("status", "==", "ACTIVE").get();
  const tokens = [...new Set(snapshot.docs.map(d => d.get("fcmToken")).filter(Boolean))];
  for (let i = 0; i < tokens.length; i += 500) {
    const batch = tokens.slice(i, i + 500);
    const response = await getMessaging().sendEachForMulticast({
      tokens: batch,
      data: {type: "NEW_ORDER", orderId}
    });
    response.responses.forEach((result, index) => {
      if (!result.success) console.error("FCM delivery failed", batch[index].slice(0, 8), result.error?.code);
    });
  }
});
