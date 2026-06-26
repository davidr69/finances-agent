package net.lavacro.finances.agent.kafka.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class DecisionModel implements Serializable {
	private String decision;        // accept, change
	private Integer transactionId;
	private Integer originalVendorId;
	private String originalVendorName;
	private Integer newVendorId;
	private String newVendorName;
}
