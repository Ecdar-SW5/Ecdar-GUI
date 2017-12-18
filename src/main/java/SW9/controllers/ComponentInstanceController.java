package SW9.controllers;

import SW9.Ecdar;
import SW9.abstractions.*;
import SW9.backend.UPPAALDriver;
import SW9.presentations.DropDownMenu;
import SW9.presentations.MenuElement;
import SW9.utility.UndoRedoStack;
import SW9.utility.keyboard.Keybind;
import SW9.utility.keyboard.KeyboardTracker;
import com.jfoenix.controls.JFXPopup;
import com.jfoenix.controls.JFXTextField;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for a component instance.
 */
public class ComponentInstanceController implements Initializable {
    public BorderPane frame;
    public Line line1;
    public Rectangle background;
    public Label originalComponentLabel;
    public JFXTextField identifier;
    public StackPane root;
    public HBox toolbar;

    public BooleanProperty hasEdge = new SimpleBooleanProperty(false);

    private ComponentInstance instance;
    private ObjectProperty<SystemModel> system = new SimpleObjectProperty<>();
    private DropDownMenu dropDownMenu;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        system.addListener(((observable, oldValue, newValue) -> {
            initializeDropDownMenu(newValue);
        }));
    }

    public void initializeDropDownMenu(final SystemModel system) {
        dropDownMenu = new DropDownMenu(root, frame, 230, true);

        dropDownMenu.addMenuElement(new MenuElement("Draw Edge")
                .setClickable(() -> {
                    system.addEdge(new EcdarSystemEdge(instance));
                    dropDownMenu.close();
                })
                .setDisableable(hasEdge));

        dropDownMenu.addSpacerElement();

        dropDownMenu.addClickableListElement("Delete", event -> {
            // TODO
            dropDownMenu.close();
        });
    }

    @FXML
    private void onMousePressed(final MouseEvent event) {
        event.consume();

        if (event.getButton().equals(MouseButton.SECONDARY)) {
            dropDownMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 20, 20);
        }
    }

    public ComponentInstance getInstance() {
        return instance;
    }

    public void setInstance(final ComponentInstance instance) {
        this.instance = instance;
    }

    public void setSystem(final SystemModel system) {
        this.system.set(system);
    }

    public SystemModel getSystem() {
        return system.get();
    }
}
