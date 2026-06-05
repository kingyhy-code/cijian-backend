package com.cijian.operation.controller;

import com.cijian.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/upload")
public class FileController {

    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Path UPLOAD_DIR = Paths.get("uploads").toAbsolutePath().normalize();

    static {
        try { Files.createDirectories(UPLOAD_DIR); } catch (IOException ignored) {}
    }

    @PostMapping("/image")
    public R<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return R.error(400, "文件为空");
        if (file.getSize() > MAX_SIZE) return R.error(400, "文件不能超过5MB");

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return R.error(400, "仅支持图片格式");
        }

        try {
            String ext = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + ext;
            Path dest = UPLOAD_DIR.resolve(filename);
            file.transferTo(dest.toFile());

            String url = "/uploads/" + filename;
            log.info("Image uploaded: {}", url);
            return R.success("上传成功", Map.of("url", url));
        } catch (IOException e) {
            log.error("Upload failed", e);
            return R.error(500, "上传失败");
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int i = filename.lastIndexOf('.');
        return i > 0 ? filename.substring(i).toLowerCase() : ".jpg";
    }
}
