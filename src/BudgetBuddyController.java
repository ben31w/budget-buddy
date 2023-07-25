package src;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.apache.commons.io.FileUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.style.PieStyler;
import org.knowm.xchart.style.Styler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetBuddyController {

    @FXML
    private TextField excelTF, outputTF;

    @FXML
    private TextArea msgLog;


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
            excelTF.setText(selected.getPath());
        }
    }


    // When the user clicks 'Create Charts', attempt to process the file inside
    // the Excel textfield. Only proceed if it's an Excel file that exists.
    public void processFile() {
        clearDefaultOutputDir();
        String filename = excelTF.getText();

        if (filename.endsWith(".xlsx")) {
            File f = new File(filename);
            if (f.exists()) {
                try (XSSFWorkbook wb = new XSSFWorkbook(f)) {
                    processWorkbook(wb);
                    String msg = "Created images for '" + f.getPath() + "'. Images saved to 'output' directory.\n";
                    msgLog.appendText(msg);
                } catch (IOException | InvalidFormatException e) {
                    msgLog.appendText("This file could not be processed: " + f.getPath() + "\n");
                    e.printStackTrace();
                }
            } else {
                msgLog.appendText("This Excel file could not be found. Check the spelling perhaps: " + f.getPath() + "\n");
            }
        } else {
            msgLog.appendText("Please select an Excel file.\n");
        }
    }

    private void clearDefaultOutputDir() {
        try {
            FileUtils.deleteDirectory(new File("output"));
        } catch (IOException e) {

        } finally {
            new File("output").mkdirs();
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

        // Styling the pie charts' legend and labels
        PieStyler eps = expensesPC.getStyler();
        eps.setLegendPosition(Styler.LegendPosition.InsideSW);
        eps.setLabelType(PieStyler.LabelType.NameAndPercentage);
        eps.setLabelsVisible(true);
        eps.setLabelsDistance(.8);

        PieStyler dps = depositsPC.getStyler();
        dps.setLegendPosition(Styler.LegendPosition.InsideSW);
        dps.setLabelType(PieStyler.LabelType.NameAndPercentage);
        dps.setLabelsVisible(true);
        dps.setLabelsDistance(.8);

        for (Map.Entry<String, Double> entry: expenses.entrySet()) {
            expensesPC.addSeries(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Double> entry: deposits.entrySet()) {
            depositsPC.addSeries(entry.getKey(), entry.getValue());
        }

        // Save them to output directory & add them to the charts list
        try {
            String expensePath = String.format("output/%s-expenses", date);
            String depositPath = String.format("output/%s-deposits", date);
            BitmapEncoder.saveBitmap(expensesPC, expensePath, BitmapEncoder.BitmapFormat.PNG);
            BitmapEncoder.saveBitmap(depositsPC, depositPath, BitmapEncoder.BitmapFormat.PNG);
        } catch (IOException  | IllegalArgumentException e) {
            // unlikely this exception will occur, but might want to add some better error handling.
            // IllegalArgumentException: Image couldn't be created from string. Maybe have better handling for this.
            e.printStackTrace();
        }
    }
}
