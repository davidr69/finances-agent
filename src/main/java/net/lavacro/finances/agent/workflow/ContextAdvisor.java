package net.lavacro.finances.agent.workflow;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ContextAdvisor {
	@Bean
	PromptChatMemoryAdvisor promptChatMemoryAdvisor() {
		return null;
	}
}
