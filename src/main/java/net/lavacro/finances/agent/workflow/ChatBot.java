package net.lavacro.finances.agent.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.lavacro.finances.agent.dto.StmtTransaction;
import net.lavacro.finances.agent.service.ActionService;
import net.lavacro.finances.agent.workflow.parsers.StatementParserFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ChatBot {
	private final ChatClient chatClient;
	private final StatementParserFactory parserFactory;
	private final VendorTool vendorTool;
	private final ActionService actionService;

	private static final int CHUNK_SIZE = 5;

	// TODO: allow the LLM to be specified via Spring resolvers
	public ChatBot(
//			@Qualifier(value = "googleGenAiChatModel") ChatModel chatModel,
//			@Qualifier(value = "ollamaChatModel") ChatModel chatModel,
			@Qualifier(value = "anthropicChatModel") ChatModel chatModel,
			StatementParserFactory parserFactory,
			VendorTool vendorTool,
			ActionService actionService
	) {
		this.chatClient = ChatClient.builder(chatModel).build();
		this.parserFactory = parserFactory;
		this.vendorTool = vendorTool;
		this.actionService = actionService;
	}

	public void workflow(byte[] pdf, int account, int year) {
		String instruction = """
You are a vendor validation agent for a personal finance system.

You will receive a JSON array of low-confidence vendor matches from a bank statement.
Each entry has:
- vendor_from_stmt: the raw string from the bank
- vendor_from_db: the current best guess matched from our database
- confidence: similarity score (all will be below 0.80)
- vendor_id: the current matched vendor ID
- vendor_from_llm: the field YOU are going to populate

For each entry:
1. Use your knowledge of business names and abbreviations to evaluate the match
2. If vendor_from_db looks correct, keep the vendor_id and set vendor_from_llm to that value
3. If vendor_from_db looks wrong, call findVendor with a better search term
4. If findVendor returns a better match, use that vendor_id
5. If findVendor returns NO_VENDOR_FOUND, set vendor_from_llm to your best guess
   of the clean business name for manual review

Return the COMPLETE original JSON array with ONLY "vendor_from_llm" updated.

Keep ALL other fields exactly as provided (confidence, amount, posted_date, transaction_date, vendor_from_stmt, vendor_from_db).

No explanation. No markdown. Only valid JSON array.

Example input entry:
{"confidence":0.76,"amount":-32.48,"posted_date":"2026-04-17","transaction_date":"2026-04-17","vendor_from_stmt":"Shoprite Hylan Plaza","vendor_from_db":"United Artists Hylan Plaza","vendor_id":303,"vendor_from_llm":null}

Example output entry:
{"confidence":0.76,"amount":-32.48,"posted_date":"2026-04-17","transaction_date":"2026-04-17","vendor_from_stmt":"Shoprite Hylan Plaza","vendor_from_db":"United Artists Hylan Plaza","vendor_id":303,"vendor_from_llm":57}

CRITICAL: Your response must start with [ and end with ]
Do not write any text before or after the JSON array.
Do not use markdown. Do not summarize. Do not explain your reasoning.
Output the complete JSON array for ALL entries with no truncation.

CRITICAL VALIDATION RULES:
- A match is WRONG if vendor_from_db contains words completely unrelated to vendor_from_stmt
- When in doubt, call findVendor — do not assume a match is correct
- These are known wrong match patterns you MUST correct:
  * "Bk of Amer" or "Bank of America" matched to anything other than a Bank of America entity → call findVendor("Bank of America")
  * Any vendor matched to "Amazon.com" that is not an Amazon purchase → call findVendor with the real name
  * Any vendor matched to "Staten Island Zoo" that is not a zoo → call findVendor with the real name
  * Any vendor matched to "Saturn of Staten Island" that is not a car dealer → call findVendor with the real name
  * "Shoprite" matched to "United Artists" → call findVendor("Shoprite")
				
ALWAYS call findVendor when confidence is below 0.65 — never accept the match blindly.

FINAL OUTPUT REQUIREMENT (read this last, this overrides everything above about format):
After you finish calling tools and reasoning internally, your VERY LAST message must contain\s
ONLY the JSON array — nothing else.
Do not write phrases like "Now I have the vendor ID" or "Let me review."
Do not summarize what you did.
Your last message starts with [ and ends with ].
If you write anything else first, you have failed this task.
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

		List<StmtTransaction> transactions;
		try {
			transactions = parserFactory.getParser(account).parseStatement(extracted, year);
		} catch(IllegalArgumentException e) {
			log.error("No parser available for account {}: {}", account, e.getMessage());
			return;
		}
		log.info("Parsed statement: {}", transactions.size());

		transactions.forEach(item -> {
			vendorTool.findVendor(item);
			log.info("raw: {}, resolved: {}, id: {}, score: {}",
					item.getVendorRaw(), item.getVendorFromDb(), item.getVendorId(), item.getConfidence());
		});

		actionService.addToStagingTable(transactions.stream()
				.filter(m -> m.getConfidence() >= 0.80)
				.toList(),
				account
		);

		List<StmtTransaction> needsValidation = transactions.stream()
				.filter(m -> m.getConfidence() < 0.80)
				.toList();

		ObjectMapper mapper = new ObjectMapper();
		String json;

		for(int i = 0; i < needsValidation.size(); i += CHUNK_SIZE) {
			List<StmtTransaction> chunk = needsValidation.subList(i, Math.min(i + CHUNK_SIZE, needsValidation.size()));

			try {
				json = mapper.writeValueAsString(chunk);
			} catch(JsonProcessingException e) {
				log.error("Error parsing JSON: {}", e.getMessage(), e);
				continue;
			}

			log.info("Validating ...");
			ChatResponse response = chatClient.prompt()
					.system(instruction)
					.user(json + "\n\nREMINDER: Respond with ONLY the JSON array. No narration. Start with [ and end with ].")
					.tools(vendorTool)
					.call()
					.chatResponse();

			if(response == null) {
				log.error("No response from chat client");
				continue;
			}

			String jsonResponse = Objects.requireNonNull(response.getResult()).getOutput().getText();
			assert jsonResponse != null;
			int jsonStart = jsonResponse.indexOf('[');

			if(jsonStart > 0) {
				jsonResponse = jsonResponse.substring(jsonStart);
			}

			log.info("Response: {}", jsonResponse);
			Usage usage = response.getMetadata().getUsage();
			log.info("Total tokens: {}", usage.getTotalTokens());
			log.info("Native usage: {}", usage.getNativeUsage());

			actionService.addToStagingTableUsingLlm(jsonResponse, account);
		}
	}
}
