package net.lavacro.finances.agent.config;

public class ThreadLocalContext {
	private ThreadLocalContext() { }

	private static final ThreadLocal<String> statementBatch = ThreadLocal.withInitial(() -> null);

	public static String getStatementBatch() { return statementBatch.get(); }
	public static void setStatementBatch(String batch) { statementBatch.set(batch); }
	public static void clearStatementBatch() { statementBatch.remove(); }
}
