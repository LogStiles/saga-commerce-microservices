package com.saga.order.messaging;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.core.ConsumerFactory;

@Configuration
public class KafkaConfig {

    @Bean
    ConsumerFactory<String, InventoryEvent> inventoryConsumerFactory(KafkaProperties props) {
        return new DefaultKafkaConsumerFactory<>(props.buildConsumerProperties(null), new StringDeserializer(), new JsonDeserializer<>(InventoryEvent.class));
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, InventoryEvent> inventoryKLCFactory(KafkaProperties props) {
        ConcurrentKafkaListenerContainerFactory<String, InventoryEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(inventoryConsumerFactory(props));
        factory.setConcurrency(props.getListener().getConcurrency());
        return factory;
    }

    @Bean
    ConsumerFactory<String, PaymentEvent> paymentConsumerFactory(KafkaProperties props) {
        return new DefaultKafkaConsumerFactory<>(props.buildConsumerProperties(null), new StringDeserializer(), new JsonDeserializer<>(PaymentEvent.class));
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> paymentKLCFactory(KafkaProperties props) {
        ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentConsumerFactory(props));
        factory.setConcurrency(props.getListener().getConcurrency());
        return factory;
    }
}
