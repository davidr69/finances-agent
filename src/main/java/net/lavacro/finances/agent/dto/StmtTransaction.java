package net.lavacro.finances.agent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StmtTransaction(
		LocalDate postedDate,
		LocalDate transactionDate,
		String vendorRaw,
		BigDecimal amount
) { }
