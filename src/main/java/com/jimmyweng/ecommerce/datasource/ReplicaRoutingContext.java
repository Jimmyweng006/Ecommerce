package com.jimmyweng.ecommerce.datasource;

public final class ReplicaRoutingContext {

    private static final ThreadLocal<Boolean> FORCE_PRIMARY = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private ReplicaRoutingContext() {}

    public static void forcePrimary() {
        FORCE_PRIMARY.set(Boolean.TRUE);
    }

    public static void clear() {
        FORCE_PRIMARY.remove();
    }

    public static boolean isForcePrimary() {
        return Boolean.TRUE.equals(FORCE_PRIMARY.get());
    }
}
