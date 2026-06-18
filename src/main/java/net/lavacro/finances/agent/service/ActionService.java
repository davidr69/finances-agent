package net.lavacro.finances.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lavacro.finances.agent.dto.StmtTransaction;
import net.lavacro.finances.agent.entities.ActionEntity;
import net.lavacro.finances.agent.repositories.ActionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActionService {
	private final ActionRepository actionRepository;

	public void addToStagingTable(List<StmtTransaction> transactions) {
		log.info("Adding transactions to staging table: {}", transactions.size());
		for(StmtTransaction transaction : transactions) {
			ActionEntity entity = new ActionEntity();

			entity.setEntity(transaction.getVendorId());
			entity.setAccount(6);
			entity.setAmount(transaction.getAmount());
			entity.setMethod(11);

			String[] dateParts = transaction.getTransactionDate().split("-");
			LocalDate ld = LocalDate.of(Integer.parseInt(dateParts[0]), Integer.parseInt(dateParts[1]), Integer.parseInt(dateParts[2]));
			entity.setMydate(ld);

			actionRepository.save(entity);
		}
		log.info("Completed adding transactions");
	}
}
