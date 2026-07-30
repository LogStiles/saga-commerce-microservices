package com.saga.order.messaging;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

@Configuration
public class KafkaConfig {

    @Bean
    ConsumerFactory<String, InventoryEvent> inventoryConsumerFactory(KafkaProperties props) {
        var valueDeserializer = new JacksonJsonDeserializer<>(InventoryEvent.class, false);
        var keyDeserializer = new StringDeserializer();
        return new DefaultKafkaConsumerFactory<>(props.buildConsumerProperties(), keyDeserializer, valueDeserializer);
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
        var valueDeserializer = new JacksonJsonDeserializer<>(PaymentEvent.class, false);
        var keyDeserializer = new StringDeserializer();
        return new DefaultKafkaConsumerFactory<>(props.buildConsumerProperties(), keyDeserializer, valueDeserializer);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> paymentKLCFactory(KafkaProperties props) {
        ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentConsumerFactory(props));
        factory.setConcurrency(props.getListener().getConcurrency());
        return factory;
    }
}
