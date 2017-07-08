package home;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Main {

	private static final String COUNTRY_URI = "https://aviation-safety.net/database/country/";
	private static final String COUNTRY_INFO_URI = "https://aviation-safety.net/database/dblist.php?Country={0}";
	private static final String COUNTRY_INFO_PAGE = "https://aviation-safety.net/database/dblist.php?Country={0}&page={1}";

	private static final String VAR_FILE = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
	private static final String EXCEL_FILE = "accidents_" + VAR_FILE + ".xlsx";

	public static void main(String[] args) {
		try {
			long startTime = System.currentTimeMillis();
			Map<String, String> countries = findCountries();
			Map<String, Integer> pages = findPagesPerCountry(countries.keySet());
			List<AccidentDto> accidents = findAccidentsPerCountry(countries, pages);
			writeToExcel(accidents);
			long endTime = System.currentTimeMillis();
			long duration = (endTime - startTime);
			System.out.println("EXECUTION TIME: " + duration / 1000 + "s");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Map<String, String> findCountries() throws IOException {
		String link = COUNTRY_URI;
		Document doc = Jsoup.connect(link).get();
		Elements links = doc.select("a[href^=country.php?id=]");
		Map<String, String> countries = new HashMap<>();

		for (Element lnk : links) {
			String countryCode = lnk.attr("href").substring("country.php?id=".length());
			String countryName = lnk.text();
			countries.put(countryCode, countryName);
		}
		return countries;
	}

	private static Map<String, Integer> findPagesPerCountry(Iterable<String> countryCodes) throws IOException {

		Map<String, Integer> pagesPerCountry = new HashMap<>();

		for (String countryCode : countryCodes) {
			String link = MessageFormat.format(COUNTRY_INFO_URI, countryCode);
			Document doc = Jsoup.connect(link).get();
			Element lastPageDiv = doc.select("div.pagenumbers > a").last();
			Integer pages = lastPageDiv == null ? 1 : Integer.valueOf(lastPageDiv.text());
			pagesPerCountry.put(countryCode, pages);
		}

		return pagesPerCountry;
	}

	private static List<AccidentDto> findAccidentsPerCountry(Map<String, String> countries, Map<String, Integer> pages)
			throws IOException {

		List<AccidentDto> list = new ArrayList<>();

		for (String countryCode : countries.keySet()) {
			Integer totPages = pages.get(countryCode);

			for (int currPage = 1; currPage <= totPages; currPage++) {
				String link = MessageFormat.format(COUNTRY_INFO_PAGE, countryCode, currPage);
				Document doc = Jsoup.connect(link).get();
				Element table = doc.select("div.innertube").select("table").last();

				if (table != null) {

					Elements rows = table.select("tr");

					for (int i = 1; i < rows.size(); i++) {
						Elements cells = rows.get(i).select("td");

						SimpleDateFormat parser = new SimpleDateFormat("DD-MMM-YYYY");
						Date date;
						try {
							date = parser.parse(cells.get(0).select("a").text());

						} catch (ParseException e) {
							date = null;
						}

						String typeStr = cells.get(1).select("nobr").text();
						String registrationStr = cells.get(2).text();
						String operatorStr = cells.get(3).text();
						Integer fatalitiesInt;

						try {
							fatalitiesInt = Integer.valueOf(cells.get(4).text());
						} catch (NumberFormatException e) {
							fatalitiesInt = 0;
						}

						String locationStr = cells.get(5).text().endsWith("...")
								? cells.get(5).text().substring(0, cells.get(5).text().length() - 4)
								: cells.get(5).text();

						char[] categortyChars = cells.get(8).text().toUpperCase().toCharArray();
						String category;
						switch (categortyChars[0]) {
						case 'A':
							category = "Accident";
							break;
						case 'I':
							category = "Incident";
							break;
						case 'H':
							category = "Hijacking";
							break;
						case 'C':
							category = "Criminal";
							break;
						default:
							category = "Other";
						}

						String damage = categortyChars[1] == '2' ? "Repairable" : "Hull-Loss";

						String country = countries.get(countryCode);

						String[] planeTypeAndModel = typeStr.split(" ", 2);

						String planeType = planeTypeAndModel[0];
						String planeModel = planeTypeAndModel.length == 2 ? planeTypeAndModel[1] : "";

						list.add(new AccidentDto(date, country, locationStr, planeType, planeModel, operatorStr,
								registrationStr, category, damage, fatalitiesInt));
					}
				}
			}
		}

		return list;
	}

	private static void writeToExcel(List<AccidentDto> accidents) throws IOException {

		Workbook wb = new XSSFWorkbook();

		CellStyle header = wb.createCellStyle();
		header.setAlignment(HorizontalAlignment.CENTER);
		header.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
		header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		Font headerFont = wb.createFont();
		headerFont.setBold(true);
		header.setFont(headerFont);

		Sheet sheet = wb.createSheet("ACCIDENTS");

		Row headerRow = sheet.createRow(0);
		Cell headerCell;

		headerCell = headerRow.createCell(0);
		headerCell.setCellStyle(header);
		headerCell.setCellValue("Date");

		headerCell = headerRow.createCell(1);
		headerCell.setCellStyle(header);
		headerCell.setCellValue("Country");

		headerCell = headerRow.createCell(2);
		headerCell.setCellStyle(header);
		headerCell.setCellValue("Location");

		headerCell = headerRow.createCell(3);
		headerCell.setCellStyle(header);
		headerCell.setCellValue("Type");

		headerCell = headerRow.createCell(4);
		headerCell.setCellStyle(header);
		headerCell.setCellValue("Model");

		headerCell = headerRow.createCell(5);
		headerCell.setCellStyle(header);
		headerCell.setCellValue("Operator");

		headerCell = headerRow.createCell(6);
		headerCell.setCellStyle(header);
		headerCell.setCellValue("Registration");

		headerCell = headerRow.createCell(7);
		headerCell.setCellStyle(header);
		headerCell.setCellValue("Accident Type");

		headerCell = headerRow.createCell(8);
		headerCell.setCellStyle(header);
		headerCell.setCellValue("Damage");

		headerCell = headerRow.createCell(9);
		headerCell.setCellStyle(header);
		headerCell.setCellValue("Fatalities");

		sheet.createFreezePane(0, 1);

		for (int rowIndex = 1; rowIndex <= accidents.size(); rowIndex++) {

			AccidentDto accident = accidents.get(rowIndex - 1);

			Row row = sheet.createRow(rowIndex);
			Cell cell;

			String dateStr = accident.getDate() != null ? new SimpleDateFormat("yyyy-MM-dd").format(accident.getDate())
					: "";

			cell = row.createCell(0);
			cell.setCellValue(dateStr);

			cell = row.createCell(1);
			cell.setCellValue(accident.getCountry());

			cell = row.createCell(2);
			cell.setCellValue(accident.getLocation());

			cell = row.createCell(3);
			cell.setCellValue(accident.getType());

			cell = row.createCell(4);
			cell.setCellValue(accident.getModel());

			cell = row.createCell(5);
			cell.setCellValue(accident.getOperator());

			cell = row.createCell(6);
			cell.setCellValue(accident.getRegistration());

			cell = row.createCell(7);
			cell.setCellValue(accident.getAccident());

			cell = row.createCell(8);
			cell.setCellValue(accident.getDamage());

			cell = row.createCell(9);
			cell.setCellType(CellType.NUMERIC);
			cell.setCellValue(accident.getFatalities());
		}

		sheet.autoSizeColumn(0);
		sheet.autoSizeColumn(1);
		sheet.autoSizeColumn(2);
		sheet.autoSizeColumn(3);
		sheet.autoSizeColumn(4);
		sheet.autoSizeColumn(5);
		sheet.autoSizeColumn(6);
		sheet.autoSizeColumn(7);
		sheet.autoSizeColumn(8);
		sheet.autoSizeColumn(9);

		FileOutputStream fileOut = new FileOutputStream(EXCEL_FILE);
		wb.write(fileOut);
		fileOut.close();
		wb.close();

	}

}