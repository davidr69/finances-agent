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
public class WellsFargoParser implements StatementParser {
	private static final Pattern pattern = Pattern.compile("^(\\d{4}\\s+)?\\d{2}/\\d{2} .*");
	private static final Pattern cardTransaction = Pattern.compile("^\\d{4}\\s.*");
	private static final NumberFormat format = NumberFormat.getInstance(Locale.US);

	/**
	 * <p>Receives a PDF file converted into text and extracts transactions</p>
	 *
	 * @param pdf The PDF file as a string
	 * @return A list of transactions objects
	 * <br/>
	 * This parser is very straight-forward. All transactions conform to a common format, whether an expense or payment:
	 * 		12/26 12/26 1234567890ABCDEFG ONLINE ACH PAYMENT        THANK YOU 123.45
	 * 		1234 12/14 12/14 1234567890ABCDEFG AMAZON MKTPL*A1B2C3D4E Amzn.com/bill WA 98.25
	 * 		1234 12/17 12/17 9876543210ZYXWVUT AMAZON MKTPL*5F6G7H9I0 Amzn.com/bill WA 88.44
	 * The fields are:
	 * - Card Ending In
	 * - transaction date
	 * - posted date
	 * - reference number
	 * - description
	 * - amount
	 * The strategy would be to use the first field, discard the second, use the last field.
	 * From the remaining string, discard the last two fields.
	 */
	@Override
	public List<StmtTransaction> parseStatement(String pdf, Integer year) {
		log.info("Wells Fargo parser ...");
		String[] lines = pdf.split("\n");
		List<StmtTransaction> transactions = new ArrayList<>();
		int lineNumber = 1;
		String originalMonth = null;

		for (String line : lines) {
			if(pattern.matcher(line).matches()) {
				StmtTransaction transaction = new StmtTransaction();

				if(cardTransaction.matcher(line).matches()) {
					line = line.substring(5);
				}

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

				String month = line.substring(0, 2);
				if(originalMonth == null) {
					originalMonth = month;
				} else {
					if(!month.equals(originalMonth) && "01".equals(month)) {
						// must have wrapped to next year
						originalMonth = month;
						year++;
					}
				}

				transaction.setId(lineNumber++);
				transaction.setAmount(amount);
				transaction.setTransactionDate(
						String.format("%d-%s", year, line.substring(0, 5).replace("/", "-"))
				);
				transaction.setPostedDate(transaction.getTransactionDate()); // faking it
				transaction.setVendorRaw(line.substring(30, from));

				transactions.add(transaction);
				log.info("date: {}, amount: {}, desc: {}", transaction.getTransactionDate(), amount, transaction.getVendorRaw());

				log.info(line);
			}
		}

		log.info("Parsed {} transactions in statement", transactions.size());
		return transactions;
	}
}
