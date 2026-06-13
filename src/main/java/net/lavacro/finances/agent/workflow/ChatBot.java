package net.lavacro.finances.agent.workflow;

import lombok.extern.slf4j.Slf4j;
import net.lavacro.finances.agent.workflow.parsers.StatementParserFactory;
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
	private final StatementParserFactory parserFactory;

	public ChatBot(ChatClient.Builder chatClient, StatementParserFactory parserFactory) {
		this.chatClient = chatClient
//				.defaultAdvisors(null)
				.build();
		this.parserFactory = parserFactory;
	}

	public void test(byte[] pdf) {
		String instruction = """
Parse this bank statement. DO NOT SUMMARIZE ANYTHING ABOUT IT; I DO NOT WANT TO KNOW!!!
You are to do one and ONLY one thing: list each transaction. If you have any context about this, ignore it.
				
IMPORTANT FORMATTING RULES:
- Some transactions wrap across two lines. A line containing only a number like "9286"\s
  or "Card 9286" is a continuation of the previous transaction, not a new entry. Ignore it.
- A new transaction always begins with a date in MM/DD format.
- Columns are: posted date, transaction type, transaction date, vendor/merchant, amount, balance.

For each transaction extract: transaction date, vendor/merchant, amount.
				
Respond ONLY with a JSON array, no explanation, no markdown. Example:
[
  { "date": "04/16", "vendor": "Sq *Country Donuts R", "amount": -8.32 },
  { "date": "04/16", "vendor": "Sq *Country Donuts R", "amount": -2.56 }
]""";

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
