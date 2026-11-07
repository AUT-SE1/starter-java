package edu.softwarelab.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * تنظیمات Kafka برای دو cluster: S3 (دستور انتقال و نتیجه) و S6 (گزارش روزانه).
 * Producer برای S6 در S6KafkaConfig از قبل هست.
 * Consumerها رشته می‌خوانند و StringJsonMessageConverter بر اساس نوع پارامتر متد @KafkaListener
 * به DTO تبدیل می‌کند؛ پس برای هر listener یک factory کافی است.
 */
@Configuration
public class KafkaClustersConfig {

    @Value("${kafka.clusters.s3.bootstrap-servers:localhost:9092}")
    private String s3Bootstrap;

    @Value("${kafka.clusters.s6.bootstrap-servers:localhost:9092}")
    private String s6Bootstrap;

    /** در تست‌ها false می‌شود تا listenerها به broker وصل نشوند. */
    @Value("${kafka.listeners.auto-startup:true}")
    private boolean autoStartup;

    @Bean(name = "s3KafkaTemplate")
    public KafkaTemplate<String, Object> s3KafkaTemplate(ObjectMapper objectMapper) {
        return new KafkaTemplate<>(producerFactory(s3Bootstrap, objectMapper));
    }

    @Bean(name = "s3ListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> s3ListenerFactory(ObjectMapper objectMapper) {
        return listenerFactory(s3Bootstrap, objectMapper);
    }

    @Bean(name = "s6ListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> s6ListenerFactory(ObjectMapper objectMapper) {
        return listenerFactory(s6Bootstrap, objectMapper);
    }

    private ProducerFactory<String, Object> producerFactory(String bootstrap, ObjectMapper objectMapper) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        JsonSerializer<Object> valueSerializer = new JsonSerializer<>(objectMapper);
        valueSerializer.setAddTypeInfo(false);
        return new DefaultKafkaProducerFactory<>(config, new StringSerializer(), valueSerializer);
    }

    private ConcurrentKafkaListenerContainerFactory<String, String> listenerFactory(
            String bootstrap, ObjectMapper objectMapper) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(config));
        factory.setRecordMessageConverter(new StringJsonMessageConverter(objectMapper));
        // ۳ بار تلاش مجدد با فاصله‌ی ۱ ثانیه، بعد رد شدن رکورد (DLT در استوری جدا)
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 3L)));
        factory.setAutoStartup(autoStartup);
        return factory;
    }
}
