package net.lavacro.finances.agent.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(schema = "staging", name = "action")
@Getter
@Setter
public class ActionEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "action_id")
	private Integer id;

	private Integer entity;
	private Integer account;
	private BigDecimal amount;
	private LocalDate mydate;
	private Integer method;
	private String reference;
	private Boolean visible;
	private Boolean reconciled;
	private Integer category;
}
