package net.lavacro.finances.agent.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class MyConsumer {

	@KafkaListener(topics = "finances-topic", batch = "true")
	public void listen(List<ConsumerRecord<String, byte[]>> messages, Acknowledgment ack) {
		messages.forEach(m -> {
			log.info("Received record: {}", m);
		});
		ack.acknowledge();
	}
}
