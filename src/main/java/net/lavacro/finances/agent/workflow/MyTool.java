package net.lavacro.finances.agent.workflow;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

@Service
public class MyTool {
	@McpTool(description = "TBD")
	String noop(@McpToolParam(description = "TBD") String input) {
		return "noop";
	}
}
