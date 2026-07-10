package net.lavacro.finances.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lavacro.finances.agent.dto.StmtTransaction;
import net.lavacro.finances.agent.entities.ActionEntity;
import net.lavacro.finances.agent.repositories.ActionRepository;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActionService {
	private final ActionRepository actionRepository;

	public void addToStagingTable(List<StmtTransaction> transactions, int accountId) {
		log.info("Adding transactions to staging table: {}", transactions.size());
		for(StmtTransaction transaction : transactions) {
			ActionEntity entity = new ActionEntity();

			entity.setEntity(transaction.getVendorId());
			entity.setDescription(transaction.getVendorRaw());
			entity.setAccount(accountId);
			entity.setAmount(transaction.getAmount());
			entity.setMethod(11);
			entity.setStatementOrder(transaction.getId());
			entity.setLlmEntity(transaction.getVendorFromLlm());

			String[] dateParts = transaction.getTransactionDate().split("-");
			LocalDate ld = LocalDate.of(Integer.parseInt(dateParts[0]), Integer.parseInt(dateParts[1]), Integer.parseInt(dateParts[2]));
			entity.setMydate(ld);

			actionRepository.save(entity);
		}
		log.info("Completed adding transactions");
	}

	public void addToStagingTableUsingLlm(String json, int accountId) {
		ObjectMapper mapper = new ObjectMapper();
		List<StmtTransaction> transactions = null;
		try {
			transactions = mapper.readValue(json.getBytes(StandardCharsets.UTF_8), new TypeReference<>(){});
		} catch(Exception e) {
			log.error("Deserialization error: {}", e.getMessage(), e);
		}
		if(transactions != null) {
			addToStagingTable(transactions, accountId);
		} else {
			log.warn("No transactions found");
		}
	}
}
