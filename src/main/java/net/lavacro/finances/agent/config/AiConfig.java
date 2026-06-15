package net.lavacro.finances.agent.config;

import net.lavacro.finances.agent.workflow.VendorTool;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

	private static final String PROMPT = """
		You are a vendor resolution agent. You MUST use tools for every request.
		For every vendor string you receive:
		1. ALWAYS call findVendor first — never skip this step
		2. If findVendor returns a vendor_id, respond with ONLY that integer
		3. If findVendor returns NO_VENDOR_FOUND, call createVendor with a clean name
		4. After createVendor, respond with ONLY the new integer vendor_id
		NEVER invent or guess a vendor_id.
	""";

	@Bean
	ChatClient anthropicChatClient(
			AnthropicChatModel anthropicChatModel,
			VendorTool vendorTool) {
		return ChatClient.builder(anthropicChatModel)
				.defaultSystem(PROMPT)
				.defaultTools(vendorTool)
				.build();
	}

	@Bean
	ChatClient geminiChatClient(
			GoogleGenAiChatModel googleGenAiChatModel,
			VendorTool vendorTool) {
		return ChatClient.builder(googleGenAiChatModel)
				.defaultSystem(PROMPT)
				.defaultTools(vendorTool)
				.build();
	}
}
