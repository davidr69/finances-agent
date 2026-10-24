package net.lavacro.finances.agent;

import com.pgvector.PGvector;
import net.lavacro.finances.agent.service.EmbedVectorService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@SpringBootTest
class EmbedVendorsTest {
	private final Logger log = LoggerFactory.getLogger(EmbedVendorsTest.class);

	@Autowired
	EmbeddingModel embeddingModel;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	EmbedVectorService embedVectorService;

//	@Test
	void multipleVendorsTest() {
		String[] vendors = {
				"Sq *Country Donuts R Staten Island NY Card 1234",
				"Wendy's 101 Staten Island NY Card 1234",
				"Abc*Pf Staten Island N 347-3493686",
				"Shoprite Hylan Plaza Staten Island NY Card 1234",
				"El Dorado Restaurant Staten Island NY Card 1234",
				"Chr*Christianbook 800-247-4784 MA Card 1234",
				"Tst*Fresca Iselin Iselin NJ Card 123",
				"Chick-Fil-A #04353 Watchung NJ Card 1234",
				"Tst*LA Piazza Pizza Staten Island NY Card 1234",
				"Cvs/Pharmacy #06 060 Staten Island NY Card 1234",
				"Costco Whse #0316 Staten Island NY Card 1234",
				"I3V*New Dorp Christi 917-399-1846 NY Card 1234"
		};

		for (String vendor : vendors) {
			log.info("Querying for: " + vendor);
			findSimilarVendor(vendor);
		}
	}

	void findSimilarVendor(String rawVendorString) {
		float[] queryVector = embeddingModel.embed(rawVendorString);
		PGvector vector = new PGvector(queryVector);

		List<Map<String, Object>> results = jdbcTemplate.queryForList("""
        SELECT id, description, 1 - (embedding <=> ?::vector) AS score
        FROM entities
        ORDER BY embedding <=> ?::vector
        LIMIT 1
        """,
				vector, vector
		);

		if (results.isEmpty()) {
			log.info("No vendors found");
			return;
		}

		Map<String, Object> row = results.get(0);
		double score = ((Number) row.get("score")).doubleValue();

		Optional<VendorMatch> resp =  Optional.of(new VendorMatch(
				((Number) row.get("id")).longValue(),
				(String) row.get("description"),
				score
		));

		resp.ifPresent(vendorMatch ->
				log.info(vendorMatch.toString() + ", confident: " + vendorMatch.isConfident() + "\n")
		);
	}

//	@Test
	void embedVectorTest() {
		embedVectorService.embedAllVectors();;
	}

	@Test
	void test() {
		log.info("Test");
	}
}

record VendorMatch(long id, String description, double score) {
	public boolean isConfident() {
		return score >= 0.80;
	}
}
