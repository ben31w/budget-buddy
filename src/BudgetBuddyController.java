package src;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.NameFileComparator;
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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class BudgetBuddyController implements Initializable {

    @FXML
    private TextField excelTF, outputTF;

    @FXML
    private TextArea msgLog;

    // stores the user's custom output directory to save their images.
    // (set to empty string if the directory is not valid)
    private String customOutputDir;


    // Clear 'output' directory when BudgetBuddy initializes.
    public void initialize(URL url, ResourceBundle rb) {
        clearDefaultOutputDir();
    }


    // Open a file chooser so the user can select an Excel file as input.
    // The filename is put inside a textfield which is later read from
    // when creating charts.
    public void openExcelFileChooser() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open Excel File");
        fc.getExtensionFilters().addAll(
                new ExtensionFilter("Excel Files", "*.xlsx"));
        File selected = fc.showOpenDialog(null);
        if (selected != null) {
            excelTF.setText(selected.getPath());
        }
    }


    // Open a file chooser so the user can select a directory to save
    // the pie chart images to.
    public void openOutputDirectoryChooser() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select a directory");
        dc.setInitialDirectory(new File(System.getProperty("user.dir")));
        File selected = dc.showDialog(null);
        if (selected != null) {
            outputTF.setText(selected.getPath());
        }
    }


    // When the user clicks 'Create Charts', attempt to process the file inside
    // the Excel textfield. Only proceed if it's an Excel file that exists.
    public void processFile() {
        clearDefaultOutputDir();
        setCustomOutputDir();
        String msg; // message to display to the user.

        String filename = excelTF.getText();
        if (filename.endsWith(".xlsx")) {
            File f = new File(filename);
            if (f.exists()) {
                try (XSSFWorkbook wb = new XSSFWorkbook(f)) {
                    processWorkbook(wb);
                    msg = "Created images for '" + f.getPath() + "'.\n";

                    // If customOutputDir is set, then we have a valid directory to save to.
                    // If it's not set but the textfield contains something, then the user entered an invalid directory.
                    if (!customOutputDir.isEmpty()) {
                        msg += "Images saved to  '" + customOutputDir + "'.\n";
                    } else if (!outputTF.getText().isEmpty()) {
                        msg += "Failed to save to '" + outputTF.getText() + "'. Directory not found; check the spelling.\n";
                    }
                } catch (IOException | InvalidFormatException e) {
                    msg = "This file could not be processed: '" + f.getPath() + "'.\n";
                    e.printStackTrace();
                }
            } else {
                msg = "This Excel file could not be found. Check the spelling perhaps: '" + f.getPath() + "'.\n";
            }
        } else {
            msg = "Please select an Excel file.\n";
        }

        msgLog.appendText(msg);
    }


    // When the user clicks 'Display Charts', get the list of charts stored
    // in the default 'output' directory, and display them in a new window.
    public void displayCharts() {
        List<Image> charts = getChartsFromDefaultOutputDir();

        Stage chartsStage = new Stage();
        FXMLLoader loader = new FXMLLoader();
        ChartViewerController c = new ChartViewerController(charts);
        try {
            loader.setLocation(getClass().getResource("ChartViewer.fxml"));
            loader.setController(c);
            Parent root = loader.load();
            chartsStage.setScene(new Scene(root));
            chartsStage.setTitle("Chart Viewer");
            chartsStage.setY(450);
            chartsStage.show();
        } catch (IOException e) {
            msgLog.appendText("An exception appeard. Failed to display charts.");
        }
    }


    // Clear the contents of the 'output' directory.
    private void clearDefaultOutputDir() {
        try {
            FileUtils.deleteDirectory(new File("output"));
        } catch (IOException e) {

        } finally {
            new File("output").mkdirs();
        }
    }

    // Check the custom output directory textfield and set the field it's a valid directory.
    private void setCustomOutputDir() {
        customOutputDir = "";

        String dir = outputTF.getText();
        if (!dir.isEmpty()) {
            Path p = Paths.get(dir);
            if (Files.exists(p)) {
                customOutputDir = dir;
            }
        }
    }


    // Check 'output' directory for pie chart images.
    // Return charts as an ArrayList<Image>.
    private List<Image> getChartsFromDefaultOutputDir() {
        List<Image> charts = new ArrayList<>();

        File[] images = new File("output").listFiles();
        Arrays.sort(images, NameFileComparator.NAME_COMPARATOR);
        for (File chartPng : images) {
            charts.add(   new Image(String.valueOf(chartPng)) );
        }

        return charts;
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

        // Save them to output directory & the user's custom directory if provided.
        try {
            String expensePath = String.format("output/%s-expenses", date);
            String depositPath = String.format("output/%s-deposits", date);
            BitmapEncoder.saveBitmap(expensesPC, expensePath, BitmapEncoder.BitmapFormat.PNG);
            BitmapEncoder.saveBitmap(depositsPC, depositPath, BitmapEncoder.BitmapFormat.PNG);

            // user entered a custom directory
            if (!customOutputDir.isEmpty()) {
                String customExpensePath =  String.format(customOutputDir + "/%s-expenses", date);
                String customDepositPath = String.format(customOutputDir + "/%s-deposits", date);
                BitmapEncoder.saveBitmap(expensesPC, customExpensePath, BitmapEncoder.BitmapFormat.PNG);
                BitmapEncoder.saveBitmap(depositsPC, customDepositPath, BitmapEncoder.BitmapFormat.PNG);
            }
        } catch (IOException  | IllegalArgumentException e) {
            // unlikely this exception will occur, but might want to add some better error handling.
            // IllegalArgumentException: Image couldn't be created from string. Maybe have better handling for this.
            e.printStackTrace();
        }
    }
}
