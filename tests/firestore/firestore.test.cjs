const {test, before, after, beforeEach} = require("node:test");
const fs = require("node:fs");
const assert = require("node:assert/strict");
const {initializeTestEnvironment, assertSucceeds, assertFails} =
  require("@firebase/rules-unit-testing");

let env;
before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-delivery-rules",
    firestore: {host: "127.0.0.1", port: 8085, rules: fs.readFileSync("firestore.rules", "utf8")}
  });
});
after(async () => { if (env) await env.cleanup(); });
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async context => {
    const db = context.firestore();
    await db.doc("USERS/admin").set({role:"ADMIN",status:"ACTIVE",email:"admin@test.local",createdAt:1,fullName:"Admin"});
    await db.doc("USERS/creator").set({role:"CREATOR",status:"ACTIVE",email:"creator@test.local",createdAt:1});
    await db.doc("USERS/shipper").set({role:"SHIPPER",status:"ACTIVE",email:"shipper@test.local",createdAt:1});
    await db.doc("USERS/locked").set({role:"SHIPPER",status:"INACTIVE",email:"locked@test.local",createdAt:1});
    await db.doc("ORDERS/order1").set({creatorId:"creator",shipperId:"",status:"PENDING"});
    await db.doc("PRODUCTS/p1").set({name:"Box"});
  });
});
const db = uid => env.authenticatedContext(uid).firestore();
test("admin reads users", async () => {
  await assertSucceeds(db("admin").doc("USERS/creator").get());
});
test("creator cannot list all users", async () => {
  await assertFails(db("creator").collection("USERS").get());
});
test("creator cannot promote itself to admin", async () => {
  await assertFails(db("creator").doc("USERS/creator").update({role:"ADMIN"}));
});
test("admin cannot change role on existing account", async () => {
  await assertFails(db("admin").doc("USERS/creator").update({role:"ADMIN"}));
});
test("admin can lock existing user", async () => {
  await assertSucceeds(db("admin").doc("USERS/creator").update({status:"INACTIVE"}));
});
test("creator can read products", async () => {
  await assertSucceeds(db("creator").doc("PRODUCTS/p1").get());
});
test("shipper cannot edit products", async () => {
  await assertFails(db("shipper").doc("PRODUCTS/p1").update({name:"Changed"}));
});
test("creator can create own pending order", async () => {
  await assertSucceeds(db("creator").doc("ORDERS/new").set({creatorId:"creator",shipperId:"",status:"PENDING"}));
});
test("shipper cannot create an order", async () => {
  await assertFails(db("shipper").doc("ORDERS/new").set({creatorId:"shipper",shipperId:"",status:"PENDING"}));
});
test("inactive user cannot read products", async () => {
  await assertFails(db("locked").doc("PRODUCTS/p1").get());
});
test("unauthenticated user cannot read products", async () => {
  await assertFails(env.unauthenticatedContext().firestore().doc("PRODUCTS/p1").get());
});
