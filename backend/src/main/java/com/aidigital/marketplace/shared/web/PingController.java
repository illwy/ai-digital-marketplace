package com.aidigital.marketplace.shared.web;

import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PingController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public PingController(JdbcTemplate jdbcTemplate, StringRedisTemplate stringRedisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Integer mysql = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        stringRedisTemplate.opsForValue().set("marketplace:ping", "ok");
        String redis = stringRedisTemplate.opsForValue().get("marketplace:ping");
        return Map.of(
                "app", "ok",
                "mysql", mysql != null && mysql == 1 ? "ok" : "fail",
                "redis", "ok".equals(redis) ? "ok" : "fail");
    }
}
