package net.lavacro.finances.agent.workflow.parsers;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ChaseParser implements StatementParser {

	@Override
	public String parseStatement(String pdf) {
		log.info("Parsing statement");
		String[] lines = pdf.split("\n");
		boolean started = false;
		List<String> lineItems = new ArrayList<>();

		for (String line : lines) {
			if(started) {
				if (line.contains("Ending Balance")) {
					started = false;
				}
				log.info(line);
				lineItems.add(line);
				continue;
			}
			if (line.contains("Beginning Balance")) {
				started = true;
			}
		}

		return String.join("\n", lineItems);
	}
}
