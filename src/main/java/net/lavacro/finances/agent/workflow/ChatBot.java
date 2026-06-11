package net.lavacro.finances.agent.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ChatBot {
	private final ChatClient chatClient;

	public ChatBot(ChatClient.Builder chatClient) {
		this.chatClient = chatClient.build();
	}

	public void test() {
		String response = chatClient.prompt("Tell me a joke").call().content();
		log.info(response);
		response = chatClient.prompt("What are the first 10 prime numbers?").call().content();
		log.info(response);
	}
}
