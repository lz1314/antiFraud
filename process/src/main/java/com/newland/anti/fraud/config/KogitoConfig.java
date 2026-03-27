package com.newland.anti.fraud.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

@Configuration
public class KogitoConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(KogitoConfig.class);


    @Bean
    public KieContainer kieContainer() {
        try {
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

            // 手动添加所有规则文件
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] drlResources = resolver.getResources("classpath*:/rules/**/*.drl");
            Resource[] bpmnResources = resolver.getResources("classpath*:/processes/**/*.bpmn2");

            for (Resource resource : drlResources) {
                kieFileSystem.write(ResourceFactory.newClassPathResource(
                        resource.getURL().getPath().substring(resource.getURL().getPath().indexOf("rules/"))));
            }

            for (Resource resource : bpmnResources) {
                kieFileSystem.write(ResourceFactory.newClassPathResource(
                        resource.getURL().getPath().substring(resource.getURL().getPath().indexOf("processes/"))));
            }

            KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem).buildAll();
            Results results = kieBuilder.getResults();

            if (results.hasMessages(Message.Level.ERROR)) {
                results.getMessages(Message.Level.ERROR).forEach(message ->
                        LOGGER.error("规则编译错误: {}", message.getText()));
                throw new RuntimeException("规则编译失败");
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("KieContainer初始化成功");
            }
            return kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());

        } catch (IOException e) {
            LOGGER.error("加载规则文件失败", e);
            throw new RuntimeException("加载规则文件失败", e);
        }
    }
}