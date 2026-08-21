package com.aidigital.marketplace.shared.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.function.Supplier;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidigital.marketplace.shared.infrastructure.entity.IdempotencyEntity;
import com.aidigital.marketplace.shared.infrastructure.mapper.IdempotencyMapper;
import com.aidigital.marketplace.shared.web.ApiException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class IdempotencyService {

    private final IdempotencyMapper mapper;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public String hashBody(Object body) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(body == null ? "" : body);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(json));
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Transactional
    public <T> T run(String key, Object requestBody, Class<T> type, Supplier<T> action) {
        if (key == null || key.isBlank()) {
            return action.get();
        }
        String hash = hashBody(requestBody);
        IdempotencyEntity existing = mapper.selectOne(
                new LambdaQueryWrapper<IdempotencyEntity>().eq(IdempotencyEntity::getIdempotencyKey, key));
        if (existing != null) {
            return replay(existing, hash, type);
        }
        IdempotencyEntity record = new IdempotencyEntity();
        record.setIdempotencyKey(key);
        record.setRequestHash(hash);
        record.setStatus("IN_PROGRESS");
        record.setCreatedAt(LocalDateTime.now());
        try {
            mapper.insert(record);
        } catch (DuplicateKeyException ex) {
            IdempotencyEntity raced = mapper.selectOne(
                    new LambdaQueryWrapper<IdempotencyEntity>().eq(IdempotencyEntity::getIdempotencyKey, key));
            return replay(raced, hash, type);
        }
        try {
            T result = action.get();
            record.setStatus("SUCCEEDED");
            record.setResponseBody(objectMapper.writeValueAsString(result));
            mapper.updateById(record);
            return result;
        } catch (RuntimeException ex) {
            mapper.deleteById(record.getId());
            throw ex;
        } catch (JsonProcessingException ex) {
            mapper.deleteById(record.getId());
            throw new IllegalStateException(ex);
        }
    }

    private <T> T replay(IdempotencyEntity existing, String hash, Class<T> type) {
        if (existing == null) {
            throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_IN_PROGRESS", "相同请求正在处理");
        }
        if (!existing.getRequestHash().equals(hash)) {
            throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", "幂等键已用于不同请求");
        }
        if ("IN_PROGRESS".equals(existing.getStatus()) || existing.getResponseBody() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_IN_PROGRESS", "相同请求正在处理");
        }
        try {
            return objectMapper.readValue(existing.getResponseBody(), type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
