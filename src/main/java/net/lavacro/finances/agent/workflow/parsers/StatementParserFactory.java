package net.lavacro.finances.agent.workflow.parsers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Map;

@Component
@Slf4j
public class StatementParserFactory {

	private final Map<Integer, StatementParser> parsers = Map.of(
			6, new ChaseParser(),
			21, new BankOfAmericaParser(),
			22, new ChaseParser() 		// WF
	);

	public StatementParser getParser(int accountId) {
		return Optional.ofNullable(parsers.get(accountId))
				.orElseThrow(() -> new IllegalArgumentException(
						"No parser registered for account id: " + accountId));
	}
}
