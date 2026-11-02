# Account setup without Cloud Functions

This CV/demo application uses Firebase Authentication and Cloud Firestore on the client, with **no Cloud Functions dependency**. Public self-registration and in-app account provisioning are disabled.

## Add an account manually (project administrator only)

1. Open the Firebase Console for the correct project.
2. In **Authentication > Users**, add a user with email and password. Copy the generated **UID**.
3. In **Firestore Database**, create a document in collection `USERS` with **document ID exactly equal to that UID**.
4. Add these fields using the appropriate Firestore types:
   - `fullName`: string
   - `email`: string (same email as Auth)
   - `phone`: string
   - `role`: string, exactly `ADMIN`, `CREATOR`, or `SHIPPER`
   - `status`: string, exactly `ACTIVE`
   - `avatarUrl`: string (empty is fine)
   - `createdAt`: timestamp
5. For Shippers, also create `USERS/{uid}/SHIPPER_DETAILS/{uid}` with fields `shipperId` (string UID), `citizenId` (string), `licensePlate` (string), `address` (string), and `joinDate` (timestamp).
6. Log in using the email and password in the Android app.

Never store passwords in Firestore. Never commit credentials or personal identity details to GitHub.

## Security warning

**Hiding the registration screen does not secure Firestore.** Firestore Security Rules must enforce that clients cannot create or change privileged roles or statuses. Existing Firebase Rules must be audited and tested with the Firebase Emulator before the app is used with real user data. Firebase Console administrative changes bypass client Rules.

This setup is intended for a controlled demonstration with test accounts. Do not use real citizen IDs or other sensitive personal data in demo records.

## Known limitations

- Creating accounts is done manually in Firebase Console, not in the Android app.
- Android build and end-to-end login tests still need to be run locally.
- Old ImgBB API keys must be revoked separately in the ImgBB dashboard.
