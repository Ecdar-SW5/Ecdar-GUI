package ecdar.controllers;

import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.backend.SimulationHandler;
import ecdar.presentations.SimulatorOverviewPresentation;
import ecdar.simulation.SimulationState;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.Initializable;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SimulatorController implements Initializable {
    public StackPane root;
    public SimulatorOverviewPresentation overviewPresentation;
    public StackPane toolbar;

    private boolean firstTimeInSimulator;
    private final static DoubleProperty width = new SimpleDoubleProperty(),
            height = new SimpleDoubleProperty();
    private static ObjectProperty<SimulationState> selectedState = new SimpleObjectProperty<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        root.widthProperty().addListener((observable, oldValue, newValue) -> width.setValue(newValue));
        root.heightProperty().addListener((observable, oldValue, newValue) -> height.setValue(newValue));
        firstTimeInSimulator = true;
    }

    /**
     * Prepares the simulator to be shown.<br />
     * It also prepares the processes to be shown in the {@link SimulatorOverviewPresentation} by: <br />
     * - Building the system if it has been updated or have never been created.<br />
     * - Adding the components which are going to be used in the simulation to
     */
    public void willShow() {
        final SimulationHandler sm = Ecdar.getSimulationHandler();
        boolean shouldSimulationBeReset = true;



        // If the user left a trace, continue from that trace
        if (sm.traceLog.size() >= 2) {
            shouldSimulationBeReset = false;
        }

        // If the composition is not the same as previous simulation, reset the simulation
        if (!(overviewPresentation.getController().getComponentObservableList().hashCode() ==
                findComponentsInCurrentSimulation(SimulationInitializationDialogController.ListOfComponents).hashCode())) {
            shouldSimulationBeReset = true;
        }
        
        if (shouldSimulationBeReset || firstTimeInSimulator || sm.currentState.get() == null) {
            resetSimulation();
            sm.initialStep();
        }

        overviewPresentation.getController().addProcessesToGroup();

        // If the simulation continues, highligt the current state and available edges
        if (sm.currentState.get() != null && !shouldSimulationBeReset) {
            overviewPresentation.getController().highlightProcessState(sm.currentState.get());
            overviewPresentation.getController().highlightAvailableEdges(sm.currentState.get());
        }

    }

    /**
     * Resets the current simulation, and prepares for a new simulation by clearing the
     * {@link SimulatorOverviewController#processContainer} and adding the processes of the new simulation.
     */
    private void resetSimulation() {
        final SimulationHandler sm = Ecdar.getSimulationHandler();

        overviewPresentation.getController().clearOverview();
        overviewPresentation.getController().getComponentObservableList().clear();
        overviewPresentation.getController().getComponentObservableList().addAll(findComponentsInCurrentSimulation(SimulationInitializationDialogController.ListOfComponents));
        firstTimeInSimulator = false;
    }

    /**
     * Finds the components that are used in the current simulation by looking at the components found in
     * Ecdar.getProject.getComponents() and compares them to the components found in the queryComponents list
     *
     * @return all the components used in the current simulation
     */
    private List<Component> findComponentsInCurrentSimulation(List<String> queryComponents) {
        //Show components from the system
        List<Component> components = new ArrayList<>();
        
        components = Ecdar.getProject().getComponents();

        //Matches query components against with existing components and adds them to simulation
        List<Component> SelectedComponents = new ArrayList<>();
        for(Component comp:components) {
            for(String componentInQuery : queryComponents) {
                if((comp.getName().equals(componentInQuery))) {
                    SelectedComponents.add(comp);
                }
            }
        }
        return SelectedComponents;
    }

    /**
     * Resets the simulation and prepares the view for showing the new simulation to the user
     */
    public void resetCurrentSimulation() {
        overviewPresentation.getController().removeProcessesFromGroup();
        resetSimulation();
        Ecdar.getSimulationHandler().resetToInitialLocation();
        overviewPresentation.getController().addProcessesToGroup();
    }

    public void willHide() {
        overviewPresentation.getController().removeProcessesFromGroup();
        overviewPresentation.getController().getComponentObservableList().forEach(component -> {
            // Previously reset coordinates of component box
        });
        // overviewPresentation.getController().unhighlightProcesses();
    }

    public static DoubleProperty getWidthProperty() {
        return width;
    }

    public static DoubleProperty getHeightProperty() {
        return height;
    }

    public static void setSelectedState(SimulationState selectedState) {
        SimulatorController.selectedState.set(selectedState);
    }
}
