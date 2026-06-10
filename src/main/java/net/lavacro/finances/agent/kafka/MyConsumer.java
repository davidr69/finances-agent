package net.lavacro.finances.agent.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class MyConsumer {

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
		File file = new File("/tmp/temp.pdf");
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(payload);
		} catch (IOException e) {
			log.error("Error writing file: {}", e.getMessage());
		}
	}
}
