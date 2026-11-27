package net.lavacro.finances.agent.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lavacro.finances.agent.service.EmbedVectorService;
import net.lavacro.finances.shared.proto.DecisionProto;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AsyncProcessor {
    private final EmbedVectorService embedVectorService;

    @Async
    public void processDecision(DecisionProto.DecisionMessage decisionModel) {
        log.info("decision: {}", decisionModel.getDecision());
        log.info("transaction id: {}", decisionModel.getTransactionId());
        log.info("original vendor id: {}", decisionModel.getOriginalVendorId());
        log.info("original vendor name: {}", decisionModel.getOriginalVendorName());
        log.info("new vendor id: {}", decisionModel.getNewVendorId());
        log.info("new vendor name: {}", decisionModel.getNewVendorName());
        embedVectorService.embedVendor(decisionModel);
    }
}

