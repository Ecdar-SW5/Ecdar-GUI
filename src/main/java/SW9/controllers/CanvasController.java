package SW9.controllers;

import SW9.abstractions.Component;
import SW9.abstractions.Declarations;
import SW9.abstractions.HighLevelModelObject;
import SW9.presentations.CanvasPresentation;
import SW9.presentations.ComponentPresentation;
import SW9.presentations.DeclarationPresentation;
import SW9.utility.helpers.SelectHelper;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.util.Pair;

import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static SW9.presentations.Grid.GRID_SIZE;

public class CanvasController implements Initializable {
    public final static double DECLARATION_X_MARGIN = GRID_SIZE * 5.5;

    public Pane root;

    private final static ObjectProperty<HighLevelModelObject> activeVerificationObject = new SimpleObjectProperty<>(null);
    private final static HashMap<Component, Pair<Double, Double>> componentTranslateMap = new HashMap<>();

    private static DoubleProperty width, height;
    private static BooleanProperty insetShouldShow;
    private ComponentPresentation activeComponentPresentation;

    public static DoubleProperty getWidthProperty() {
        return width;
    }

    public static DoubleProperty getHeightProperty() {
        return height;
    }

    public static BooleanProperty getInsetShouldShow() {
        return insetShouldShow;
    }

    public static HighLevelModelObject getActiveVerificationObject() {
        return activeVerificationObject.get();
    }

    /**
     * Sets the given HighLevelModelObject as the one to be active / to be shown on the screen
     * @param object the given HighLevelModelObject
     */
    public static void setActiveVerificationObject(final HighLevelModelObject object) {
        CanvasController.activeVerificationObject.set(object);
        Platform.runLater(CanvasController::leaveTextAreas);
    }

    public static ObjectProperty<HighLevelModelObject> activeComponentProperty() {
        return activeVerificationObject;
    }

    public static void leaveTextAreas() {
        leaveTextAreas.run();
    }

    public static EventHandler<KeyEvent> getLeaveTextAreaKeyHandler() {
        return getLeaveTextAreaKeyHandler(keyEvent -> {});
    }

    public static EventHandler<KeyEvent> getLeaveTextAreaKeyHandler(final Consumer<KeyEvent> afterEnter) {
        return (keyEvent) -> {
            leaveOnEnterPressed.accept(keyEvent);
            afterEnter.accept(keyEvent);
        };
    }

    private static Consumer<KeyEvent> leaveOnEnterPressed;
    private static Runnable leaveTextAreas;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        width = new SimpleDoubleProperty();
        height = new SimpleDoubleProperty();
        insetShouldShow = new SimpleBooleanProperty();
        insetShouldShow.set(true);

        root.widthProperty().addListener((observable, oldValue, newValue) -> width.setValue(newValue));
        root.heightProperty().addListener((observable, oldValue, newValue) -> height.setValue(newValue));

        CanvasPresentation.mouseTracker.registerOnMousePressedEventHandler(event -> {
            // Deselect all elements
            SelectHelper.clearSelectedElements();
        });

        activeVerificationObject.addListener((obs, oldVeriObj, newVeriObj) ->
                onActiveVerificationObjectChanged(oldVeriObj, newVeriObj));

        leaveTextAreas = () -> root.requestFocus();

        leaveOnEnterPressed = (keyEvent) -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER) || keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                leaveTextAreas();
            }
        };

    }

    /**
     * Updates component translate map with old verification object.
     * Removes old verification object from view and shows new one.
     * @param oldVeriObj old verification object
     * @param newVeriObj new verification object
     */
    private void onActiveVerificationObjectChanged(final HighLevelModelObject oldVeriObj, final HighLevelModelObject newVeriObj) {
        // If old object is a component, add to map
        if (oldVeriObj != null && oldVeriObj instanceof Component) {
            componentTranslateMap.put((Component) oldVeriObj, new Pair<>(root.getTranslateX(), root.getTranslateY()));
        }

        if (newVeriObj == null) return; // We should not add the new component since it is null (clear the view)

        // Remove verification object from view
        root.getChildren().removeIf(node -> node instanceof ComponentPresentation || node instanceof DeclarationPresentation);

        if (newVeriObj instanceof Component) {
            if (componentTranslateMap.containsKey(newVeriObj)) {
                final Pair<Double, Double> restoreCoordinates = componentTranslateMap.get(newVeriObj);
                root.setTranslateX(restoreCoordinates.getKey());
                root.setTranslateY(restoreCoordinates.getValue());
            } else {
                root.setTranslateX(GRID_SIZE * 3);
                root.setTranslateY(GRID_SIZE * 8);
            }

            activeComponentPresentation = new ComponentPresentation((Component) newVeriObj);
            root.getChildren().add(activeComponentPresentation);
        } else if (newVeriObj instanceof Declarations) {
            root.setTranslateX(0);
            root.setTranslateY(DECLARATION_X_MARGIN);

            activeComponentPresentation = null;
            root.getChildren().add(new DeclarationPresentation((Declarations) newVeriObj));
        }

        root.requestFocus();
    }

    /**
     * Updates if height of views should have offsets at the bottom.
     * Whether views should have an offset is based on the configuration of the error view.
     * @param shouldHave true iff views should have an offset
     */
    public static void updateOffset(final Boolean shouldHave) {
        insetShouldShow.set(shouldHave);
    }

    /**
     * Gets the active component presentation.
     * This is null, if the declarations presentation is shown instead.
     * @return the active component presentation
     */
    ComponentPresentation getActiveComponentPresentation() {
        return activeComponentPresentation;
    }
}
