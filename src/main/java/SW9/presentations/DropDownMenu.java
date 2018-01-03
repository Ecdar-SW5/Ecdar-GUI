package SW9.presentations;

import SW9.utility.colors.Color;
import SW9.utility.colors.EnabledColor;
import com.jfoenix.controls.JFXPopup;
import com.jfoenix.controls.JFXRippler;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
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
import static javafx.scene.paint.Color.TRANSPARENT;

/**
 * DropDownMenu is a {@link JFXPopup} which is used as the right-click menu and options menu on for instance
 * {@link QueryPresentation#actionButton}.
 * The DropDownMenu includes methods for adding elements to the menu itself.
 *
 * Batteries included
 */
public class DropDownMenu extends JFXPopup {
    public static double x = 0;
    public static double y = 0;
    private final Node source;
    /**
     * The width of the {@link DropDownMenu}.
     * the value 230 showed to be a good base for all {@link DropDownMenu}s.
     * In this way the {@link DropDownMenu} will always stay consistent
     */
    private final int width = 230;

    /**
     * The {@link StackPane} with all the added content from the add methods
     */
    private final StackPane content;
    private final VBox list;

    private final SimpleBooleanProperty isHidden = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty isHoveringMenu = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty isHoveringSubMenu = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty canIShowSubMenu = new SimpleBooleanProperty(false);

    /**
     * Constructor for the {@link DropDownMenu}.
     * The {@link DropDownMenu} is places on top of all the views, and does even go outside of the window/screen for now.
     * @param src The view where the {@link DropDownMenu} should be shown
     */
    public DropDownMenu(final Node src) {
        list = new VBox();
        list.setStyle("-fx-background-color: white; -fx-padding: 8 0 8 0;");
        list.setMaxHeight(1);
        StackPane.setAlignment(list, Pos.TOP_CENTER);

        content = new StackPane(list);
        content.setMinWidth(width);
        content.setMaxWidth(width);

        list.setOnMouseExited(event -> isHoveringMenu.set(false));
        list.setOnMouseEntered(event -> isHoveringMenu.set(true));

        this.setPopupContent(content);
        source = src;

        initializeClosingClock();
    }

    /**
     * Initializer for the closing clock.
     * Makes sure that the {@link DropDownMenu} closes/hides after a short time
     */
    private void initializeClosingClock() {
        final Runnable checkIfWeShouldClose = () -> {
            if (!isHoveringMenu.get() && !isHoveringSubMenu.get()) {
                final Timer timer = new Timer(400, arg0 -> {
                        Platform.runLater(() -> {
                        if (!isHoveringMenu.get() && !isHoveringSubMenu.get()) {
                            hide();
                        }
                    });
                });
                timer.setRepeats(false); // Only execute once
                timer.start(); // Go go go!
            }
        };
        isHoveringMenu.addListener(observable -> checkIfWeShouldClose.run());
        isHoveringSubMenu.addListener(observable -> checkIfWeShouldClose.run());
    }

    /**
     * Shows the DropDownMenu with the added content, gotten from the add* methods
     * Remember to call {@link #hide()} when you want to hide the {@link DropDownMenu}
     * @param vAlign Vertically alignment
     * @param hAlign Horizontal alignment
     * @param initOffsetX Offset on the X-axis of the view
     * @param initOffsetY Offset on the Y-axis of the view
     */
    public void show(final JFXPopup.PopupVPosition vAlign, final JFXPopup.PopupHPosition hAlign, final double initOffsetX, final double initOffsetY) {
        this.isHidden.set(false);
        super.show(source, vAlign, hAlign, initOffsetX, initOffsetY);
    }

    /**
     * Adds a {@link MenuElement} to the {@link DropDownMenu}
     * @param s The title of the element
     */
    public void addListElement(final String s) {
        final MenuElement element = new MenuElement(s);
        addHideListener(element);
        list.getChildren().add(element.getItem());
    }

    /**
     * Adds a clickable {@link MenuElement} to the {@link DropDownMenu}
     * @param s The title of the element
     * @param mouseEventConsumer The event that is triggered when clicked
     */
    public void addClickableListElement(final String s, final Consumer<MouseEvent> mouseEventConsumer) {
        final MenuElement element = new MenuElement(s, mouseEventConsumer);
        addHideListener(element);
        list.getChildren().add(element.getItem());
    }

    /**
     * Adds a toggleable {@link MenuElement} to the {@link DropDownMenu}
     * @param s The title of the element
     * @param isToggled Defines if the element should be disabled of enabled.
     *                  Enabled if it is true, disabled if false
     * @param mouseEventConsumer The event that is triggered when clicked
     */
    public void addToggleableListElement(final String s, final ObservableBooleanValue isToggled, final Consumer<MouseEvent> mouseEventConsumer) {
        final MenuElement element = new MenuElement(s, "gmi-done", mouseEventConsumer);
        addHideListener(element);
        element.setToggleable(isToggled);
        list.getChildren().add(element.getItem());
    }

    /**
     * Adds a toggleable and disableable {@link MenuElement} to {@link #content}
     * @param s The title of the element
     * @param isToggled Defines if the element should be disabled of enabled.
     *                  Enabled if it is true, disabled if false
     * @param isDisabled Determines whether it is currently disabled or enabled.
     *                      True means disabled, false means enabled
     * @param mouseEventConsumer The event that is triggered when clicked
     */
    public void addToggleableAndDisableableListElement(final String s, final ObservableBooleanValue isToggled, final ObservableBooleanValue isDisabled, final Consumer<MouseEvent> mouseEventConsumer) {
        final MenuElement element = new MenuElement(s, "gmi-done", mouseEventConsumer);
        addHideListener(element);
        element.setToggleable(isToggled);
        element.setDisableable(isDisabled);
        list.getChildren().add(element.getItem());
    }

