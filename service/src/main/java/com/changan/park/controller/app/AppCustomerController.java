package com.changan.park.controller.app;

import com.changan.common.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/customer")
@RequiredArgsConstructor
@Tag(name = "客户相关接口")
public class AppCustomerController {

    @GetMapping("/me")
    @Operation(summary = "查看我的信息")
    public R<Void> getMyInfo() {
        return R.ok();
    }

    @PutMapping("/me")
    @Operation(summary = "修改信息")
    public R<Void> updateMyInfo() {
        return R.ok();
    }
}
