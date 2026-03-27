package com.newland.anti.fraud.controller;

import com.newland.anti.fraud.model.Order;
import com.newland.anti.fraud.model.OrderResult;
import com.newland.anti.fraud.service.OrderProcessingService;
import com.newland.anti.fraud.service.RuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderProcessingService orderProcessingService;

    @Autowired
    private RuleService ruleService;

    @PostMapping("/process")
    public ResponseEntity<OrderResult> processOrder(@Valid @RequestBody Order order) {
        try {
            OrderResult result = orderProcessingService.processOrder(order);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("处理订单请求失败", e);
            OrderResult errorResult = new OrderResult();
            errorResult.setStatus("ERROR");
            errorResult.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    @PostMapping("/discount")
    public ResponseEntity<Order> calculateDiscount(@Valid @RequestBody Order order) {
        try {
            Order result = ruleService.applyDiscounts(order);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("计算折扣失败", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/status/{processInstanceId}")
    public ResponseEntity<OrderResult> getOrderStatus(@PathVariable String processInstanceId) {
        try {
            OrderResult result = orderProcessingService.getOrderStatus(processInstanceId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("获取订单状态失败", e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testOrder() {
        // 创建测试订单
        Order order = new Order();
        order.setOrderId("TEST-001");
        order.setCustomerId("CUST-001");
        order.setCustomerName("张三");
        order.setCustomerTier("VIP");
        order.setTotalAmount(new BigDecimal("1500.00"));

        // 创建订单项
        Order.OrderItem item1 = new Order.OrderItem();
        item1.setProductId("P001");
        item1.setProductName("笔记本电脑");
        item1.setQuantity(1);
        item1.setUnitPrice(new BigDecimal("1200.00"));
        item1.setSubtotal(new BigDecimal("1200.00"));

        Order.OrderItem item2 = new Order.OrderItem();
        item2.setProductId("P002");
        item2.setProductName("鼠标");
        item2.setQuantity(2);
        item2.setUnitPrice(new BigDecimal("150.00"));
        item2.setSubtotal(new BigDecimal("300.00"));

        order.setItems(java.util.Arrays.asList(item1, item2));

        Map<String, Object> response = new HashMap<>();
        response.put("order", order);

        try {
            Order discountedOrder = ruleService.applyDiscounts(order);
            response.put("discountedOrder", discountedOrder);

            OrderResult processedOrder = orderProcessingService.processOrder(order);
            response.put("processedOrder", processedOrder);

            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}