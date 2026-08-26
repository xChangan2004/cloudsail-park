package com.changan.admin.controller;

import com.changan.admin.service.IParkingOrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/parking/order")
@Tag(name = "停车场订单相关接口")
@RequiredArgsConstructor
public class ParkingOrderController {

    private final IParkingOrderService parkingOrderService;


}
