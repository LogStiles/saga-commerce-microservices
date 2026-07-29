package com.saga.inventory.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

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
    @ConfigurationProperties(prefix = "kafka.topic.itemorder.inbox.events")
    KafkaTopic itemOrderInboxTopicProps() {
        return new KafkaTopic();
    }

    @Bean
    @ConfigurationProperties(prefix = "kafka.topic.itemorder.outbox.events")
    KafkaTopic itemOrderOutboxTopicProps() {
        return new KafkaTopic();
    }

    @Bean
    NewTopic itemOrderInboxTopic() {
        var props = itemOrderInboxTopicProps();
        return new NewTopic(props.name, props.partitions, props.replicas);
    }

    @Bean
    NewTopic itemOrderOutboxTopic() {
        var props = itemOrderOutboxTopicProps();
        return new NewTopic(props.name, props.partitions, props.replicas);
    }

    @Bean
    ConsumerFactory<String, ItemOrderEventPayload> consumerFactory(KafkaProperties props) {
        return new DefaultKafkaConsumerFactory<>(props.buildConsumerProperties(),
                                                new StringDeserializer(),
                                                new JsonDeserializer<>(ItemOrderEventPayload.class));
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, ItemOrderEventPayload> itemOrderKLCFactory(KafkaProperties props) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, ItemOrderEventPayload>();
        factory.setConsumerFactory(consumerFactory(props));
        factory.setConcurrency(props.getListener().getConcurrency());
        return factory;
    }
}
