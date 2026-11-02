# Account provisioning and rollout

## Policy
Public self-registration is disabled for all roles. Existing Admin accounts must be provisioned out of band by a trusted project owner. The Android login screen hides the registration link and RegistrationRepository refuses self-registration.

## Creator and Shipper creation
The Admin UI calls the Firebase callable functions `createCreator` and `createShipper` using the signed-in Admin session. The function checks `USERS/{callerUid}` for `role == ADMIN` and `status == ACTIVE`, creates an Auth user via Admin SDK, then batch-writes `USERS/{uid}` and `USERS/{uid}/SHIPPER_DETAILS/{uid}`. It attempts to remove the Auth user if profile creation fails.

## Deployment prerequisites
1. Confirm the correct Firebase project, billing requirements, and IAM access.
2. Install function dependencies in `functions/` and deploy `createCreator,createShipper` to a **test project** first.
3. Confirm Android and Functions use the same Firebase project and region.
4. Test successful creation by active Admin and denial for inactive Admin, Creator, Shipper, and unauthenticated users.
5. Verify duplicate email and invalid data errors; verify failed creation does not leave an orphaned user.
6. Design and test Firestore Rules with the Emulator before production deployment. Never allow arbitrary clients to write `role` or `status` on `USERS`.

## Remaining work
- Admin-only Creator provisioning is implemented in code but has not been deployed or verified. Public registration remains disabled.
- Existing admin account provisioning and recovery must be defined before onboarding new installations.
- Firebase Functions, Android build, and Emulator tests have not been executed in this repository change.
- Review sensitive identity data in `SHIPPER_DETAILS`; minimize retention and enforce access controls.
- Revoke the old ImgBB API key in the ImgBB dashboard; removing it from Git history alone does not revoke it.

## Commands for test deployment (from project root)

```powershell
cd functions
npm install
cd ..
firebase use <YOUR_TEST_PROJECT_ID>
firebase deploy --only functions:createCreator,functions:createShipper
```

**Do not deploy untested Firestore Rules.** The callable functions use Firebase Admin SDK, which bypasses Firestore Rules. Android clients still require tested, role-based rules for their direct reads/writes. Use a test Firebase project, not production, for initial verification.

## Smoke test after deploying functions

1. Login as an existing active Admin and create one Creator and one Shipper.
2. Verify both profiles appear in `USERS` with correct role and status; verify Shipper details exist.
3. Login with each newly created account and check the correct home screen.
4. Attempt account creation while signed in as Creator or Shipper; expect permission denied.
5. Lock a Shipper in the Admin UI and verify login is denied by application role/status checks; unlock and verify access returns.
6. Check Firebase logs for errors without printing passwords or identity numbers.
