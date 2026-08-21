package com.aidigital.marketplace.shared.web;

public record ErrorBody(String code, String message, Object details) {
}
