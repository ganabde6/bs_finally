package com.zhixue.ai.module.system.controller;

import com.zhixue.ai.common.result.Result;
import com.zhixue.ai.common.utils.FileUploadUtils;
import com.zhixue.ai.module.system.entity.SysFile;
import com.zhixue.ai.module.system.mapper.SysFileMapper;
import com.zhixue.ai.module.system.service.SystemService;
import com.zhixue.ai.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 公共接口(文件上传、基础数据查询)
 */
@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
public class CommonController {

    private final FileUploadUtils fileUploadUtils;
    private final SysFileMapper fileMapper;
    private final SystemService systemService;

    /** 文件上传 */
    @PostMapping("/upload")
    public Result<SysFile> upload(@RequestParam("file") MultipartFile file) {
        String path = fileUploadUtils.upload(file);
        SysFile sysFile = new SysFile();
        sysFile.setFileName(file.getOriginalFilename());
        sysFile.setFilePath(path);
        sysFile.setFileSize(file.getSize());
        sysFile.setFileType(file.getContentType());
        sysFile.setUploaderId(SecurityUtils.getCurrentUserId());
        fileMapper.insert(sysFile);
        return Result.success(sysFile);
    }

    /** 学科列表（支持按学段过滤） */
    @GetMapping("/subjects")
    public Result<?> subjects(@RequestParam(required = false) Integer gradeLevel) {
        if (gradeLevel != null && gradeLevel > 0) {
            return Result.success(systemService.listSubjectsByGradeLevel(gradeLevel));
        }
        return Result.success(systemService.listSubjects());
    }

    /** 班级列表（支持按学段过滤） */
    @GetMapping("/classes")
    public Result<?> classes(@RequestParam(required = false) Integer gradeLevel) {
        if (gradeLevel != null && gradeLevel > 0) {
            return Result.success(systemService.listClassesByGradeLevel(gradeLevel));
        }
        return Result.success(systemService.listClasses());
    }

    /** 角色列表 */
    @GetMapping("/roles")
    public Result<?> roles() {
        return Result.success(systemService.listRoles());
    }
}
