package net.lavacro.finances.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StmtTransaction {
	private Integer id;

	@JsonProperty("posted_date")
	private String postedDate;

	@JsonProperty("transaction_date")
	private String transactionDate;

	@JsonProperty("vendor_from_stmt")
	private String vendorRaw;

	@JsonProperty("vendor_from_db")
	private String vendorFromDb;

	@JsonProperty("vendor_id")
	private Integer vendorId;

	@JsonProperty("vendor_from_llm")
	private Integer vendorFromLlm;

	private Double confidence;
	private BigDecimal amount;
}
