package com.saga.inventory.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
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

import lombok.Setter;

/**
 * KafkaConfig serves multiple purposes.
 * It creates our two inventory-service's Kafka topics using properties defined in application.properties.
 * It builds the consumer pipeline used by ItemOrderInboxEventConsumer to handle incoming ItemOrderEventPayload objects.
 * It builds the DLT pipeline with the beans deadLetterKafkaTemplate and kafkaErrorHandler.
 */
@Configuration
@EnableKafka
class KafkaConfig {

    @Setter
    public static class KafkaTopic {
        private String name;
        private int partitions;
        private short replicas;
    }

    @Bean
    @ConfigurationProperties(prefix = "kafka.topic.inbox.events") //prefix refers to a property in application.properties
    KafkaTopic inventoryInboxTopicProps() {
        return new KafkaTopic(); //use Lombok @Setter to populate the fields of the KafkaTopic with values defined in application.properties
    }

    @Bean
    @ConfigurationProperties(prefix = "kafka.topic.outbox.events") //prefix refers to a property in application.properties
    KafkaTopic inventoryOutboxTopicProps() {
        return new KafkaTopic(); //use Lombok @Setter to populate the fields of the KafkaTopic with values defined in application.properties
    }
    
    //topics are auto-created by Spring Kafka's KafkaAdmin detecting NewTopic and @Bean
    @Bean
    NewTopic inventoryInboxTopic() {
        var props = inventoryInboxTopicProps();
        return new NewTopic(props.name, props.partitions, props.replicas);
    }

    @Bean
    NewTopic inventoryOutboxTopic() {
        var props = inventoryOutboxTopicProps();
        return new NewTopic(props.name, props.partitions, props.replicas);
    }

    //ErrorHandlingDeserializer prevents malformed/garbage Kafka messages from crashing the service
    //Errors are handled by our kafkaErrorHandler @Bean found at the bottom of the class
    @Bean
    ConsumerFactory<String, ItemOrderEventPayload> consumerFactory(KafkaProperties props) {
        var valueDeserializer = new ErrorHandlingDeserializer<>(new JacksonJsonDeserializer<>(ItemOrderEventPayload.class, false)); //false means we always deserialize into ItemOrderEventPayload.class and ignore type-info headers
        var keyDeserializer = new ErrorHandlingDeserializer<>(new StringDeserializer());
        return new DefaultKafkaConsumerFactory<>(props.buildConsumerProperties(), keyDeserializer, valueDeserializer);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, ItemOrderEventPayload> kafkaListenerContainerFactory(
            KafkaProperties props, DefaultErrorHandler kafkaErrorHandler) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, ItemOrderEventPayload>();
        factory.setConsumerFactory(consumerFactory(props));
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
