package com.saga.order;

import com.saga.outbox.OutboxConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(OutboxConfig.class)
@EntityScan({"com.saga.order", "com.saga.outbox"})
public class OrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}

}
