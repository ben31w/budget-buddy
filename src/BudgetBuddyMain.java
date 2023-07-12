package src;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.style.Styler;

public class BudgetBuddyMain {

    // Process a workbook. Iterate through each sheet, and get expense & deposit
    // data using Apache POI.
    private static void processWorkbook(XSSFWorkbook wb) {
        for (int i=0; i<wb.getNumberOfSheets(); i++) {
            Map<String, Double> expenseMap = new HashMap<>();
            Map<String, Double> depositMap = new HashMap<>();

            XSSFSheet sheet = wb.getSheetAt(i);
            processSheet(sheet, expenseMap, depositMap);
        }
    }


    // Process this spreadsheet and fill the given expense and deposit maps.
    // Ignore first row (header row). Ideally skip the last row (monthly total),
    // though getLastRowNum() might not return the actual last row.
    // Check if each transaction is negative or positive, and put
    // it in the appropriate map.
    // After filling the maps, call createCharts()
    private static void processSheet(XSSFSheet month, Map<String, Double> expenses, Map<String, Double> deposits) {
        for (int i=1; i<month.getLastRowNum(); i++) {
            XSSFRow r = month.getRow(i);

            // getLastRowNum() might return more rows than necessary, so we
            // need to check for empyt rows
            if (r == null) {
                break;
            }

            double money;
            String cat;

            try {
                money = r.getCell(1).getNumericCellValue();
                cat = r.getCell(3).getStringCellValue();
            } catch (NullPointerException e) {
                continue;
            }

            if (money < 0) {
                updateMap(expenses, cat, money);
            } else if (money > 0) {
                updateMap(deposits, cat, money);
            }
        }

        // Remove empty keys; these are the last rows in the spreadsheet
        // and contain the net gain/loss of the month. This does not belong in
        // the maps.
        if (expenses.containsKey("")) {
            expenses.remove("");
        } else if (deposits.containsKey("")) {
            deposits.remove("");
        }

        createCharts(month.getSheetName(), expenses, deposits);
    }


    // Update the value in a map. If there is already a value for this
    // category, add to it. Otherwise, initialize a new value.
    private static void updateMap(Map<String, Double> map, String cat, double value) {
        if (map.containsKey(cat)) {
            double currVal = map.get(cat);
            map.replace(cat, currVal + value);
        } else {
            map.put(cat, value);
        }
    }


    // Create expenses and deposits pie charts using the given Maps.
    private static void createCharts(String date, Map<String, Double> expenses, Map<String, Double> deposits) {
        PieChart expensesPC = new PieChartBuilder().width(800).height(600).title(date + " expenses").theme(Styler.ChartTheme.GGPlot2).build();
        PieChart depositsPC = new PieChartBuilder().width(800).height(600).title(date + " deposits").theme(Styler.ChartTheme.GGPlot2).build();

        for (Map.Entry<String, Double> entry: expenses.entrySet()) {
            expensesPC.addSeries(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Double> entry: deposits.entrySet()) {
            depositsPC.addSeries(entry.getKey(), entry.getValue());
        }

        // Save them to output directory
        try {
            String expensePath = String.format("output/%s-expenses", date);
            String depositPath = String.format("output/%s-deposits", date);
            BitmapEncoder.saveBitmap(expensesPC, expensePath, BitmapEncoder.BitmapFormat.PNG);
            BitmapEncoder.saveBitmap(depositsPC, depositPath, BitmapEncoder.BitmapFormat.PNG);
        } catch (IOException e) {
            System.out.println("IOException...");
            e.printStackTrace();
        }
    }

    // Open & process "Finances" workbook.
    public static void main(String[] args) {
        File f = new File("input/Finances.xlsx");
        try (XSSFWorkbook wb = new XSSFWorkbook(f)) {
            processWorkbook(wb);
        } catch (IOException | InvalidFormatException e) {
            System.out.println("An exception appeared.");
            e.printStackTrace();
        }
    }

}
