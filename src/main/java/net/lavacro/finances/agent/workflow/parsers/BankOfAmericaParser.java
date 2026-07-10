package net.lavacro.finances.agent.workflow.parsers;

import lombok.extern.slf4j.Slf4j;
import net.lavacro.finances.agent.dto.StmtTransaction;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
public class BankOfAmericaParser implements StatementParser {
	private static final Pattern pattern = Pattern.compile("^\\d{2}/\\d{2} .*");
	private static final NumberFormat format = NumberFormat.getInstance(Locale.US);

	/**
	 * <p>Receives a PDF file converted into text and extracts transactions</p>
	 *
	 * @param pdf The PDF file as a string
	 * @return A list of transactions objects
	 * <br/>
	 * This parser is very straight-forward. All transactions conform to a common format, whether an expense or payment:
	 * 		12/26 12/26 BA ELECTRONIC PAYMENT 1234 9876 -321.62
	 * 		12/15 12/16 EXXON KINGS PORT RICHM   STATEN ISLANDNY 1122 9876 45.67
	 * 		12/23 12/24 BP#6634117NEW SPRINGVILL STATEN ISLANDNY 3344 9876 56.78
	 * The fields are:
	 * - transaction date
	 * - posted date
	 * - vendor/activty
	 * - location
	 * - transaction id?
	 * - end of card number
	 * - amount
	 * The strategy would be to use the first field, discard the second, use the last field.
	 * From the remaining string, discard the last two fields.
	 */
	@Override
	public List<StmtTransaction> parseStatement(String pdf) {
		log.info("Bank of America parser ...");
		String[] lines = pdf.split("\n");
		List<StmtTransaction> transactions = new ArrayList<>();
		int lineNumber = 1;

		for (String line : lines) {
			if(pattern.matcher(line).matches()) {
				StmtTransaction transaction = new StmtTransaction();

				int from = line.lastIndexOf(' ');

				// number may have a comma for thousands indicator, so ...
				BigDecimal amount;
				try {
					Number number = format.parse(line.substring(from + 1));
					amount = new BigDecimal(number.toString()).multiply(new BigDecimal("-1"));
				} catch(ParseException e) {
					log.error("Unable to parse amount", e);
					amount = BigDecimal.ZERO;
				}

				if(amount.compareTo(BigDecimal.ZERO) == 0) {
					continue;
				}

				transaction.setId(lineNumber++);
				transaction.setAmount(amount);
				transaction.setTransactionDate("2026-" + line.substring(0, 5).replace("/", "-"));
				transaction.setPostedDate(transaction.getTransactionDate()); // faking it

				line = line.substring(12, from);
				// now have "EXXON KINGS PORT RICHM   STATEN ISLANDNY 1122 9876"
				extractValues(transaction, line);

				transactions.add(transaction);
				log.info("date: {}, amount: {}, desc: {}", transaction.getTransactionDate(), amount, transaction.getVendorRaw());
			}
		}

		log.info("Parsed {} transactions in statement", transactions.size());
		return transactions;
	}

	private void extractValues(StmtTransaction transaction, String line) {
		// line is something like "EXXON KINGS PORT RICHM   STATEN ISLANDNY 1122 9876"
		transaction.setVendorRaw(line.substring(0, line.substring(0, line.lastIndexOf(' ')).lastIndexOf(' ')));
	}
}
