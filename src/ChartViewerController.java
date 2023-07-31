package src;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

// Controller for ChartViewer.fxml, the popup page that displays the
// pie charts in the 'output' directory.
public class ChartViewerController implements Initializable {

    @FXML
    Button leftButton, rightButton;

    @FXML
    ImageView leftImg, rightImg;

    // List of charts to display in the ChartViewer window
    private List<Image> charts;

    // Index of current left image displayed (within charts list)
    private int currImgLeft;

    // Index of current right image displayed (within charts list)
    private int currImgRight;

    // Initialize the charts to display. This is called by JavaFX when the
    // GUI is loaded.
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (!charts.isEmpty()) {
            leftImg.setImage(charts.get(currImgLeft));
            rightImg.setImage(charts.get(currImgRight));
        }
        disableLeftRightButtons();
    }

    // Intiialize fields.
    public ChartViewerController(List<Image> charts) {
        this.charts = charts;

        if (charts.isEmpty()) {
            currImgLeft = -1;
            currImgRight = -1;
            return;
        } else if (charts.size()  == 1){
            currImgLeft = 0;
            currImgRight = 0;
        } else {
            currImgLeft = 0;
            currImgRight = 1;
        }
    }

    public void setInitialImages() {
        if (!charts.isEmpty()) {
            leftImg.setImage(charts.get(currImgLeft));
            rightImg.setImage(charts.get(currImgRight));
        }
    }

    public void handleLeftButton() {
            leftImg.setImage(charts.get(--currImgLeft));
            rightImg.setImage(charts.get(--currImgRight));
        disableLeftRightButtons();
    }

    public void handleRightButton() {
            leftImg.setImage(charts.get(++currImgLeft));
            rightImg.setImage(charts.get(++currImgRight));
        disableLeftRightButtons();
    }

    // disable the appropriate left or right button if
    // displaying the first or last image.
    public void disableLeftRightButtons() {
        leftButton.setDisable(false);
        rightButton.setDisable(false);

        if (currImgLeft == -1 || currImgLeft == 0) {
            leftButton.setDisable(true);
        }

        if (currImgRight == -1 || currImgRight == charts.size() - 1) {
            rightButton.setDisable(true);
        }
    }
}
