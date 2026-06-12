package net.lavacro.finances.agent.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.ByteArrayInputStream;
import java.io.IOException;


@Service
@Slf4j
public class ChatBot {
	private final ChatClient chatClient;

	public ChatBot(ChatClient.Builder chatClient) {
		this.chatClient = chatClient.build();
	}

	public void test(byte[] pdf) {
		String instruction = "Parse this bank statement. Determine the opening balance and each transaction. " +
			"This particular statement has two accounts, each with an opening balance, transactions, interest, and ending balance.";

		String extracted;
		try {
			extracted = extractTextFromPdf(pdf);
		} catch (IOException e) {
			log.error("Failed to extract PDF text", e);
			return;
		}

		if (extracted == null || extracted.isBlank()) {
			log.warn("No text extracted from PDF");
			return;
		}

		// Send the full extracted text to your model (no chunking per user preference)
		String toSend = "Bank statement text:\n\n" + extracted + "\n\n" + instruction;

		String response = chatClient.prompt(toSend).call().content();
		log.info(response);
	}

	private static String extractTextFromPdf(byte[] pdf) throws IOException {
		// Use reflection to call PDFBox loader methods so the code works across PDFBox 2.x and 3.x
		java.io.InputStream is = new ByteArrayInputStream(pdf);

		// Try any Loader.load(...) variant first
		try {
			Class<?> loaderClass = Class.forName("org.apache.pdfbox.Loader");
			for (java.lang.reflect.Method m : loaderClass.getMethods()) {
				if (!m.getName().equals("load")) continue;
				Class<?>[] params = m.getParameterTypes();
				if (params.length != 1) continue;
				try {
					Object docObj = null;
					if (params[0].isAssignableFrom(java.io.InputStream.class)) {
						docObj = m.invoke(null, new ByteArrayInputStream(pdf));
					} else if (params[0].isAssignableFrom(byte[].class)) {
						docObj = m.invoke(null, (Object) pdf);
					} else {
						continue;
					}
					if (docObj instanceof PDDocument) {
						log.debug("Loaded PDF using Loader.load overload: {}", m);
						try (PDDocument d = (PDDocument) docObj) {
							PDFTextStripper stripper = new PDFTextStripper();
							return stripper.getText(d);
						}
					}
				} catch (IllegalArgumentException | ReflectiveOperationException e) {
					// try next overload
				}
			}
		} catch (ClassNotFoundException e) {
			// ignore - try PDDocument methods next
		}

		// Try PDDocument.load(...) overloads
		for (java.lang.reflect.Method method : PDDocument.class.getMethods()) {
				if (!method.getName().equals("load")) continue;
				Class<?>[] params = method.getParameterTypes();
				if (params.length != 1) continue;
				try {
					Object docObj = null;
					if (params[0].isAssignableFrom(java.io.InputStream.class)) {
						docObj = method.invoke(null, new ByteArrayInputStream(pdf));
					} else if (params[0].isAssignableFrom(byte[].class)) {
						docObj = method.invoke(null, (Object) pdf);
					} else {
						continue;
					}
					if (docObj instanceof PDDocument) {
						log.debug("Loaded PDF using PDDocument.load overload: {}", method);
						try (PDDocument d = (PDDocument) docObj) {
							PDFTextStripper stripper = new PDFTextStripper();
							return stripper.getText(d);
						}
					}
				} catch (IllegalArgumentException | ReflectiveOperationException e) {
					// continue trying other overloads
				}
			}

		throw new IOException("No suitable PDFBox load method found on the classpath");
	}


}
