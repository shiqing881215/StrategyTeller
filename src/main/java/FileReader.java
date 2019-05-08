import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.common.collect.Lists;

public class FileReader {

	public List<DateAndPrice> readFile(String filePath) throws IOException {
		FileInputStream file = new FileInputStream(filePath);
		
		// Get the workbook instance for the XLS file
		XSSFWorkbook workbook = new XSSFWorkbook(file);
		
		// Get the first sheet
		XSSFSheet sheet = workbook.getSheetAt(0);
		
		// Get all the rows of the current sheet
		Iterator<Row> rowIterator = sheet.iterator();
		
		List<DateAndPrice> prices = Lists.newArrayList();
		
		while (rowIterator.hasNext()) {
			Row r = rowIterator.next();
			// reverse the order
			prices.add(0,new DateAndPrice(r.getCell(0).toString(), Double.parseDouble(r.getCell(1).toString())));
		}
		
		return prices;
	}
	
	public static void main(String[] args) throws IOException {
		final String filePath = "/Users/qshi/Downloads/HistoricalQuotes.xlsx";
		
		FileReader reader = new FileReader();
		
		System.out.println(reader.readFile(filePath));
	}
}
