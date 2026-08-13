package com.saga.outbox;

import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

/**
 * OutboxConfig is the object we give to SpringBootApplications. Owns and encapsulates the OutboxEventDispatcher bean.
 */
@Configuration
@EntityScan("com.saga.outbox")
public class OutboxConfig {

    @Bean
    OutboxEventDispatcher outboxEventDispatcher(EntityManager entityManager) {
        return new OutboxEventDispatcher(entityManager, false); 
        // removeAfterInsert is set to false for debugging by default. If this were a legitimate production product you would change this to true.
    }
}
