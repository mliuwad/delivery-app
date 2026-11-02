package com.example.udgh.utils;

/** Shared Firestore collection names, user roles and order states. */
public final class AppConstants {
    private AppConstants() {}

    public static final class Collections {
        private Collections() {}
        public static final String USERS = "USERS";
        public static final String ORDERS = "ORDERS";
        public static final String ORDER_DETAILS = "ORDER_DETAILS";
        public static final String PRODUCTS = "PRODUCTS";
        public static final String SALARIES = "SALARIES";
    }

    public static final class Roles {
        private Roles() {}
        public static final String ADMIN = "ADMIN";
        public static final String CREATOR = "CREATOR";
        public static final String SHIPPER = "SHIPPER";
    }

    public static final class AccountStatus {
        private AccountStatus() {}
        public static final String ACTIVE = "ACTIVE";
        public static final String INACTIVE = "INACTIVE";
    }

    public static final class OrderStatuses {
        private OrderStatuses() {}
        public static final String PENDING = "PENDING";
        /** Legacy pending state: keep reading it until old documents are migrated. */
        public static final String WAITING = "WAITING";
        public static final String DELIVERING = "DELIVERING";
        public static final String COMPLETED = "COMPLETED";
        public static final String CANCELLED = "CANCELLED";
    }
}
