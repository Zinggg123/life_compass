package com.zing.compass.controller;

import com.zing.compass.entity.OrderInfo;
import com.zing.compass.service.OrderService;
import com.zing.compass.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public Result createOrder(OrderInfo orderInfo) {
        try {
            orderInfo = orderService.makeOrder(orderInfo);
            return Result.success("Order created successfully", orderInfo);
        } catch (IllegalArgumentException e) {
            return Result.failure(e.getMessage());
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
