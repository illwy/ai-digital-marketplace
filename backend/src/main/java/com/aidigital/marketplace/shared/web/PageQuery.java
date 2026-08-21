package com.aidigital.marketplace.shared.web;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public final class PageQuery {

    private PageQuery() {}

    public static <T> Page<T> of(int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        return new Page<>(safePage, safeSize);
    }
}
