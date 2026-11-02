# Firestore security audit (draft — not deployed)

This document tracks security requirements for the Android delivery app. **Do not deploy permissive catch-all rules.** The repository does not currently include a verified Firestore Emulator test suite.

## Collections and role boundaries

- `USERS/{uid}`: authenticated user can read their own profile; admin can administer profiles. Never permit ordinary clients to change `role`, privileged `status`, or create an `ADMIN` profile. Provision administrator roles from a trusted backend / Firebase Admin SDK.
- `PRODUCTS/{productId}`: creators can create and update their own products, with immutable `createdBy`; admin access needs separate explicit policy. The existing `ProductRepository.addProduct()` scans all PRODUCTS to allocate a sequential ID, which conflicts with owner-scoped list rules and risks duplicate IDs.
- `ORDERS/{orderId}`: creators create and cancel their own pending orders; shippers claim pending orders and complete only assigned deliveries. Accept legacy `WAITING` for reads/transitions. Restrict field changes by transition.
- `ORDERS/{orderId}/ORDER_DETAILS/{detailId}`: permit access only to participants or administrators, subject to query and batch-write behavior.
- `SALARIES/{salaryId}`: shipper reads their own salary; admin handles payment. Completion currently writes salary and order in a client transaction, so rules must validate the paired changes or move salary accounting to a trusted server.

## Verified code dependencies

1. `OrderRepository.getOrdersForShipper()` queries pending orders without a shipper filter and queries assigned orders by `shipperId`.
2. `OrderRepository.getOrdersByCreator()` filters by `creatorId`; admin `getOrders()` may query all orders.
3. `OrderRepository.executeOrderAction()` updates status, shipper assignment, and salary within Firestore transactions.
4. `ProductRepository.getProducts(ownerUid, isAdmin)` uses owner-scoped query for non-admin users.
5. `ProductRepository.addProduct()` reads the entire PRODUCTS collection before writing.

## Before deployment

- Inventory every direct Firestore access in activities, fragments, repositories, and viewmodels.
- Design immutable role provisioning and locked-account checks.
- Replace sequential ID scans with collision-safe IDs.
- Define and test exact permitted changes for order transitions and salary accounting.
- Write emulator allow/deny tests for all three roles, anonymous clients, cross-user reads/writes, and legacy orders.
- Test Android flows against emulator; only then deploy restrictive rules to production.

**Status:** audit draft only; no Firebase rules have been deployed.

## Critical finding: shipper account provisioning

`CreateShipperRepository.create()` creates a secondary Firebase Auth session for the new shipper, then writes `USERS/{newUid}` and `SHIPPER_DETAILS/{newUid}` using that new shipper's credentials. Firestore Rules cannot establish that the original admin authorized this operation from the new user's authentication context. **Do not allow arbitrary client-created role documents as a workaround.**

Required redesign: call a trusted backend (for example an authenticated Firebase Callable Cloud Function) as the existing admin. Verify admin role and active status server-side, create the Firebase Auth user via Admin SDK, and write the shipper profile and detail documents with server privileges. Return only non-sensitive success/error information. Consider compensating cleanup if provisioning fails. Keep credentials out of logs.

## Other observed dependencies

- `OrderRepository.createOrder()` writes the order and its `ORDER_DETAILS` subcollection in a batch. Child rules must be compatible with the parent document's state after the batch.
- `OrderRepository.prepareCompletion()` queries `SALARIES` by `shipperID`, `month`, and `year`; completion updates the salary record in a transaction. Query restrictions and write validation must account for both.
- Client-supplied `userId` arguments must never be trusted by security rules; use `request.auth.uid` for authorization.
- Review salary payment status updates separately from delivery completion; admin-only payment transitions should not be writable by shippers.

## Updated implementation order

1. Replace privileged account provisioning with a trusted server-side flow.
2. Inventory remaining direct Firestore operations, including salary management and profile updates.
3. Define collection rules and emulator tests together, including cross-user and privilege-escalation denials.
4. Verify full Admin/Creator/Shipper flows in the emulator before deploying.
