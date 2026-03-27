package com.newland.anti.fraud;

import com.newland.anti.fraud.model.Order;
import com.newland.anti.fraud.model.OrderResult;
import com.newland.anti.fraud.service.OrderProcessingService;
import com.newland.anti.fraud.service.RuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OrderProcessingTest {
    @Autowired
    private OrderProcessingService orderProcessingService;

    @Autowired
    private RuleService ruleService;

    @Test
    public void testDiscountCalculation() {
        Order order = createTestOrder("VIP", new BigDecimal("1500"));
        Order result = ruleService.applyDiscounts(order);

        assertNotNull(result);
        assertTrue(result.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(new BigDecimal("1150.00"), result.getFinalAmount());
    }

    @Test
    public void testOrderProcessing() {
        Order order = createTestOrder("GOLD", new BigDecimal("800"));
        OrderResult result = orderProcessingService.processOrder(order);

        assertNotNull(result);
        assertTrue(result.isApproved());
        assertEquals(new BigDecimal("680.00"), result.getFinalAmount());
    }

    private Order createTestOrder(String tier, BigDecimal amount) {
        Order order = new Order();
        order.setOrderId("TEST-001");
        order.setCustomerId("CUST-001");
        order.setCustomerName("测试用户");
        order.setCustomerTier(tier);
        order.setTotalAmount(amount);

        Order.OrderItem item = new Order.OrderItem();
        item.setProductId("P001");
        item.setProductName("测试产品");
        item.setQuantity(1);
        item.setUnitPrice(amount);
        item.setSubtotal(amount);

        order.setItems(Arrays.asList(item));

        return order;
    }
}