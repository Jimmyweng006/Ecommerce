package com.jimmyweng.ecommerce.constant;

public final class ErrorMessages {

    private ErrorMessages() {
    }

    public static final String RESOURCE_MODIFIED = "Resource was modified by another request";
    public static final String AUTHENTICATION_FAILED = "Authentication failed";
    public static final String ACCESS_DENIED = "Access is denied";
    public static final String VALIDATION_FAILED = "Validation failed";
    public static final String UNEXPECTED_ERROR = "Unexpected error";
    private static final String PRODUCT_NOT_FOUND_PREFIX = "Product not found: ";
    private static final String OUT_OF_STOCK_PREFIX = "Product out of stock: ";
    private static final String USER_NOT_FOUND_PREFIX = "User not found: ";
    private static final String ORDER_NOT_FOUND_PREFIX = "Order not found: ";

    public static String productNotFound(long productId) {
        return PRODUCT_NOT_FOUND_PREFIX + productId;
    }

    public static String outOfStock(long productId) {
        return OUT_OF_STOCK_PREFIX + productId;
    }

    public static String userNotFound(String email) {
        return USER_NOT_FOUND_PREFIX + email;
    }

    public static String orderNotFound(long orderId) {
        return ORDER_NOT_FOUND_PREFIX + orderId;
    }
}
