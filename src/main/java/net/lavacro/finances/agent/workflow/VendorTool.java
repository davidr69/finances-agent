package net.lavacro.finances.agent.workflow;

import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import net.lavacro.finances.agent.dto.StmtTransaction;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import org.intellij.lang.annotations.Language;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VendorTool {

	private final EmbeddingModel embeddingModel;
	private final JdbcTemplate jdbcTemplate;

	@Language("SQL")
	private final static String FIND_VENDOR_QUERY = """
	SELECT id, description, 1 - (embedding <=> ?::vector) AS score
	FROM entities
	ORDER BY embedding <=> ?::vector
	LIMIT 1
	""";

	public void findVendor(StmtTransaction transaction) {
		float[] queryVector = embeddingModel.embed(transaction.getVendorRaw());

		PGvector vector = new PGvector(queryVector);

		List<Map<String, Object>> results = jdbcTemplate.queryForList(FIND_VENDOR_QUERY, vector, vector);

		if (results.isEmpty()) return;

		Map<String, Object> row = results.get(0);
		double score = ((Number) row.get("score")).doubleValue();

		transaction.setConfidence(score);
		transaction.setVendorId(((Number) row.get("id")).intValue());
		transaction.setVendorFromDb((String) row.get("description"));
	}

	@Tool(description = "Create a new vendor and return its id.")
	public String createVendor(String vendorName) {
		// check it doesn't already exist first
		List<Map<String, Object>> existing = jdbcTemplate.queryForList(
				"SELECT id FROM entities WHERE LOWER(description) = LOWER(?)",
				vendorName
		);

		if (!existing.isEmpty()) {
			return "vendor_id:" + ((Number) existing.get(0).get("id")).longValue();
		}

		// insert new vendor
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement ps = connection.prepareStatement(
					"INSERT INTO entities (description) VALUES (?)",
					Statement.RETURN_GENERATED_KEYS
			);
			ps.setString(1, vendorName);
			return ps;
		}, keyHolder);

		Long newId = keyHolder.getKey().longValue();

		// embed the new vendor immediately
		float[] vector = embeddingModel.embed(vendorName);
		jdbcTemplate.update(
				"UPDATE entities SET embedding = ? WHERE id = ?",
				new PGvector(vector), newId
		);

		return "vendor_id:" + newId;
	}
}
