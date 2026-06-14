package net.lavacro.finances.agent.workflow;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VendorTool {

	private final EmbeddingModel embeddingModel;

	private final JdbcTemplate jdbcTemplate;


//	@McpTool(description = "TBD")
//	String noop(@McpToolParam(description = "TBD") String input) {
//		return "noop";
//	}
}
