package SW9.controllers;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

/**
 *
 */
public class ModelController {
    public StackPane root;
    public Rectangle background;
    public BorderPane frame;
    public Rectangle rightAnchor;
    public Rectangle bottomAnchor;
    public Line topLeftLine;
    public BorderPane toolbar;

    /**
     * Hides the border and background.
     */
    void hideBorderAndBackground() {
        frame.setVisible(false);
        topLeftLine.setVisible(false);
        background.setVisible(false);
    }

    /**
     * Shows the border and background.
     */
    void showBorderAndBorder() {
        frame.setVisible(true);
        topLeftLine.setVisible(true);
        background.setVisible(true);
    }
}
