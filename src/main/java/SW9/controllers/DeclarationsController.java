package SW9.controllers;

import SW9.abstractions.Declarations;
import SW9.presentations.ComponentPresentation;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.Initializable;
import javafx.scene.layout.StackPane;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for overall declarations.
 */
public class DeclarationsController implements Initializable {
    public StyleClassedTextArea textArea;
    public StackPane root;

    private double offSet, canvasHeight;
    private final ObjectProperty<Declarations> declarations;

    public DeclarationsController() {
        declarations = new SimpleObjectProperty<>(null);
    }

    public void setDeclarations(final Declarations declarations) {
        this.declarations.set(declarations);
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        initializeWidthAndHeight();
        initializeText();
    }

    /**
     * Initializes width and height of the text editor field, such that it fills up the whole canvas
     */
    private void initializeWidthAndHeight() {
        // Fetch width and height of canvas and update
        root.setMinWidth(CanvasController.getWidthProperty().doubleValue());
        canvasHeight = CanvasController.getHeightProperty().doubleValue();
        updateOffset(CanvasController.getInsetShouldShow().get());
        updateHeight();

        CanvasController.getWidthProperty().addListener((observable, oldValue, newValue) -> {
            root.setMinWidth(newValue.doubleValue());
            root.setMaxWidth(newValue.doubleValue());
        });
        CanvasController.getHeightProperty().addListener((observable, oldValue, newValue) -> {
            canvasHeight = newValue.doubleValue();
            updateHeight();
        });
        CanvasController.getInsetShouldShow().addListener((observable, oldValue, newValue) -> {
            updateOffset(newValue);
            updateHeight();
        });
    }

    /**
     * Sets up the linenumbers and binds the text in the text area to the declaration object
     */
    private void initializeText() {
        textArea.setParagraphGraphicFactory(LineNumberFactory.get(textArea));

        // Bind the declarations of the abstraction the the view
        declarations.addListener((observable, oldValue, newValue) -> {
            textArea.replaceText(0, textArea.getLength(), declarations.get().getDeclarationsText());

            // Initially style the declarations
            updateHighlighting();
        });

        textArea.textProperty().addListener((observable, oldDeclaration, newDeclaration) ->
                declarations.get().setDeclarationsText(newDeclaration));
    }

    /**
     * Updates highlighting of the text in the text area.
     */
    public void updateHighlighting() {
        textArea.setStyleSpans(0, ComponentPresentation.computeHighlighting(declarations.get().getDeclarationsText()));
    }

    /**
     * Updates the offset of the height of the view based on whether an inset should show.
     * @param insetShouldShow true iff an inset should show
     */
    private void updateOffset(final boolean insetShouldShow) {
        if (insetShouldShow) {
            offSet = 20;
        } else {
            offSet = 0;
        }
    }

    /**
     * Updates the height of the view.
     */
    private void updateHeight() {
        final double value = canvasHeight - CanvasController.DECLARATION_X_MARGIN - offSet;

        root.setMinHeight(value);
        root.setMaxHeight(value);
    }
}
