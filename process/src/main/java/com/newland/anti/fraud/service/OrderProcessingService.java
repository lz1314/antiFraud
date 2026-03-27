package com.newland.anti.fraud.service;

import com.newland.anti.fraud.model.OrderResult;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.utils.KieHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.newland.anti.fraud.model.Order;
import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderProcessingService.class);

    private KieSession kieSession;
    private Map<String, ProcessInstance> activeProcesses = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            // 手动创建KieSession，不使用自动生成
            KieHelper kieHelper = new KieHelper();

            // 加载BPMN2流程
            kieHelper.addResource(ResourceFactory.newClassPathResource("processes/order-process.bpmn2"));

            // 加载DRL规则文件
            kieHelper.addResource(ResourceFactory.newClassPathResource("rules/order-discount.drl"));

            KieBase kieBase = kieHelper.build();
            kieSession = kieBase.newKieSession();

            // 注册工作项处理程序
            registerWorkItemHandlers();

            if(LOGGER.isInfoEnabled()){
                LOGGER.info("Kogito引擎初始化成功");
            }
        } catch (Exception e) {
            LOGGER.error("Kogito引擎初始化失败", e);
            throw new RuntimeException("Kogito引擎初始化失败", e);
        }
    }

    private void registerWorkItemHandlers() {
        // 注册自定义工作项处理器
        kieSession.getWorkItemManager().registerWorkItemHandler("BusinessRuleTask",
                new org.kie.api.runtime.process.WorkItemHandler() {
                    @Override
                    public void executeWorkItem(org.kie.api.runtime.process.WorkItem workItem,
                                                org.kie.api.runtime.process.WorkItemManager manager) {
                        try {
                            if(LOGGER.isInfoEnabled()){
                                LOGGER.info("执行业务规则任务: {}", workItem.getName());
                            }
                            // 业务规则任务已经在规则引擎中处理
                            manager.completeWorkItem(workItem.getId(), workItem.getParameters());
                        } catch (Exception e) {
                            LOGGER.error("执行业务规则任务失败", e);
                            manager.abortWorkItem(workItem.getId());
                        }
                    }

                    @Override
                    public void abortWorkItem(org.kie.api.runtime.process.WorkItem workItem,
                                              org.kie.api.runtime.process.WorkItemManager manager) {
                        manager.abortWorkItem(workItem.getId());
                    }
                });
    }

    public OrderResult processOrder(Order order) {
        if(LOGGER.isInfoEnabled()) {
            LOGGER.info("开始处理订单: {}", order.getOrderId());
        }
        try {
            // 设置初始状态
            if (order.getOrderId() == null) {
                order.setOrderId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            }

            order.setStatus("PENDING");

            // 创建流程实例变量
            Map<String, Object> params = new HashMap<>();
            params.put("order", order);

            // 启动流程
            ProcessInstance processInstance = kieSession.startProcess("order-process", params);
            String processId = processInstance.getId();
            activeProcesses.put(processId, processInstance);
            if(LOGGER.isInfoEnabled()) {
                LOGGER.info("流程实例启动成功: {}", processId);

            }
            // 执行规则
            kieSession.fireAllRules();

            // 获取更新后的订单数据
            Order updatedOrder = (Order) kieSession.getGlobal("order");
            if (updatedOrder == null) {
                updatedOrder = order;
            }

            // 构建结果
            OrderResult result = new OrderResult();
            result.setOrderId(updatedOrder.getOrderId());
            result.setProcessInstanceId(processId);
            result.setOriginalAmount(updatedOrder.getTotalAmount());
            result.setDiscountAmount(updatedOrder.getDiscountAmount());
            result.setFinalAmount(updatedOrder.getFinalAmount());
            result.setStatus(updatedOrder.getStatus());

            if ("PROCESSED".equals(updatedOrder.getStatus())) {
                result.setApproved(true);
                result.setMessage("订单处理成功");
            } else if ("INVALID".equals(updatedOrder.getStatus())) {
                result.setApproved(false);
                result.setMessage("订单验证失败");
            } else {
                result.setApproved(false);
                result.setMessage("订单状态: " + updatedOrder.getStatus());
            }
            if(LOGGER.isInfoEnabled()) {
                LOGGER.info("订单处理完成: {}", result);
            }

            return result;

        } catch (Exception e) {
            LOGGER.error("处理订单失败", e);
            throw new RuntimeException("处理订单失败", e);
        }
    }

    public OrderResult getOrderStatus(String processInstanceId) {
        ProcessInstance processInstance = activeProcesses.get(processInstanceId);
        if (processInstance == null) {
            throw new RuntimeException("流程实例不存在: " + processInstanceId);
        }

        OrderResult result = new OrderResult();
        result.setProcessInstanceId(processInstanceId);
        result.setStatus(processInstance.getState() == ProcessInstance.STATE_COMPLETED ? "COMPLETED" : "ACTIVE");

        return result;
    }

    public void dispose() {
        if (kieSession != null) {
            kieSession.dispose();
        }
    }
}