package com.saga.outbox;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@Configuration
@EntityScan("com.saga.outbox")
public class OutboxConfig {

}
