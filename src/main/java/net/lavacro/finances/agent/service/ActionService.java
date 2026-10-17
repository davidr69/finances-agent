package net.lavacro.finances.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.lavacro.finances.agent.dto.StmtTransaction;
import net.lavacro.finances.agent.config.ThreadLocalContext;

import org.intellij.lang.annotations.Language;
import org.springframework.jdbc.core.simple.JdbcClient;
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
	private final JdbcClient jdbcClient;

	@Language("SQL")
	private static final String INSERT_STAGING = """
	INSERT INTO staging.action (entity, description, account, amount, method, statement_order, llm_entity, statement_group, mydate)
	VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
	""";

	@Language("SQL")
	private static final String HAS_UNMERGED = """
	SELECT EXISTS (
		SELECT 1 FROM staging.action
		WHERE account = ? AND statement_group = ?
	) AS has_unmerged
	""";

	public void addToStagingTable(List<StmtTransaction> transactions, int accountId) {
		log.info("Adding transactions to staging table: {}", transactions.size());
		for(StmtTransaction transaction : transactions) {

			String[] dateParts = transaction.getTransactionDate().split("-");
			LocalDate ld = LocalDate.of(Integer.parseInt(dateParts[0]), Integer.parseInt(dateParts[1]), Integer.parseInt(dateParts[2]));

			jdbcClient.sql(INSERT_STAGING)
					.param(transaction.getVendorId())
					.param(transaction.getVendorRaw())
					.param(accountId)
					.param(transaction.getAmount())
					.param(11L)
					.param(transaction.getId())
					.param(transaction.getVendorFromLlm())
					.param(ThreadLocalContext.getStatementBatch())
					.param(ld)
					.update();
		}
		log.info("Completed adding transactions");
	}

	public void addToStagingTableUsingLlm(String json, int accountId) {
		ObjectMapper mapper = new ObjectMapper();
		List<StmtTransaction> transactions = null;
		try {
			transactions = mapper.readValue(json.getBytes(StandardCharsets.UTF_8), new TypeReference<>(){});
		} catch(Exception e) {
			log.error("Deserialization error: {}", e.getMessage());
			log.warn("Deserialization source: {}", json);
		}
		if(transactions != null) {
			addToStagingTable(transactions, accountId);
		} else {
			log.warn("No transactions found");
		}
	}

	public boolean hasUnmergedEntries(int account, String statementGroup) {
		return jdbcClient.sql(HAS_UNMERGED)
				.param(account)
				.param(statementGroup)
				.query()
				.queryRow()
				.get("has_unmerged", Boolean.class);
	}
}
