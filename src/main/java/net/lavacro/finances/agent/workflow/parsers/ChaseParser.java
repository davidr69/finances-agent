package net.lavacro.finances.agent.workflow.parsers;

import lombok.extern.slf4j.Slf4j;
import net.lavacro.finances.agent.dto.StmtTransaction;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ChaseParser implements StatementParser {
	private static final Pattern pattern = Pattern.compile("^\\d{2}/\\d{2} .*");
	private static final Pattern vendorPattern = Pattern.compile("\\d{2}/\\d{2} ");
	private static final NumberFormat format = NumberFormat.getInstance(Locale.US);

	/**
	 * <p>Receives a PDF file converted into text and extracts transactions</p>
	 *
	 * @param pdf The PDF file as a string
	 * @return A list of transactions objects
	 * <br/>
	 * There are two possible transaction formations:
	 *   04/27 Card Purchase           04/24 Chick Fil A 3400812 Woodbridge NJ Card 1234 -5.68 11,111.11
	 *   04/27 Con Ed of NY     Cecony     12345678901     CCD ID: 1234567890 -123.45 11,111.11
	 * They have 3 things in common:
	 * 1. they begin with a date in the format of mm/dd
	 * 2. the last field is the balance
	 * 3. the penultimate field is the amount
	 * <br/>
	 * The logic required here is as follows:
	 * - after removing the above-mentioned fields, if there is another mm/dd field, everything after that is the vendor
	 * - otherwise, everything up to two consecutive spaces is the vendor
	 * Rather than immediately split into tokens, we can first deal with substrings, and THEN tokenize the rest.
	 */
	@Override
	public List<StmtTransaction> parseStatement(String pdf, Integer year) {
		log.info("Parsing statement");
		String[] lines = pdf.split("\n");
		boolean started = false;
		List<StmtTransaction> transactions = new ArrayList<>();
		int lineNumber = 1;
		String originalMonth = null;

		for (String line : lines) {
			if(started) {
				if (line.contains("Ending Balance")) {
					started = false;
				}
				if(pattern.matcher(line).matches()) {
					StmtTransaction transaction = new StmtTransaction();

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

					int until = line.lastIndexOf(' ');
					int from = line.lastIndexOf(' ', until - 1);

					// number may have a comma for thousands indicator, so ...
					BigDecimal amount;
					try {
						Number number = format.parse(line.substring(from + 1, until));
						amount = new BigDecimal(number.toString());
					} catch(ParseException e) {
						log.error("Unable to parse amount", e);
						amount = BigDecimal.ZERO;
					}

					transaction.setId(lineNumber++);
					transaction.setAmount(amount);
					transaction.setTransactionDate(
							String.format("%d-%s", year, line.substring(0, 5).replace("/", "-"))
					);
					transaction.setPostedDate(transaction.getTransactionDate());

					line = line.substring(6, from);
					extractValues(transaction, line);

					transactions.add(transaction);
				}
				continue;
			}
			if (line.contains("Beginning Balance")) {
				started = true;
			}
		}

		log.info("Parsed statement");
		return transactions;
	}

	private void extractValues(StmtTransaction transaction, String line) {
		Matcher matcher = vendorPattern.matcher(line);
		if(matcher.find()) {
			// original line had this format:
			// 04/27 Card Purchase           04/24 Chick Fil A 3400812 Woodbridge NJ Card 1234 -5.68 11,111.11
			// This becomes:
			// Card Purchase           04/24 Chick Fil A 3400812 Woodbridge NJ Card 1234
			String date = line.substring(matcher.start(), matcher.end()).trim();
			String actualDate = "2026-" + date.replace("/", "-");
			transaction.setVendorRaw(sanitize(line.substring(matcher.end()).replace(" Card", "")));
			transaction.setVendorRaw(normalizeVendor(transaction.getVendorRaw())); // just testing
			transaction.setTransactionDate(actualDate);
		} else {
			// original line is:
			// 04/27 Con Ed of NY     Cecony     12345678901     CCD ID: 1234567890 -123.45 11,111.11
			// modified line:
			// Con Ed of NY     Cecony     12345678901     CCD ID: 1234567890
			int spacePos = line.indexOf("  ");
			String vendor = spacePos > 0 ? line.substring(0, spacePos) : line;
			transaction.setVendorRaw(vendor);
		}
	}

	private String sanitize(String vendor) {
		return vendor
				.replaceAll("\\b\\d{4}\\b", "")           // 4-digit card numbers
				.replaceAll("\\d{3}-\\d{7,8}", "")         // phone numbers
				.replaceAll("\\d{3}-\\d{3}-\\d{4}", "")    // alternate phone format
				.replaceAll("(CCD|PPD)\\s+ID:\\s*\\w+", "") // transaction IDs
				.replaceAll("\\b[A-Z0-9]{8,}\\b", "")      // long alphanumeric codes
				.replaceAll("\\s{2,}", " ")                 // collapse extra spaces
				.trim();
	}

	private String normalizeVendor(String raw) {
		return raw
				.replaceAll("\\b(Staten Island|New York|Brooklyn|Queens|Bronx|Manhattan)\\b,?\\s*(NY|NJ|CA|MA|MO|WA|UT)?", "")
				.replaceAll("\\b(NY|NJ|CA|MA|MO|WA|UT|CO)\\b", "")
				.replaceAll("\\s{2,}", " ")
				.trim();
	}
}
