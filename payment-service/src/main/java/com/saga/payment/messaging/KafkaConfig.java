package com.saga.payment.messaging;

import com.saga.payment.Payment;

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
public class KafkaConfig {

    @Setter
    private static class KafkaTopic {
        private String name;
        private int partitions;
        private short replicas;
    }

    @Bean
    @ConfigurationProperties(prefix = "kafka.topic.inbox.events")
    KafkaTopic paymentInboxTopicProps() {
        return new KafkaTopic();
    }

    @Bean
    @ConfigurationProperties(prefix = "kafka.topic.outbox.events")
    KafkaTopic paymentOutboxTopicProps() {
        return new KafkaTopic();
    }

    @Bean
    NewTopic paymentInboxTopic() {
        var props = paymentInboxTopicProps();
        return new NewTopic(props.name, props.partitions, props.replicas);
    }

    @Bean
    NewTopic paymentOutboxTopic() {
        var props = paymentOutboxTopicProps();
        return new NewTopic(props.name, props.partitions, props.replicas);
    }

    @Bean
    ConsumerFactory<String, Payment> consumerFactory(KafkaProperties props) {
        var valueDeserializer = new ErrorHandlingDeserializer<>(new JacksonJsonDeserializer<>(Payment.class, false));
        var keyDeserializer = new ErrorHandlingDeserializer<>(new StringDeserializer());
        return new DefaultKafkaConsumerFactory<>(props.buildConsumerProperties(), keyDeserializer, valueDeserializer);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, Payment> kafkaListenerContainerFactory(
            KafkaProperties props, DefaultErrorHandler kafkaErrorHandler) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Payment>();
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
