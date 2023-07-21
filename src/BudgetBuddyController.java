package src;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.style.Styler;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BudgetBuddyController {
    @FXML
    private ImageView imgView;

    @FXML
    private Label msgLabel;

    @FXML
    private TextField textField;


    // Open a file chooser so the user can select an Excel file as input.
    // The filename is put inside a textfield which is later read from
    // when creating charts.
    public void openFileChooser() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open Excel File");
        fc.getExtensionFilters().addAll(
                new ExtensionFilter("Excel Files", "*.xlsx"));
        File selected = fc.showOpenDialog(null);
        if (selected != null) {
            textField.setText(selected.getPath());
        }
    }


    // When the user clicks 'Create Charts', attempt to process the file inside
    // BudgetBuddy's textfield. Only proceed if it's an Excel file that exists.
    public void processFile() {
        String filename = textField.getText();

        if (filename.endsWith(".xlsx")) {
            File f = new File(filename);
            if (f.exists()) {
                try (XSSFWorkbook wb = new XSSFWorkbook(f)) {
                    processWorkbook(wb);
                    msgLabel.setText("Finished processing '" + f.getName() + "'. Check the 'output' directory.");
                } catch (IOException | InvalidFormatException e) {
                    msgLabel.setText("This file could not be processed.");
                    e.printStackTrace();
                }
            } else {
                msgLabel.setText("This Excel file could not be found. Check the spelling perhaps.");
            }
        } else {
            msgLabel.setText("Please select an Excel file.");
        }

        displayChartInGUI();
    }


    private void displayChartInGUI() {
        // TODO is this the right directory to use all the time?
        //  It works in IntelliJ but should it be up one directory?
        Image image = new Image("output/2023-04-deposits.png");
        if (!image.isError()) {
            imgView.setImage(image);
        }
    }


    // Process a workbook. Iterate through each sheet, and get expense & deposit
    // data using Apache POI.
    private void processWorkbook(XSSFWorkbook wb) {
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
    private void processSheet(XSSFSheet month, Map<String, Double> expenses, Map<String, Double> deposits) {
        for (int i=1; i<month.getLastRowNum(); i++) {
            XSSFRow r = month.getRow(i);

            // getLastRowNum() might return more rows than necessary, so we
            // need to check for empty rows
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


    // Update the value in the given map. If this category is already in the map,
    // add to its existing total. Otherwise, intialize the category.
    private void updateMap(Map<String, Double> map, String cat, double value) {
        if (map.containsKey(cat)) {
            double currVal = map.get(cat);
            map.replace(cat, currVal + value);
        } else {
            map.put(cat, value);
        }
    }


    // Create expenses and deposits pie charts using the given Maps.
    private void createCharts(String date, Map<String, Double> expenses, Map<String, Double> deposits) {
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
            // unlikely this exception will occur, but might want to add some better error handling.
            e.printStackTrace();
        }
    }
}
