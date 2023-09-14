package com.danshop.products;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class MDCUtils {
    public static final String MDC_USER = "dan-user";
    public static final String MDC_APPLICATION_NAME = "application-name";
}
