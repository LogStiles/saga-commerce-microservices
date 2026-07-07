package com.saga.order;

import com.saga.outbox.OutboxConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Import(OutboxConfig.class)
@EntityScan("com.saga.order")
public class OrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}

}
