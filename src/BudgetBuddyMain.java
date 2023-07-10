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
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.style.Styler;

public class BudgetBuddyMain {
    // map spending category to amount spent
    static Map<String, Double> expensesByCat = new HashMap<>();

    // map deposit category to amount deposited
    static Map<String, Double> depositsByCat = new HashMap<>();


    // Process a workbook.
    private static void processWorkbook(XSSFWorkbook wb) {
        XSSFSheet april = wb.getSheet("2023-04");

        if (april != null) {
            processSheet(april);
        }

        // Check for empty keys; these are the last rows in the spreadsheet
        // and contain the net gain/loss of the month. This does not belong in
        // the maps.
        double net;
        if (expensesByCat.containsKey("")) {
            net = expensesByCat.get("");
            expensesByCat.remove("");
        } else {
            net = depositsByCat.get("");
            depositsByCat.remove("");
        }

        System.out.println(expensesByCat);
        System.out.println(depositsByCat);
        System.out.println("Net pay: " + net);
    }


    // Process a spreadsheet. Fill the expense and deposit maps.
    // Loop through rows. Ignore first row (header row).
    // Check if each transaction is negative or positive, and put
    // it in the appropriate map.
    private static void processSheet(XSSFSheet month) {
        for (int i=1; i<month.getLastRowNum(); i++) {
            XSSFRow r = month.getRow(i);
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
                updateMap(expensesByCat, cat, money);
            } else if (money > 0) {
                updateMap(depositsByCat, cat, money);
            }
        }
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


    // Make, display, and save the expenses & deposits pie charts.
    private static void showCharts() {
        PieChart expenses = new PieChartBuilder().width(800).height(600).title("April expenses").theme(Styler.ChartTheme.GGPlot2).build();
        PieChart deposits = new PieChartBuilder().width(800).height(600).title("April deposits").theme(Styler.ChartTheme.GGPlot2).build();

        for (Map.Entry<String, Double> entry: expensesByCat.entrySet()) {
            expenses.addSeries(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Double> entry: depositsByCat.entrySet()) {
            deposits.addSeries(entry.getKey(), entry.getValue());
        }

        new SwingWrapper<>(expenses).displayChart();
        new SwingWrapper<>(deposits).displayChart();

        // Save them to img directory
        try {
            BitmapEncoder.saveBitmap(expenses, "img/April-Expenses", BitmapEncoder.BitmapFormat.PNG);
            BitmapEncoder.saveBitmap(deposits, "img/April-Deposits", BitmapEncoder.BitmapFormat.PNG);
        } catch (IOException e) {
            System.out.println("IOException...");
            e.printStackTrace();
        }

    }

    // Open "Finances" workbook. Get expense/deposit data using Apache POI.
    // Create pie charts using XChart and display in a window.
    public static void main(String[] args) {
        File f = new File("Finances.xlsx");
        try (XSSFWorkbook wb = new XSSFWorkbook(f)) {
            processWorkbook(wb);
        } catch (IOException | InvalidFormatException e) {
            System.out.println("An exception appeared.");
            e.printStackTrace();
        }

        showCharts();
    }

}
