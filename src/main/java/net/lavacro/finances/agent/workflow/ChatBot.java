package net.lavacro.finances.agent.workflow;

import lombok.extern.slf4j.Slf4j;
import net.lavacro.finances.agent.dto.StmtTransaction;
import net.lavacro.finances.agent.workflow.parsers.StatementParserFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class ChatBot {
	private final ChatClient chatClient;
	private final StatementParserFactory parserFactory;
	private final VendorTool vendorTool;

	public ChatBot(
//			@Qualifier(value = "googleGenAiChatModel") ChatModel chatModel,
			@Qualifier(value = "ollamaChatModel") ChatModel chatModel,
			StatementParserFactory parserFactory,
			VendorTool vendorTool
	) {
//		this.chatClient = chatClient.build();
		this.chatClient = ChatClient.builder(chatModel).build();
		this.parserFactory = parserFactory;
		this.vendorTool = vendorTool;
	}

	public void test(byte[] pdf) {
		String instruction = """
				You are a vendor resolution agent. You MUST use tools for every request.

				For every vendor string you receive:
				1. If the string appears to have a location (for example "Staten Island NY"), ignore that as it is not part of the vendor's name
				2. ALWAYS call findVendor first — never skip this step
				3. If findVendor returns a vendor_id, respond with ONLY that integer
				4. If findVendor returns NO_VENDOR_FOUND, call createVendor with a clean name
				5. After createVendor, respond with ONLY the new integer vendor_id

				NEVER invent or guess a vendor_id.
				NEVER skip calling findVendor.
				Your response must be a single integer obtained from a tool call.
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

		List<StmtTransaction> transactions = parserFactory.getParser(6).parseStatement(extracted);
		log.info("Parsed statement: {}", transactions.size());

		transactions.forEach( stmt -> {
			log.info("Processing statement: {}", stmt.vendorRaw());
			String response = chatClient.prompt()
					.system(instruction)
					.user(stmt.vendorRaw())
					.tools(vendorTool)
					.call()
					.content();
			log.info("Response: {}", response);
		});
	}
}
