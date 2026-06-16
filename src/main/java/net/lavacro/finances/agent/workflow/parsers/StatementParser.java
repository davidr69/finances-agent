package net.lavacro.finances.agent.workflow.parsers;

import net.lavacro.finances.agent.dto.StmtTransaction;

import java.util.List;

public interface StatementParser {
	List<StmtTransaction> parseStatement(String pdf);
}
