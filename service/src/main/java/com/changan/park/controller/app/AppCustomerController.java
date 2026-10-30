package com.changan.park.controller.app;

import com.changan.common.domain.R;
import com.changan.model.dto.AppCustomerUpdateDTO;
import com.changan.park.service.ICustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/app/customer")
@RequiredArgsConstructor
@Tag(name = "客户相关接口")
public class AppCustomerController {

    private final ICustomerService customerService;

    @PutMapping("/me")
    @Operation(summary = "修改信息")
    public R<Void> updateMyInfo(@RequestBody @Valid AppCustomerUpdateDTO dto) {
        customerService.updateMyInfo(dto);
        return R.ok();
    }
}
