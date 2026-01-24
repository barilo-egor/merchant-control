package tgb.cryptoexchange.merchantcontrol.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.backoff.FixedBackOff;
import tgb.cryptoexchange.merchantcontrol.kafka.ConsumerErrorService;
import tgb.cryptoexchange.merchantcontrol.kafka.DealReceive;

import java.util.Map;

@Configuration
@EnableAsync
public class CommonConfig {

    @Bean
    @Profile({"!kafka-disabled"})
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    @Bean
    @Profile({"!kafka-disabled"})
    public ConsumerFactory<String, DealReceive> consumerDealFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, DealReceive.KafkaDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    @Profile({"!kafka-disabled"})
    public ConcurrentKafkaListenerContainerFactory<String, DealReceive> dealListenerFactory(
            KafkaProperties kafkaProperties,
            ConsumerErrorService dealConsumerErrorService) {
        ConcurrentKafkaListenerContainerFactory<String, DealReceive> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerDealFactory(kafkaProperties));
        factory.setCommonErrorHandler(defaultErrorHandler(dealConsumerErrorService));
        return factory;
    }

    @Bean
    public DefaultErrorHandler defaultErrorHandler(ConsumerErrorService dealConsumerErrorService) {
        return new DefaultErrorHandler(
                dealConsumerErrorService::handle,
                new FixedBackOff(60000, 1)
        );
    }

    @Bean(name = "dealRequestSaveExecutor")
    public ThreadPoolTaskExecutor detailsRequestSearchExecutor(
            @Value("${details.executor.core-pool-size}") Integer corePoolSize,
            @Value("${details.executor.max-pool-size}") Integer maxPoolSize,
            @Value("${details.executor.queue-capacity}") Integer queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("DealRequestSave-");
        executor.initialize();
        return executor;
    }

}
