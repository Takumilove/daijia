package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.driver.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "上传管理接口")
@RestController
@RequestMapping("file")
@RequiredArgsConstructor
public class FileController {
    private final CosService cosService;
    private final FileService fileService;

    // @Operation(summary = "上传")
    // @PostMapping("/upload")
    // public Result<String> upload(@RequestPart("file") MultipartFile file,
    //                              @RequestParam(name = "path", defaultValue = "auth") String path) {
    //     CosUploadVo cosUploadVo = cosService.uploadFile(file, path);
    //     String showUrl = cosUploadVo.getShowUrl();
    //     return Result.ok(showUrl);
    // }

    @Operation(summary = "上传")
    @PostMapping("/upload")
    public Result<String> upload(@RequestPart("file") MultipartFile file) {
        String url = fileService.upload(file);
        return Result.ok(url);
    }
}
