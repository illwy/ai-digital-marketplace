package com.aidigital.marketplace.shared.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aidigital.marketplace.shared.web.ApiException;
import com.aidigital.marketplace.shared.web.DataResponse;

/**
 * 管理端图片上传。仅接受白名单扩展名的图片，落盘为随机文件名，
 * 通过 /api/v1/uploads/** 静态公开访问（商品详情图文用）。
 */
@RestController
@RequestMapping("/api/v1/admin/uploads")
public class UploadController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "gif");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final Path uploadDir;

    public UploadController(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DataResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UPLOAD_EMPTY", "请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UPLOAD_TOO_LARGE", "图片不能超过 5MB");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UPLOAD_TYPE_UNSUPPORTED", "仅支持 png/jpg/webp/gif 图片");
        }
        if (file.getContentType() != null && !file.getContentType().toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ApiException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UPLOAD_TYPE_UNSUPPORTED", "仅支持上传图片文件");
        }

        String stored = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Files.createDirectories(uploadDir);
        file.transferTo(uploadDir.resolve(stored).toFile());
        return new DataResponse<>(Map.of("url", "/api/v1/uploads/" + stored));
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
