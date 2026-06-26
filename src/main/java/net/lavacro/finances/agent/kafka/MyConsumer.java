package net.lavacro.finances.agent.kafka;

import lombok.extern.slf4j.Slf4j;
import net.lavacro.finances.agent.kafka.model.DecisionModel;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class MyConsumer {
	private final AsyncProcessor asyncProcessor;

	public MyConsumer(AsyncProcessor asyncProcessor) {
		this.asyncProcessor = asyncProcessor;
	}

	@KafkaListener(topics = "finances-topic")
	public void listen(ConsumerRecord<String, byte[]> message) {
		log.info("Received record: {}", message);

		Header filenameHeader = message.headers().lastHeader("filename");
		String filename = filenameHeader != null ? new String(filenameHeader.value(), StandardCharsets.UTF_8) : null;
		log.info("file name: {}", filename);

		Header accountHeader = message.headers().lastHeader("accountId");
		String accountId = accountHeader != null ? new String(accountHeader.value(), StandardCharsets.UTF_8) : null;
		log.info("account id: {}", accountId);

		byte[] payload = message.value();

		log.info("payload size: {}", payload.length);
		asyncProcessor.process(filename, accountId, payload);
	}

	@KafkaListener(topics = "finances-decision")
	public void listenDecision(ConsumerRecord<String, DecisionModel> message) {
		log.info("Received decision record: {}", message);

		DecisionModel decisionModel = message.value();
		log.info("decision: {}", decisionModel.getDecision());
		log.info("transaction id: {}", decisionModel.getTransactionId());
		log.info("original vendor id: {}", decisionModel.getOriginalVendorId());
		log.info("original vendor name: {}", decisionModel.getOriginalVendorName());
		log.info("new vendor id: {}", decisionModel.getNewVendorId());
		log.info("new vendor name: {}", decisionModel.getNewVendorName());
	}
}
