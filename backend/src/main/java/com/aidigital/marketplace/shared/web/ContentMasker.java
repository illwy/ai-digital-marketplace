package com.aidigital.marketplace.shared.web;

public final class ContentMasker {

    private ContentMasker() {}

    public static String mask(String content) {
        if (content == null || content.isBlank()) {
            return "****";
        }
        if (content.length() <= 4) {
            return "****";
        }
        return content.substring(0, 2) + "****" + content.substring(content.length() - 2);
    }
}
