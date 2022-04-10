package com.github.adetiamarhadi.orderservice;

import com.github.adetiamarhadi.sagacore.config.AxonConfig;
import org.axonframework.config.Configuration;
import org.axonframework.config.ConfigurationScopeAwareProvider;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.SimpleDeadlineManager;
import org.axonframework.spring.messaging.unitofwork.SpringTransactionManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@EnableDiscoveryClient
@SpringBootApplication
@Import({AxonConfig.class})
public class OrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}

	@Bean
	public DeadlineManager deadlineManager(Configuration configuration,
										   SpringTransactionManager springTransactionManager) {
		return SimpleDeadlineManager.builder()
				.scopeAwareProvider(new ConfigurationScopeAwareProvider(configuration))
				.transactionManager(springTransactionManager)
				.build();
	}

}
