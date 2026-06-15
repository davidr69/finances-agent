package net.lavacro.finances.agent.kafka;

import lombok.extern.slf4j.Slf4j;
import net.lavacro.finances.agent.workflow.ChatBot;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AsyncProcessor {
    private final ChatBot chatBot;

    public AsyncProcessor(ChatBot chatBot) {
        this.chatBot = chatBot;
    }

    @Async
    public void process(String fileName, String accountId, byte[] payload) {
        log.info("[async] processing file={} accountId={} payloadSize={}", fileName, accountId, payload == null ? 0 : payload.length);
        chatBot.test(payload);
    }
}

