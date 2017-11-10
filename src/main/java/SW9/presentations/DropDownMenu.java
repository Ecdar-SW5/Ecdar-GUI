package SW9.presentations;

import SW9.utility.colors.Color;
import SW9.utility.colors.EnabledColor;
import com.jfoenix.controls.JFXPopup;
import javafx.animation.ScaleTransition;
import javafx.beans.binding.When;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.swing.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static SW9.utility.colors.EnabledColor.enabledColors;

public class DropDownMenu {

    public static double x = 0;
    public static double y = 0;
    private final int width;
    private final StackPane content;
    private final VBox list;
    private final JFXPopup popup;
    private final SimpleBooleanProperty isHoveringSubMenu = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty isHoveringMenu = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty canIShowSubMenu = new SimpleBooleanProperty(false);

    public DropDownMenu(final Pane container, final Node source, final int width, final boolean closeOnMouseExit) {
        this.width = width;

        popup = new JFXPopup();

        list = new VBox();
        list.setStyle("-fx-background-color: white; -fx-padding: 8 0 8 0;");
        list.setMaxHeight(1);
        StackPane.setAlignment(list, Pos.TOP_CENTER);

        content = new StackPane(list);
        content.setMinWidth(width);
        content.setMaxWidth(width);

        if (closeOnMouseExit) {
            final Runnable checkIfWeShouldClose = () -> {
                if (!isHoveringMenu.get() && !isHoveringSubMenu.get()) {
                    final Timer timer = new Timer(20, arg0 -> {
                        if (!isHoveringMenu.get() && !isHoveringSubMenu.get()) {
                            close();
                        }
                    });
                    timer.setRepeats(false); // Only execute once
                    timer.start(); // Go go go!
                }
            };
            isHoveringMenu.addListener(observable -> checkIfWeShouldClose.run());
            isHoveringSubMenu.addListener(observable -> checkIfWeShouldClose.run());
        }


        list.setOnMouseExited(event -> isHoveringMenu.set(false));
        list.setOnMouseEntered(event -> isHoveringMenu.set(true));

        popup.setContent(content);
        popup.setPopupContainer(container);
        popup.setSource(source);
    }

    public void close() {
        popup.close();
    }

    public void show(final JFXPopup.PopupVPosition vAlign, final JFXPopup.PopupHPosition hAlign, final double initOffsetX, final double initOffsetY) {
        popup.show(vAlign, hAlign, initOffsetX, initOffsetY);
    }

    public void addListElement(final String s) {
        MenuElement element = new MenuElement(s, width);
        list.getChildren().add(element.getItem());
    }

    public void addClickableListElement(final String s, final Consumer<MouseEvent> mouseEventConsumer) {
        MenuElement element = new MenuElement(s, mouseEventConsumer, width);
        list.getChildren().add(element.getItem());
    }

    public void addTogglableListElement(final String s, final ObservableBooleanValue isToggled, final Consumer<MouseEvent> mouseEventConsumer) {
        MenuElement element = new MenuElement(s, "gmi-done", mouseEventConsumer, width);
        element.setToggleable(isToggled);
        list.getChildren().add(element.getItem());
    }

    public void addTogglableAndDisableableListElement(final String s, final ObservableBooleanValue isToggled, final ObservableBooleanValue isDisableable, final Consumer<MouseEvent> mouseEventConsumer) {
        MenuElement element = new MenuElement(s, "gmi-done", mouseEventConsumer, width);
        element.setToggleable(isToggled);
        element.setDisableable(isDisableable);
        list.getChildren().add(element.getItem());
    }

    public void addClickableAndDisableableListElement(final String s, final ObservableBooleanValue isDisabled, final Consumer<MouseEvent> mouseEventConsumer) {
        MenuElement element = new MenuElement(s, mouseEventConsumer, width);
        element.setDisableable(isDisabled);
        list.getChildren().add(element.getItem());
    }

    public void addSpacerElement() {
        final Region space1 = new Region();
        space1.setMinHeight(8);
        list.getChildren().add(space1);

        final Line sep = new Line(0, 0, width - 1, 0);
        sep.setStroke(Color.GREY.getColor(Color.Intensity.I300));
        list.getChildren().add(sep);

        final Region space2 = new Region();
        space2.setMinHeight(8);
        list.getChildren().add(space2);

        space1.setOnMouseEntered(event -> canIShowSubMenu.set(false));
        space2.setOnMouseEntered(event -> canIShowSubMenu.set(false));
    }

    public void addColorPicker(final HasColor hasColor, final BiConsumer<Color, Color.Intensity> consumer) {
        final FlowPane flowPane = new FlowPane();
        flowPane.setStyle("-fx-padding: 0 8 0 8");

        for (final EnabledColor color : enabledColors) {
            final Circle circle = new Circle(16, color.color.getColor(color.intensity));
            circle.setStroke(color.color.getColor(color.intensity.next(2)));
            circle.setStrokeWidth(1);

            final FontIcon icon = new FontIcon();
            icon.setIconLiteral("gmi-done");
            icon.setFill(color.color.getTextColor(color.intensity));
            icon.setIconSize(20);
            icon.visibleProperty().bind(new When(hasColor.colorProperty().isEqualTo(color.color)).then(true).otherwise(false));

            final StackPane child = new StackPane(circle, icon);
            child.setMinSize(40, 40);
            child.setMaxSize(40, 40);

            child.setOnMouseEntered(event -> {
                final ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(100), circle);
                scaleTransition.setFromX(circle.getScaleX());
                scaleTransition.setFromY(circle.getScaleY());
                scaleTransition.setToX(1.1);
                scaleTransition.setToY(1.1);
                scaleTransition.play();
            });

            child.setOnMouseExited(event -> {
                final ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(100), circle);
                scaleTransition.setFromX(circle.getScaleX());
                scaleTransition.setFromY(circle.getScaleY());
                scaleTransition.setToX(1.0);
                scaleTransition.setToY(1.0);
                scaleTransition.play();
            });

            child.setOnMouseClicked(event -> {
                event.consume();

                // Only color the subject if the user chooses a new color
                if (hasColor.colorProperty().get().equals(color.color)) return;

                consumer.accept(color.color, color.intensity);
            });

            flowPane.getChildren().add(child);
        }

        flowPane.setOnMouseEntered(event -> canIShowSubMenu.set(false));

        addCustomChild(flowPane);
    }

    public void addCustomChild(final Node child) {
        list.getChildren().add(child);
    }

    public interface HasColor {
        ObjectProperty<Color> colorProperty();

        ObjectProperty<Color.Intensity> colorIntensityProperty();
    }
}
