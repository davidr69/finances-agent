package net.lavacro.finances.agent.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;

import java.io.IOException;

@Service
@Slf4j
public class ChatBot {
	private final ChatClient chatClient;

	public ChatBot(ChatClient.Builder chatClient) {
		this.chatClient = chatClient
//				.defaultAdvisors(null)
				.build();
	}

	public void test(byte[] pdf) {
		String instruction = "Parse this bank statement. Determine the opening balance and each transaction. " +
			"This particular statement has two accounts, each with an opening balance, transactions, interest, and ending balance.";

		String extracted;

		try (
				PDDocument doc = Loader.loadPDF(pdf)
		) {
			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setSortByPosition(true);
			extracted = stripper.getText(doc);
		} catch(IOException e) {
			log.error("Error parsing PDF: {}", e.getMessage(), e);
			return;
		}

		String response = chatClient.prompt()
			.system(instruction)
			.user(extracted)
			.call()
			.content();

		log.info("ChatBot response: {}", response);
	}
}
