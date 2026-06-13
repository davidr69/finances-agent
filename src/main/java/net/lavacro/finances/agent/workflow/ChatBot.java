package net.lavacro.finances.agent.workflow;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;
import org.springframework.util.MimeTypeUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
		String instruction = """
Parse this bank statement transaction detail section.
				
IMPORTANT FORMATTING RULES:
- Some transactions wrap across two lines. A line containing only a number like "9286"\s
  or "Card 9286" is a continuation of the previous transaction, not a new entry. Ignore it.
- A new transaction always begins with a date in MM/DD format.
- Columns are: posted date, transaction type, transaction date, vendor/merchant, amount, balance.
				
Focus ONLY on lines that begin with a date (MM/DD format).
For each transaction extract: transaction date, vendor/merchant, amount.
				
Respond ONLY with a JSON array, no explanation, no markdown. Example:
[
  { "date": "04/16", "vendor": "Sq *Country Donuts R", "amount": -8.32 },
  { "date": "04/16", "vendor": "Sq *Country Donuts R", "amount": -2.56 }
]""";

//		String extracted;
//
//		try (
//				PDDocument doc = Loader.loadPDF(pdf)
//		) {
//			PDFTextStripper stripper = new PDFTextStripper();
//			stripper.setSortByPosition(true);
//			extracted = stripper.getText(doc);
//		} catch(IOException e) {
//			log.error("Error parsing PDF: {}", e.getMessage(), e);
//			return;
//		}
//
//		log.info("Prompt: {}", instruction);
//		log.info("PDF: {}", extracted);

//		String response = chatClient.prompt()
//			.system(instruction)
//			.user(extracted)
//			.call()
//			.content();

		List<byte[]> pages = pdfToImages(pdf);

// Build media list from pages
		List<Media> mediaList = pages.stream()
				.map(page -> new Media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(page)))
				.toList();

		String response = chatClient.prompt()
				.system(instruction)
				.user(u -> u.text("Extract all transactions from these bank statement pages.")
						.media(mediaList.toArray(new Media[0])))
				.call()
				.content();

		log.info("ChatBot response: {}", response);
	}

	List<byte[]> pdfToImages(byte[] pdf) {
		List<byte[]> images = new ArrayList<>();
		try (PDDocument doc = Loader.loadPDF(pdf)) {
			PDFRenderer renderer = new PDFRenderer(doc);
			for (int i = 0; i < doc.getNumberOfPages(); i++) {
				BufferedImage image = renderer.renderImageWithDPI(i, 300);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(image, "PNG", baos);
				images.add(baos.toByteArray());
			}
		} catch(IOException e) {
			log.error(e.getMessage(), e);
		}
		return images;
	}}
