package net.lavacro.finances.agent.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lavacro.finances.agent.workflow.ChatBot;
import net.lavacro.finances.shared.proto.DecisionProto;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class MyConsumer {
	private final AsyncProcessor asyncProcessor;
	private final ChatBot chatBot;

	@KafkaListener(topics = "finances-topic")
	public void listen(ConsumerRecord<String, byte[]> message) {
		log.info("Received record: {}", message);

		Header filenameHeader = message.headers().lastHeader("filename");
		String filename = filenameHeader != null ? new String(filenameHeader.value(), StandardCharsets.UTF_8) : null;
		log.info("file name: {}", filename);

		Header accountHeader = message.headers().lastHeader("accountId");
		String accountId = accountHeader != null ? new String(accountHeader.value(), StandardCharsets.UTF_8) : null;
		log.info("account id: {}", accountId);

		Header yearHeader = message.headers().lastHeader("year");
		String year = yearHeader != null ? new String(yearHeader.value(), StandardCharsets.UTF_8) : null;
		log.info("year: {}", year);

		byte[] payload = message.value();

		log.info("payload size: {}", payload.length);

		if(accountId == null) {
			log.error("account id is null");
			return;
		}

		if(year == null) {
			log.error("year is null");
			return;
		}

		try {
			int accountNumber = Integer.parseInt(accountId);
			int yearInt = Integer.parseInt(year);
			chatBot.workflow(payload, accountNumber, yearInt);
		}  catch (NumberFormatException e) {
			log.error("Invalid account id: {}", accountId);
		}
	}

	@KafkaListener(topics = "finances-decision", containerFactory = "decisionKafkaListenerContainerFactory")
	public void listenDecision(ConsumerRecord<String, DecisionProto.DecisionMessage> message) {
		log.info("Received decision record: {}", message);
		asyncProcessor.processDecision(message.value());
	}
}
