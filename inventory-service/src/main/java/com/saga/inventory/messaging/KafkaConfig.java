package com.saga.inventory.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.ByteArraySerializer;
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
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import lombok.Setter;

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
    @ConfigurationProperties(prefix = "kafka.topic.inbox.events")
    KafkaTopic inventoryInboxTopicProps() {
        return new KafkaTopic();
    }

    @Bean
    @ConfigurationProperties(prefix = "kafka.topic.outbox.events")
    KafkaTopic inventoryOutboxTopicProps() {
        return new KafkaTopic();
    }

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

    @Bean
    ConsumerFactory<String, ItemOrderEventPayload> consumerFactory(KafkaProperties props) {
        var valueDeserializer = new ErrorHandlingDeserializer<>(new JacksonJsonDeserializer<>(ItemOrderEventPayload.class, false));
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

    @Bean
    KafkaTemplate<String, byte[]> deadLetterKafkaTemplate(KafkaProperties props) {
        var producerFactory = new DefaultKafkaProducerFactory<String, byte[]>(
                props.buildProducerProperties(), new StringSerializer(), new ByteArraySerializer());
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, byte[]> deadLetterKafkaTemplate) {
        var recoverer = new DeadLetterPublishingRecoverer(deadLetterKafkaTemplate);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2));
    }
}
