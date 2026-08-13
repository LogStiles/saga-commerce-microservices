package com.saga.payment.messaging;

import com.saga.payment.Payment;

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
 * It creates our two payment-service's Kafka topics using properties defined in application.properties.
 * It builds the consumer pipeline used by PaymentInboxEventConsumer to handle incoming Payment objects.
 * It builds the DLT pipeline with the beans deadLetterKafkaTemplate and kafkaErrorHandler.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    /**
     * KafkaTopic is an inner class that encapsulates the properties of our kafka topics
     */
    @Setter
    private static class KafkaTopic {
        private String name;
        private int partitions;
        private short replicas;
    }

    @Bean
    @ConfigurationProperties(prefix = "kafka.topic.inbox.events") //prefix refers to a property in application.properties
    KafkaTopic paymentInboxTopicProps() {
        return new KafkaTopic(); //use Lombok @Setter to populate the fields of the KafkaTopic with values defined in application.properties
    }

    @Bean
    @ConfigurationProperties(prefix = "kafka.topic.outbox.events") //prefix refers to a property in application.properties
    KafkaTopic paymentOutboxTopicProps() {
        return new KafkaTopic(); //use Lombok @Setter to populate the fields of the KafkaTopic with values defined in application.properties
    }

    //topics are auto-created by Spring Kafka's KafkaAdmin detecting NewTopic and @Bean
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
        var valueDeserializer = new ErrorHandlingDeserializer<>(new JacksonJsonDeserializer<>(Payment.class, false)); //false means we always deserialize into Payment.class and ignore type-info headers
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

    /**
     * A failed *deserialization* recovers the raw bytes, but a failed *listener* recovers the
     *  already-deserialized object, so the DLT producer has to handle both. byte[] is matched
     * first; anything else falls through to JSON. A byte[]-only template throws
     * ClassCastException on listener failures, leaving the record stuck in an endless retry.
     */
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

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> deadLetterKafkaTemplate) {
        var recoverer = new DeadLetterPublishingRecoverer(deadLetterKafkaTemplate);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2));
    }
}