    /**
     * Adds a clickable and disableable {@link MenuElement} to {@link #content}
     * @param s The title of the element
     * @param isDisabled Determines whether it is currently disabled or enabled.
     *                      True means disabled, false means enabled
     * @param mouseEventConsumer The event that is triggered when clicked
     */
    public void addClickableAndDisableableListElement(final String s, final ObservableBooleanValue isDisabled, final Consumer<MouseEvent> mouseEventConsumer) {
        final MenuElement element = new MenuElement(s, mouseEventConsumer);
        addHideListener(element);
        element.setDisableable(isDisabled);
        list.getChildren().add(element.getItem());
    }

    /**
     * Adds a custom {@link MenuElement} to {@link #content}
     * @param element The constructed menu element to be added
     */
    public void addMenuElement(final MenuElement element) {
        addHideListener(element);
        list.getChildren().add(element.getItem());
    }

    /**
     * Adds a spacing element to the {@link DropDownMenu}
     */
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

    /**
     * Adds a color picker to the {@link DropDownMenu}s to {@link #content} with the title color
     * @param hasColor The current color
     * @param consumer A consumer for the color property
     */
    public void addColorPicker(final HasColor hasColor, final BiConsumer<Color, Color.Intensity> consumer) {
        addListElement("Color");

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

        addCustomElement(flowPane);
    }


    /***
     * Makes the elements of the {@link DropDownMenu} listen to the {@link #isHidden} property,
     * such that they can react when the menu is hidden/shown
     * @param element The {@link MenuElement} that should {@link #hide()} when {@link #isHidden} is changed
     */
    private void addHideListener(MenuElement element) {
        final Consumer<Boolean> updateHide = (hidden) -> { if(hidden) element.hide(); };

        this.isHidden.addListener((obs, oldValue, newValue) -> updateHide.accept(newValue));
        updateHide.accept(this.isHidden.get());
    }

    /**
     * An overridden method that lets us set {@link #isHidden} when the {@link DropDownMenu} is hidden
     * It is called manually when we want to close {@link DropDownMenu},
     * but also automatically for example when changing window and the {@link DropDownMenu} is open
     */
    @Override
    public void hide(){
        if (this.isShowing()) {
            this.isHidden.set(true);
            super.hide();
        }
    }

    /**
     * Adds a custom element to the {@link DropDownMenu}.
     * @param element The element to add to the {@link DropDownMenu}.
     */
    public void addCustomElement(final Node element) {
        list.getChildren().add(element);
    }

    /**
     * Adds a sub menu to the {@link DropDownMenu}.
     * @param s The text of the label use for showing the sub menu
     * @param subMenu The sub menu
     * @param offset The vertical offset of the sub menu's content
     */
    public void addSubMenu(final String s, final DropDownMenu subMenu, final int offset) {
        final Label label = new Label(s);
        final ObjectProperty<Boolean> isHoveringLabel = new SimpleObjectProperty<>(false);

        label.setStyle("-fx-padding: 8 16 8 16;");
        label.getStyleClass().add("body2");
        label.setMinWidth(width);

        final StackPane subMenuContent = subMenu.content;
        subMenuContent.setStyle("-fx-padding: 0 0 0 2;");
        subMenuContent.setMinWidth(subMenuContent.getMinWidth() + 1);
        subMenuContent.setMaxWidth(subMenuContent.getMinWidth() + 1);
        subMenuContent.setTranslateX(width);
        subMenuContent.setTranslateY(offset);
        subMenuContent.setOpacity(0);

        final Runnable show = () -> {
            // Set the background to a light grey
            label.setBackground(new Background(new BackgroundFill(
                    Color.GREY.getColor(Color.Intensity.I200),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));

            subMenuContent.setOpacity(1);
        };

        final Runnable hide = () -> {
            // Set the background to be transparent
            label.setBackground(new Background(new BackgroundFill(
                    TRANSPARENT,
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));

            subMenuContent.setOpacity(0);
        };

        // Set properties in order to prevent closing when hovering sub menu
        subMenuContent.setOnMouseEntered(event -> {
            isHoveringSubMenu.set(true);
            show.run();
        });

        subMenuContent.setOnMouseExited(event -> {
            isHoveringSubMenu.set(false);

            if (!isHoveringLabel.get()) {
                hide.run();
            }
        });

        this.content.getChildren().add(subMenuContent);

        final JFXRippler rippler = new JFXRippler(label);
        rippler.setRipplerFill(Color.GREY_BLUE.getColor(Color.Intensity.I300));

        rippler.setOnMouseEntered(event -> {
            isHoveringLabel.set(true);

            show.run();
        });

        rippler.setOnMouseExited(event -> {
            isHoveringLabel.set(false);

            if (!isHoveringSubMenu.get()) {
                hide.run();
            }
        });

        final FontIcon icon = new FontIcon();
        icon.setIconLiteral("gmi-chevron-right");
        icon.setFill(Color.GREY.getColor(Color.Intensity.I600));
        icon.setIconSize(20);

        final StackPane iconContainer = new StackPane(icon);
        iconContainer.setMaxWidth(20);
        iconContainer.setMaxHeight(20);
        iconContainer.setStyle("-fx-padding: 8;");
        iconContainer.setMouseTransparent(true);

        rippler.getChildren().add(iconContainer);
        StackPane.setAlignment(iconContainer, Pos.CENTER_RIGHT);

        list.getChildren().add(rippler);
    }

    public interface HasColor {
        ObjectProperty<Color> colorProperty();

        ObjectProperty<Color.Intensity> colorIntensityProperty();
    }
}