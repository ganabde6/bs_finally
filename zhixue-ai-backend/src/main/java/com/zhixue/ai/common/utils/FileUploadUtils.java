package com.zhixue.ai.common.utils;

import com.zhixue.ai.common.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传工具
 */
@Component
public class FileUploadUtils {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.allowed-types}")
    private String allowedTypes;

    /**
     * 上传文件,返回相对路径
     */
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String suffix = originalName == null ? "" :
                originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        List<String> allowed = Arrays.asList(allowedTypes.split(","));
        if (!allowed.contains(suffix)) {
            throw new BizException("不支持的文件类型: " + suffix);
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        File dir = new File(uploadDir + File.separator + datePath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BizException("创建上传目录失败");
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + suffix;
        try {
            file.transferTo(new File(dir, fileName));
        } catch (IOException e) {
            throw new BizException("文件上传失败: " + e.getMessage());
        }
        return "/upload/" + datePath + "/" + fileName;
    }
}
