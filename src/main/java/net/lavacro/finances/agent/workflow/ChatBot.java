package net.lavacro.finances.agent.workflow;

import lombok.extern.slf4j.Slf4j;
import net.lavacro.finances.agent.workflow.parsers.StatementParserFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;

import java.io.IOException;

@Service
@Slf4j
public class ChatBot {
	private final ChatClient chatClient;
	private final EmbeddingModel embeddingModel;
	private final StatementParserFactory parserFactory;

	public ChatBot(
			ChatClient.Builder chatClient,
			EmbeddingModel embeddingModel,
			StatementParserFactory parserFactory
	) {
		this.chatClient = chatClient
//				.defaultAdvisors(null)
				.build();
		this.parserFactory = parserFactory;
		this.embeddingModel = embeddingModel;
	}

	public void test(byte[] pdf) {
		String instruction = """
These are transactions from my bank statement. The first field is the posted date. The last field is the balance, and the field before the last is the transaction amount.

Sometimes there will be a second date, which is the transaction date, immediately followed by the vendor. In cases where there is no second date, the vendor will be immediately after the first date.

Please identify the vendors in these transactions. DO NOT SUMMARIZE. Provide a list of JSON objects in the following format:

[
	{"date":"04/05", "vendor":"Netflix", "amount":"12.34"},
	{"date":"05/09", "vendor":"Costco", "amount":"12.34"}
]

""";

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

		String parsed = parserFactory.getParser(6).parseStatement(extracted);
		log.info("Parsed statement: {}", parsed);

		String response = chatClient.prompt()
			.system(instruction)
			.user(parsed)
			.call()
			.content();

		log.info("ChatBot response: {}", response);
	}
}
