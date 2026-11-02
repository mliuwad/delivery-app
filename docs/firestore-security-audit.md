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
