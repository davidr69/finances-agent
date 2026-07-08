package net.lavacro.finances.agent.service;

import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lavacro.finances.shared.proto.DecisionProto;
import org.intellij.lang.annotations.Language;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Slf4j
@Service
public class EmbedVectorService {
	private final EmbeddingModel embeddingModel;
	private final JdbcTemplate jdbcTemplate;
	private final JdbcClient jdbcClient;
	private final StringRedisTemplate redisTemplate;

	@Language("SQL")
	private static final String GET_ROW = """
		SELECT id, description, bank_alias
		FROM entities
		WHERE id = ?
	""";

	@Language("SQL")
	private static final String GET_EXISTING_EMBEDDINGS = """
		SELECT id, description, bank_alias
		FROM entities
		WHERE embedding IS NOT NULL
		ORDER BY description
	""";

	@Language("SQL")
	private static final String UPDATE_ONE_EMBEDDING = """
		UPDATE entities
		SET embedding = ?
		WHERE id = ?
	""";

	public void embedVendor(DecisionProto.DecisionMessage message) {
		if(message.getDecision() == DecisionProto.DecisionMessage.Decision.REFRESH) {
			embedAllVectors();
		} else {
			Map<String, Object> vendor = jdbcClient.sql(GET_ROW).param(message.getNewVendorId()).query().singleRow();

			String concat = doEmbedding(vendor);
			log.info("Embedded {} ({})", message.getNewVendorId(), concat);
		}
	}

	public void embedAllVectors() {
		log.info("Updating vectors ...");

		boolean canRefresh = redisTemplate.opsForValue().setIfAbsent("finances-agent-embedding", "active", 1L, TimeUnit.HOURS);

		if(canRefresh) {
			List<Map<String, Object>> vendors = jdbcTemplate.queryForList(GET_EXISTING_EMBEDDINGS);

			for (Map<String, Object> vendor : vendors) {
				doEmbedding(vendor);
			}

			log.info("Processed {} entities", vendors.size());
		} else {
			log.warn("Cannot refresh embedding due to lock");
		}
	}

	private String doEmbedding(Map<String, Object> entity) {
		Long id = ((Number) entity.get("id")).longValue();
		String description = (String) entity.get("description");
		String bankAlias = (String) entity.get("bank_alias");

		String concat = bankAlias == null || bankAlias.isEmpty() ? description : String.format("%s %s", description, bankAlias);

		log.info("Updating id {}: {} ...", id, concat);

		float[] vector = embeddingModel.embed(concat);

		jdbcTemplate.update(UPDATE_ONE_EMBEDDING, new PGvector(vector), id);
		return concat;
	}
}
