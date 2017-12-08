package SW9.presentations;

import SW9.abstractions.Component;
import SW9.abstractions.ComponentInstance;
import SW9.controllers.ComponentInstanceController;
import SW9.utility.colors.Color;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.util.function.BiConsumer;

/**
 *
 */
public class ComponentInstancePresentation extends StackPane {
    private final ComponentInstanceController controller;

    public ComponentInstancePresentation(final ComponentInstance instance) {
        controller = new EcdarFXMLLoader().loadAndGetController("ComponentInstancePresentation.fxml", this);

        controller.setInstance(instance);

        initializeDimensions(instance);
        initializeFrame();
        initializeBackground();
    }

    private void initializeDimensions(final ComponentInstance instance) {
        setMinWidth(Grid.GRID_SIZE * 24);
        setMaxWidth(Grid.GRID_SIZE * 24);
        setMinHeight(Grid.GRID_SIZE * 12);
        setMaxHeight(Grid.GRID_SIZE * 12);

        instance.getBox().getWidthProperty().bind(widthProperty());
        instance.getBox().getHeightProperty().bind(heightProperty());

        // Bind x and y
        setLayoutX(instance.getBox().getX());
        setLayoutY(instance.getBox().getY());
        instance.getBox().getXProperty().bind(layoutXProperty());
        instance.getBox().getYProperty().bind(layoutYProperty());
    }

    private void initializeFrame() {
        final Component component = controller.getInstance().getComponent();

        final Shape[] mask = new Shape[1];
        final Rectangle rectangle = new Rectangle(getMinWidth(), getMinHeight());

        // Generate first corner (to subtract)
        final Polygon corner1 = new Polygon(
                0, 0,
                ModelPresentation.CORNER_SIZE + 2, 0,
                0, ModelPresentation.CORNER_SIZE + 2
        );

        // Generate second corner (to subtract)
        final Polygon corner2 = new Polygon(
                getMinWidth(), getMinHeight(),
                getMinWidth() - ModelPresentation.CORNER_SIZE - 2, getMinHeight(),
                getMinWidth(), getMinHeight() - ModelPresentation.CORNER_SIZE - 2
        );

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Mask the parent of the frame (will also mask the background)
            mask[0] = Path.subtract(rectangle, corner1);
            mask[0] = Path.subtract(mask[0], corner2);
            controller.frame.setClip(mask[0]);
            controller.background.setClip(Path.union(mask[0], mask[0]));

            // Bind the missing lines that we cropped away
            controller.line1.setStartX(ModelPresentation.CORNER_SIZE);
            controller.line1.setStartY(0);
            controller.line1.setEndX(0);
            controller.line1.setEndY(ModelPresentation.CORNER_SIZE);
            controller.line1.setStroke(newColor.getColor(newIntensity.next(2)));
            controller.line1.setStrokeWidth(1.25);
            StackPane.setAlignment(controller.line1, Pos.TOP_LEFT);

            controller.line2.setStartX(ModelPresentation.CORNER_SIZE);
            controller.line2.setStartY(0);
            controller.line2.setEndX(0);
            controller.line2.setEndY(ModelPresentation.CORNER_SIZE);
            controller.line2.setStroke(newColor.getColor(newIntensity.next(2)));
            controller.line2.setStrokeWidth(1.25);
            StackPane.setAlignment(controller.line2, Pos.BOTTOM_RIGHT);

            // Set the stroke color to two shades darker
            controller.frame.setBorder(new Border(new BorderStroke(
                    newColor.getColor(newIntensity.next(2)),
                    BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY,
                    new BorderWidths(1),
                    Insets.EMPTY
            )));
        };

        // Update color now and on color change
        updateColor.accept(component.getColor(), component.getColorIntensity());
        component.colorProperty().addListener(observable -> updateColor.accept(component.getColor(), component.getColorIntensity()));

    }

    private void initializeBackground() {
        final Component component = controller.getInstance().getComponent();

        // Bind the background width and height to the values in the model
        controller.background.widthProperty().bind(minWidthProperty());
        controller.background.heightProperty().bind(minHeightProperty());

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Set the background color to the lightest possible version of the color
            controller.background.setFill(newColor.getColor(newIntensity.next(-20)));
        };

        // Update color now and on color change
        updateColor.accept(component.getColor(), component.getColorIntensity());
        component.colorProperty().addListener(observable -> updateColor.accept(component.getColor(), component.getColorIntensity()));

    }

}
