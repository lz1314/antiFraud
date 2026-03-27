package com.newland.anti.fraud.service;

import com.newland.anti.fraud.model.Order;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.utils.KieHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class RuleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuleService.class);

    private KieSession kieSession;

    @PostConstruct
    public void init() {
        try {
            // 创建仅包含规则的KieSession
            KieHelper kieHelper = new KieHelper();
            kieHelper.addResource(ResourceFactory.newClassPathResource("rules/order-discount.drl"));

            KieBase kieBase = kieHelper.build();
            kieSession = kieBase.newKieSession();

            if(LOGGER.isInfoEnabled()){
                LOGGER.info("规则引擎初始化成功");
            }
        } catch (Exception e) {
            LOGGER.error("规则引擎初始化失败", e);
            throw new RuntimeException("规则引擎初始化失败", e);
        }
    }

    public Order applyDiscounts(Order order) {
        if(LOGGER.isInfoEnabled()){
            LOGGER.info("应用折扣规则: {}", order.getOrderId());
        }
        try {

            // 复制订单对象
            Order workingOrder = copyOrder(order);

            // 插入事实
            kieSession.insert(workingOrder);

            // 执行规则
            int firedRules = kieSession.fireAllRules();
            if(LOGGER.isInfoEnabled()) {
                LOGGER.info("执行了 {} 条规则", firedRules);
            }
            return workingOrder;

        } catch (Exception e) {
            LOGGER.error("应用折扣规则失败", e);
            throw new RuntimeException("应用折扣规则失败", e);
        }
    }

    private Order copyOrder(Order original) {
        Order copy = new Order();
        copy.setOrderId(original.getOrderId());
        copy.setCustomerId(original.getCustomerId());
        copy.setCustomerName(original.getCustomerName());
        copy.setCustomerTier(original.getCustomerTier());
        copy.setTotalAmount(original.getTotalAmount());
        copy.setDiscountAmount(BigDecimal.ZERO);
        copy.setFinalAmount(BigDecimal.ZERO);
        copy.setStatus("PENDING");

        if (original.getItems() != null) {
            List<Order.OrderItem> items = new ArrayList<>();
            for (Order.OrderItem item : original.getItems()) {
                Order.OrderItem newItem = new Order.OrderItem();
                newItem.setProductId(item.getProductId());
                newItem.setProductName(item.getProductName());
                newItem.setQuantity(item.getQuantity());
                newItem.setUnitPrice(item.getUnitPrice());
                newItem.setSubtotal(item.getSubtotal());
                items.add(newItem);
            }
            copy.setItems(items);
        }

        return copy;
    }
}