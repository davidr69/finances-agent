package net.lavacro.finances.agent.service;

import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Service
public class EmbedVectorService {
	private final EmbeddingModel embeddingModel;
	private final JdbcTemplate jdbcTemplate;

	public void embedAllVendors() {
		List<Map<String, Object>> vendors = jdbcTemplate.queryForList(
				"SELECT id, description, bank_alias FROM entities WHERE embedding IS NULL"
		);

		for (Map<String, Object> vendor : vendors) {
			Long id = ((Number) vendor.get("id")).longValue();
			String description = (String) vendor.get("description");
			String bankAlias = (String) vendor.get("bank_alias");

			String concat = bankAlias == null || bankAlias.isEmpty() ? description : String.format("%s %s", description, bankAlias);
			float[] vector = embeddingModel.embed(concat);

			jdbcTemplate.update(
					"UPDATE entities SET embedding = ? WHERE id = ?",
					new PGvector(vector), id
			);
		}

		log.info("Embedded {} vendors", vendors.size());
	}
}
