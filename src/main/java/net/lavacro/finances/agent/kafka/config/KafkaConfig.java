package net.lavacro.finances.agent.kafka.config;

import net.lavacro.finances.agent.kafka.model.DecisionModel;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

@Configuration
@EnableKafka
public class KafkaConfig {

	@Bean
	public ConsumerFactory<String, byte[]> byteArrayConsumerFactory(KafkaProperties kafkaProperties) {
		return new DefaultKafkaConsumerFactory<>(
				kafkaProperties.buildConsumerProperties(),
				new StringDeserializer(),
				new ByteArrayDeserializer()
		);
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactory(
			KafkaProperties kafkaProperties,
			ConsumerFactory<String, byte[]> byteArrayConsumerFactory) {
		var factory = new ConcurrentKafkaListenerContainerFactory<String, byte[]>();
		factory.setConsumerFactory(byteArrayConsumerFactory);
		applyListenerSettings(kafkaProperties, factory);
		return factory;
	}

	@Bean
	public ConsumerFactory<String, DecisionModel> decisionConsumerFactory(KafkaProperties kafkaProperties) {
		var jsonDeserializer = new JacksonJsonDeserializer<>(DecisionModel.class);
		jsonDeserializer.trustedPackages("net.lavacro.finances.agent.kafka.model");
		return new DefaultKafkaConsumerFactory<>(
				kafkaProperties.buildConsumerProperties(),
				new StringDeserializer(),
				jsonDeserializer
		);
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, DecisionModel> decisionKafkaListenerContainerFactory(
			KafkaProperties kafkaProperties,
			ConsumerFactory<String, DecisionModel> decisionConsumerFactory) {
		var factory = new ConcurrentKafkaListenerContainerFactory<String, DecisionModel>();
		factory.setConsumerFactory(decisionConsumerFactory);
		applyListenerSettings(kafkaProperties, factory);
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
	}
}
