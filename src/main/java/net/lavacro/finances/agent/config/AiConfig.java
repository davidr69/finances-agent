package net.lavacro.finances.agent.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
	@Bean
	ChatClient anthropicChatClient(AnthropicChatModel anthropicChatModel) {
		return ChatClient.builder(anthropicChatModel).build();
	}

	@Bean
	ChatClient geminiChatClient(
			GoogleGenAiChatModel googleGenAiChatModel) {
		return ChatClient.builder(googleGenAiChatModel).build();
	}
}
