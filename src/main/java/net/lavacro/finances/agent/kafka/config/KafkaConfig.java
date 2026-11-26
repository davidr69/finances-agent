package net.lavacro.finances.agent.kafka.config;

import net.lavacro.finances.shared.proto.DecisionProto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaConfigBuilder;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {
	@Bean
	public ConsumerFactory<String, byte[]> byteArrayConsumerFactory(KafkaProperties kafkaProperties) {
		Map<String, Object> props = new HashMap<>(KafkaConfigBuilder.of(kafkaProperties).consumer().build());
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
		props.put("spring.deserializer.key.delegate.class", StringDeserializer.class.getName());
		props.put("spring.deserializer.value.delegate.class", ByteArrayDeserializer.class.getName());

		return new DefaultKafkaConsumerFactory<>(props);
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactory(
			KafkaProperties kafkaProperties,
			ConsumerFactory<String, byte[]> byteArrayConsumerFactory,
			DefaultErrorHandler errorHandler) {
		var factory = new ConcurrentKafkaListenerContainerFactory<String, byte[]>();
		factory.setConsumerFactory(byteArrayConsumerFactory);
		factory.setCommonErrorHandler(errorHandler);
		applyListenerSettings(kafkaProperties, factory);
		return factory;
	}

	@Bean
	public ConsumerFactory<String, DecisionProto.DecisionMessage> decisionConsumerFactory(KafkaProperties kafkaProperties) {
		Map<String, Object> props = new HashMap<>(KafkaConfigBuilder.of(kafkaProperties).consumer().build());
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

		return new DefaultKafkaConsumerFactory<>(
				props,
				new StringDeserializer(),
				new ProtobufDeserializer<>(DecisionProto.DecisionMessage.parser())
		);
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, DecisionProto.DecisionMessage> decisionKafkaListenerContainerFactory(
			KafkaProperties kafkaProperties
	) {
		ConcurrentKafkaListenerContainerFactory<String, DecisionProto.DecisionMessage> factory =
				new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(decisionConsumerFactory(kafkaProperties));
		return factory;
	}

	private static void applyListenerSettings(
			KafkaProperties kafkaProperties,
			ConcurrentKafkaListenerContainerFactory<?, ?> factory) {
		var listener = kafkaProperties.getListener();
		factory.setConcurrency(listener.getConcurrency());
		if (listener.getType() == KafkaProperties.Listener.Type.BATCH) {
			factory.setBatchListener(true);
		}
		factory.getContainerProperties().setAckMode(listener.getAckMode());
		factory.getContainerProperties().setPollTimeout(listener.getPollTimeout().toMillis());
		factory.getContainerProperties().setIdleBetweenPolls(listener.getIdleBetweenPolls().toMillis());
	}

	@Bean
	public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);

		DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));

		handler.addNotRetryableExceptions(DeserializationException.class, IllegalArgumentException.class);
		return handler;
	}
}
