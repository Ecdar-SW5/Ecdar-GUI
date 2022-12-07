package ecdar.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.util.WaitForAsyncUtils;

import com.jfoenix.controls.JFXComboBox;

import ecdar.Ecdar;
import ecdar.abstractions.Query;
import ecdar.abstractions.QueryState;
import ecdar.backend.SimulationHandler;
import ecdar.presentations.SimulationInitializationDialogPresentation;
import javafx.application.Platform;
import javafx.scene.layout.BorderPane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testfx.api.FxAssert.verifyThat;

import java.util.Optional;

public class UiSwitcherTest extends TestFXBase {
    @Test
    public void UiSwitcherNoComponentsTest() {
        WaitForAsyncUtils.waitForFxEvents();
        clickOn("#switchGuiView");
        WaitForAsyncUtils.waitForFxEvents();
        JFXComboBox<String> simDialogComboBox = lookup("#simulationComboBox").query();
        WaitForAsyncUtils.waitForFxEvents();
        assertFalse(simDialogComboBox.isShowing());
    }
    
    @Test
    public void UiSwitcherTest() {
        WaitForAsyncUtils.waitForFxEvents();
        Platform.runLater(() -> Ecdar.getProject().getQueries().add(new Query("(Component1)", "null", QueryState.UNKNOWN)));
        WaitForAsyncUtils.waitForFxEvents();
        clickOn("#switchGuiView");
        WaitForAsyncUtils.waitForFxEvents();
        JFXComboBox<String> simDialogComboBox = lookup("#simulationComboBox").query();
        Platform.runLater(() -> simDialogComboBox.selectionModelProperty().get().select(0));
        WaitForAsyncUtils.waitForFxEvents();
        clickOn("#startButton");
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(Ecdar.getPresentation().getController().currentMode.get().name(), "Simulator");
    }
}
