package net.lavacro.finances.agent.dto;

import java.math.BigDecimal;

public record StmtTransaction(
		String postedDate,
		String transactionDate,
		String vendorRaw,
		BigDecimal amount
) { }
