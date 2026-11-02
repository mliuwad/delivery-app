# Account provisioning and rollout

## Policy
Public self-registration is disabled for all roles. Existing Admin accounts must be provisioned out of band by a trusted project owner. The Android login screen hides the registration link and RegistrationRepository refuses self-registration.

## Shipper creation
The Admin UI calls the Firebase callable function `createShipper` using the signed-in Admin session. The function checks `USERS/{callerUid}` for `role == ADMIN` and `status == ACTIVE`, creates an Auth user via Admin SDK, then batch-writes `USERS/{uid}` and `USERS/{uid}/SHIPPER_DETAILS/{uid}`. It attempts to remove the Auth user if profile creation fails.

## Deployment prerequisites
1. Confirm the correct Firebase project, billing requirements, and IAM access.
2. Install function dependencies in `functions/` and deploy `createShipper` to a **test project** first.
3. Confirm Android and Functions use the same Firebase project and region.
4. Test successful creation by active Admin and denial for inactive Admin, Creator, Shipper, and unauthenticated users.
5. Verify duplicate email and invalid data errors; verify failed creation does not leave an orphaned user.
6. Design and test Firestore Rules with the Emulator before production deployment. Never allow arbitrary clients to write `role` or `status` on `USERS`.

## Remaining work
- Admin-only Creator provisioning has not been implemented; the old public registration flow is disabled.
- Existing admin account provisioning and recovery must be defined before onboarding new installations.
- Firebase Functions, Android build, and Emulator tests have not been executed in this repository change.
- Review sensitive identity data in `SHIPPER_DETAILS`; minimize retention and enforce access controls.
- Revoke the old ImgBB API key in the ImgBB dashboard; removing it from Git history alone does not revoke it.
