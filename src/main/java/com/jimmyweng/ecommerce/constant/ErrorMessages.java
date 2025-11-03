package com.jimmyweng.ecommerce.constant;

public final class ErrorMessages {

    private ErrorMessages() {}

    public static final String RESOURCE_MODIFIED = "Resource was modified by another request";
    public static final String AUTHENTICATION_FAILED = "Authentication failed";
    public static final String ACCESS_DENIED = "Access is denied";
    public static final String VALIDATION_FAILED = "Validation failed";
    public static final String UNEXPECTED_ERROR = "Unexpected error";
    private static final String PRODUCT_NOT_FOUND_PREFIX = "Product not found: ";

    public static String productNotFound(long productId) {
        return PRODUCT_NOT_FOUND_PREFIX + productId;
    }
}
