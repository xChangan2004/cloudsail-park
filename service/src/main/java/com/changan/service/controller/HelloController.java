package com.changan.service.controller;

import com.changan.common.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
@Tag(name = "测试")
public class HelloController {

    @Operation(summary = "你好")
    @GetMapping
    public R<String> hello() {
        return R.ok("hello world");
    }
}
