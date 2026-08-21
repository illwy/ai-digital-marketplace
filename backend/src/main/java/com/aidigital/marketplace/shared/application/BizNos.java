package com.aidigital.marketplace.shared.application;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class BizNos {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private BizNos() {}

    public static String next(String prefix) {
        int suffix = ThreadLocalRandom.current().nextInt(10000);
        return prefix + LocalDateTime.now().format(FORMAT) + String.format("%04d", suffix);
    }
}
