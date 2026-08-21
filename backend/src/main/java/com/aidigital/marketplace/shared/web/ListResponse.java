package com.aidigital.marketplace.shared.web;

import java.util.List;

public record ListResponse<T>(List<T> data, Pagination pagination) {
}
