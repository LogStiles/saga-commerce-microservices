package com.saga.order.messaging;

import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.LinkedHashMap;

@Configuration
public class KafkaConfig {

    //ErrorHandlingDeserializer prevents malformed/garbage Kafka messages from crashing the service
    //Errors are handled by our kafkaErrorHandler @Bean found at the bottom of the class
    @Bean
    ConsumerFactory<String, InventoryEvent> inventoryConsumerFactory(KafkaProperties props) {
        var valueDeserializer = new ErrorHandlingDeserializer<>(new JacksonJsonDeserializer<>(InventoryEvent.class, false));
        var keyDeserializer = new ErrorHandlingDeserializer<>(new StringDeserializer());
        return new DefaultKafkaConsumerFactory<>(props.buildConsumerProperties(), keyDeserializer, valueDeserializer);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, InventoryEvent> inventoryKLCFactory(
            KafkaProperties props, DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, InventoryEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(inventoryConsumerFactory(props));
        factory.setConcurrency(props.getListener().getConcurrency());
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    @Bean
    ConsumerFactory<String, PaymentEvent> paymentConsumerFactory(KafkaProperties props) {
        var valueDeserializer = new ErrorHandlingDeserializer<>(new JacksonJsonDeserializer<>(PaymentEvent.class, false));
        var keyDeserializer = new ErrorHandlingDeserializer<>(new StringDeserializer());
        return new DefaultKafkaConsumerFactory<>(props.buildConsumerProperties(), keyDeserializer, valueDeserializer);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> paymentKLCFactory(
            KafkaProperties props, DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentConsumerFactory(props));
        factory.setConcurrency(props.getListener().getConcurrency());
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    // A failed *deserialization* recovers the raw bytes, but a failed *listener* recovers the
    // already-deserialized object, so the DLT producer has to handle both. byte[] is matched
    // first; anything else falls through to JSON. A byte[]-only template throws
    // ClassCastException on listener failures, leaving the record stuck in an endless retry.
    @Bean
    KafkaTemplate<String, Object> deadLetterKafkaTemplate(KafkaProperties props) {
        var delegates = new LinkedHashMap<Class<?>, Serializer<?>>();
        delegates.put(byte[].class, new ByteArraySerializer());
        delegates.put(Object.class, new JacksonJsonSerializer<>());

        var producerFactory = new DefaultKafkaProducerFactory<String, Object>(
                props.buildProducerProperties(), new StringSerializer(),
                new DelegatingByTypeSerializer(delegates, true));
        return new KafkaTemplate<>(producerFactory);
    }

    //Handles errors for the ConcurrentKafkaListenerContainer. A safety net not just for Kafka-related errors, but any kind of exception thrown in its call-stack
    //Retries twice before passing it off to DLT handling.
    //Bounded retries prevent the consumer's poll loop from going over max.poll.interval.ms, going over would stop polling and the broker would think the consumer is dead, which would cause endless rebalances
    //Retry budget only applies to listener failures, parsing errors on malformed jsons are automatically sent to DeadLetterPublishingRecoverer
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> deadLetterKafkaTemplate) {
        var recoverer = new DeadLetterPublishingRecoverer(deadLetterKafkaTemplate);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2));
    }
}
