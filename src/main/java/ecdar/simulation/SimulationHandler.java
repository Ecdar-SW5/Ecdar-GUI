package ecdar.simulation;

import EcdarProtoBuf.ComponentProtos;
import EcdarProtoBuf.ObjectProtos;
import EcdarProtoBuf.QueryProtos;
import EcdarProtoBuf.ObjectProtos.Location;
import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.backend.*;
import io.grpc.stub.StreamObserver;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.util.Pair;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import EcdarProtoBuf.QueryProtos.SimulationStepRequest;
import EcdarProtoBuf.QueryProtos.SimulationStepResponse;

/**
 * Handles state changes, updates of values / clocks, and keeps track of all the transitions that
 * have been taken throughout a simulation.
 */
public class SimulationHandler {
    public static final String QUERY_PREFIX = "Query: ";
    public String composition;
    public ObjectProperty<SimulationState> currentState = new SimpleObjectProperty<>();
    public ObjectProperty<SimulationState> initialState = new SimpleObjectProperty<>();
    public ObjectProperty<Edge> selectedEdge = new SimpleObjectProperty<>();
    private EcdarSystem system;
    private int numberOfSteps;

    /**
     * A string to keep track what is currently being simulated
     * For now the string is prefixed with {@link #QUERY_PREFIX} when doing a query simulation
     * and kept empty when doing system simulations
     */
    public String currentSimulation = "";

    private final ObservableMap<String, BigDecimal> simulationVariables = FXCollections.observableHashMap();
    private final ObservableMap<String, BigDecimal> simulationClocks = FXCollections.observableHashMap();
    /**
     * For some reason the successor.getTransitions() only sometimes returns some of the transitions
     * that are available, when running the initial step.
     * That is why we need to keep track of the initial transitions.
     */
    public ObservableList<SimulationState> traceLog = FXCollections.observableArrayList();
    private final BackendDriver backendDriver;
    private final ArrayList<BackendConnection> connections = new ArrayList<>();

    /**
     * Empty constructor that should be used if the system or project has not be initialized yet
     */
    public SimulationHandler(BackendDriver backendDriver) {
        this.backendDriver = backendDriver;
    }

    /**
     * Initializes the default system (non-query system)
     */
    public void initializeDefaultSystem() {
        currentSimulation = "";
    }

    /**
     * Initializes the values and properties in the {@link SimulationHandler}.
     * Can also be used as a reset of the simulation.
     * THIS METHOD DOES NOT RESET THE ENGINE,
     */
    private void initializeSimulation() {
        // Initialization
        this.numberOfSteps = 0;
        this.simulationVariables.clear();
        this.simulationClocks.clear();
        this.traceLog.clear();
        
        this.system = getSystem();
    }

    private SimulationStepResponse getStartTestData(){
        var response = SimulationStepResponse.newBuilder();
        var decisionPoint = ObjectProtos.DecisionPoint.newBuilder();
        var state = ObjectProtos.State.newBuilder();
        var specComp = ObjectProtos.SpecificComponent.newBuilder().setComponentName("Researcher").setComponentIndex(1);
        var locationTuple = ObjectProtos.LocationTuple.newBuilder()
            .addLocations(Location.newBuilder().setId(Ecdar.getProject().getComponents().stream().filter(p -> p.getName().equals("Researcher")).findFirst().get().getLocations().get(0).getId()).setSpecificComponent(specComp));
        state.setLocationTuple(locationTuple);
        decisionPoint.setSource(state);
        decisionPoint.addEdges(ObjectProtos.Edge.newBuilder().setId(Ecdar.getProject().getComponents().stream().filter(p -> p.getName().equals("Researcher")).findFirst().get().getEdges().get(3).getId()).setSpecificComponent(specComp));
        decisionPoint.addEdges(ObjectProtos.Edge.newBuilder().setId(Ecdar.getProject().getComponents().stream().filter(p -> p.getName().equals("Researcher")).findFirst().get().getEdges().get(7).getId()).setSpecificComponent(specComp));
        response.setNewDecisionPoint(decisionPoint);
        return response.build();
    }


    /**
     * Reloads the whole simulation sets the initial transitions, states, etc
     */
    public void initialStep() {
        initializeSimulation();

        GrpcRequest request = new GrpcRequest(backendConnection -> {
            StreamObserver<SimulationStepResponse> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(QueryProtos.SimulationStepResponse value) {
                    System.out.println(value);
                    currentState.set(new SimulationState(value.getNewDecisionPoint()));
                    Platform.runLater(() -> traceLog.add(currentState.get()));
                    selectedEdge.set(null);
                }
                
                @Override
                public void onError(Throwable t) {
                    System.out.println(t.getMessage());
                    Ecdar.showToast("Could not start simulation");
                    SimulationStepResponse value = getStartTestData();
                    currentState.set(new SimulationState(value.getNewDecisionPoint()));
                    Platform.runLater(() -> traceLog.add(currentState.get()));
                    selectedEdge.set(null);
                    
                    // Release backend connection
                    backendDriver.addBackendConnection(backendConnection);
                    connections.remove(backendConnection);
                }

                @Override
                public void onCompleted() {
                    // Release backend connection
                    backendDriver.addBackendConnection(backendConnection);
                    connections.remove(backendConnection);
                }
            };

            var comInfo = ComponentProtos.ComponentsInfo.newBuilder();
            for (Component c : Ecdar.getProject().getComponents()) {
                comInfo.addComponents(ComponentProtos.Component.newBuilder().setJson(c.serialize().toString()).build());
            }
            comInfo.setComponentsHash(comInfo.getComponentsList().hashCode());
            var simStartRequest = QueryProtos.SimulationStartRequest.newBuilder();
            var simInfo = QueryProtos.SimulationInfo.newBuilder()
                    .setComponentComposition(composition)
                    .setComponentsInfo(comInfo);
            simStartRequest.setSimulationInfo(simInfo);
            backendConnection.getStub().withDeadlineAfter(this.backendDriver.getResponseDeadline(), TimeUnit.MILLISECONDS)
                    .startSimulation(simStartRequest.build(), responseObserver);
        }, BackendHelper.getDefaultBackendInstance());
        
        backendDriver.addRequestToExecutionQueue(request);
        
        //Save the previous states, and get the new
        this.traceLog.add(currentState.get());
        numberOfSteps++;
    
        //Updates the transitions available
        updateAllValues();
        
    }
    
    /**
     * Resets the simulation to the initial location
     */
    public void resetToInitialLocation() {
        initialStep();
    }

    
    private SimulationStepResponse getNextTestData(){
        var response = SimulationStepResponse.newBuilder();
        var decisionPoint = ObjectProtos.DecisionPoint.newBuilder();
        var state = ObjectProtos.State.newBuilder();
        var specComp = ObjectProtos.SpecificComponent.newBuilder().setComponentName("Researcher").setComponentIndex(1);
        var locationTuple = ObjectProtos.LocationTuple.newBuilder()
            .addLocations(Location.newBuilder().setId(Ecdar.getProject().getComponents().stream().filter(p -> p.getName().equals("Researcher")).findFirst().get().getLocations().get(2).getId()).setSpecificComponent(specComp));
        state.setLocationTuple(locationTuple);
        decisionPoint.setSource(state);
        decisionPoint.addEdges(ObjectProtos.Edge.newBuilder().setId(Ecdar.getProject().getComponents().stream().filter(p -> p.getName().equals("Researcher")).findFirst().get().getEdges().get(4).getId()).setSpecificComponent(specComp));
        decisionPoint.addEdges(ObjectProtos.Edge.newBuilder().setId(Ecdar.getProject().getComponents().stream().filter(p -> p.getName().equals("Researcher")).findFirst().get().getEdges().get(10).getId()).setSpecificComponent(specComp));
        response.setNewDecisionPoint(decisionPoint);
        return response.build();
    }

    /**
     * Take a step in the simulation.
     */
    public void nextStep() {
        // check if the edge is a valid transition
        if (!currentState.get().getEdges().stream().anyMatch(e -> e.getValue() == selectedEdge.get().getId())) {
            Ecdar.showToast("Invalid transition");
            return;
        }

        GrpcRequest request = new GrpcRequest(backendConnection -> {
            StreamObserver<SimulationStepResponse> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(QueryProtos.SimulationStepResponse value) {
                    System.out.println(value);
                    value = getNextTestData();
                    currentState.set(new SimulationState(value.getNewDecisionPoint()));
                    Platform.runLater(() -> traceLog.add(currentState.get()));
                    selectedEdge.set(null);
                }
                
                @Override
                public void onError(Throwable t) {
                    currentState.set(null);
                    System.out.println(t.getMessage());
                    Ecdar.showToast("Could not take next step in simulation\nError: " + t.getMessage());
                    var value = getNextTestData();
                    currentState.set(new SimulationState(value.getNewDecisionPoint()));
                    Platform.runLater(() -> traceLog.add(currentState.get()));
                    selectedEdge.set(null);
                    
                    // Release backend connection
                    backendDriver.addBackendConnection(backendConnection);
                    connections.remove(backendConnection);
                }

                @Override
                public void onCompleted() {
                    // Release backend connection
                    backendDriver.addBackendConnection(backendConnection);
                    connections.remove(backendConnection);
                }
            };

            var comInfo = ComponentProtos.ComponentsInfo.newBuilder();
            for (Component c : Ecdar.getProject().getComponents()) {
                comInfo.addComponents(ComponentProtos.Component.newBuilder().setJson(c.serialize().toString()).build());
            }
            comInfo.setComponentsHash(comInfo.getComponentsList().hashCode());
            var simStepRequest = QueryProtos.SimulationStepRequest.newBuilder();
            var simInfo = QueryProtos.SimulationInfo.newBuilder()
                    .setComponentComposition(composition)
                    .setComponentsInfo(comInfo);
            simStepRequest.setSimulationInfo(simInfo);
            backendConnection.getStub().withDeadlineAfter(this.backendDriver.getResponseDeadline(), TimeUnit.MILLISECONDS)
                    .takeSimulationStep(simStepRequest.build(), responseObserver);
        }, BackendHelper.getDefaultBackendInstance());
        
        backendDriver.addRequestToExecutionQueue(request);


        // increments the number of steps taken during this simulation
        numberOfSteps++;


        updateAllValues();
    }


    /**
     * Updates all values and clocks that are used doing the current simulation.
     * It also stores the variables in the {@link SimulationHandler#simulationVariables}
     * and the clocks in {@link SimulationHandler#simulationClocks}.
     */
    private void updateAllValues() {
        setSimVarAndClocks();
    }

    /**
     * Sets the value of simulation variables and clocks, based on {@link SimulationHandler#currentConcreteState}
     */
    private void setSimVarAndClocks() {
        // The variables and clocks are all found in the getVariables array
        // the array is always of the following order: variables, clocks.
        // The noOfVars variable thus also functions as an offset for the clocks in the getVariables array
//        final int noOfClocks = engine.getSystem().getNoOfClocks();
//        final int noOfVars = engine.getSystem().getNoOfVariables();

//        for (int i = 0; i < noOfVars; i++){
//            simulationVariables.put(engine.getSystem().getVariableName(i),
//                    currentConcreteState.get().getVariables()[i].getValue(BigDecimal.ZERO));
//        }

        // As the clocks values starts after the variables values in currentConcreteState.get().getVariables()
        // Then i needs to start where the variables ends.
        // j is needed to map the correct name with the value
//        for (int i = noOfVars, j = 0; i < noOfClocks + noOfVars ; i++, j++) {
//            simulationClocks.put(engine.getSystem().getClockName(j),
//                    currentConcreteState.get().getVariables()[i].getValue(BigDecimal.ZERO));
//        }
    }



    /**
     * The number of total steps taken in the current simulation
     *
     * @return the number of steps
     */
    public int getNumberOfSteps() {
        return numberOfSteps;
    }

    /**
     * All the transitions taken in this simulation
     *
     * @return an {@link ObservableList} of all the transitions taken in this simulation so far
     */
    public ObservableList<SimulationState> getTraceLog() {
        return traceLog;
    }

    /**
     * All the available transitions in this state
     * @return 
     *
     * @return an {@link ObservableList} of all the currently available transitions in this state
     */
    public ArrayList<Pair<String, String>> getAvailableTransitions() {
        return currentState.get().getEdges();
    }

    /**
     * All the variables connected to the current simulation.
     * This does not return any clocks, if you need please use {@link SimulationHandler#getSimulationClocks()} instead
     *
     * @return a {@link Map} where the name (String) is the key, and a {@link BigDecimal} is the value
     */
    public ObservableMap<String, BigDecimal> getSimulationVariables() {
        return simulationVariables;
    }

    /**
     * All the clocks connected to the current simulation.
     *
     * @return a {@link Map} where the name (String) is the key, and a {@link BigDecimal} is the clock value
     * @see SimulationHandler#getSimulationVariables()
     */
    public ObservableMap<String, BigDecimal> getSimulationClocks() {
        return simulationClocks;
    }

    /**
     * The initial state of the current simulation
     *
     * @return the initial {@link SimulationState} of this simulation
     */
    public SimulationState getInitialState() {
        // ToDo: Implement
        return initialState.get();
    }

    public ObjectProperty<SimulationState> initialStateProperty() {
        return initialState;
    }


    public EcdarSystem getSystem() {
        return system;
    }

    public String getCurrentSimulation() {
        return currentSimulation;
    }

    public boolean isSimulationRunning() {
        return false; // ToDo: Implement
    }

    /**
     * Close all open backend connection and kill all locally running processes
     *
     * @throws IOException if any of the sockets do not respond
     */
    public void closeAllBackendConnections() throws IOException {
        for (BackendConnection con : connections) {
            con.close();
        }
    }

    public void selectTransitionFromLog(SimulationState state) {
        while (traceLog.get(traceLog.size() - 1) != state) {
            traceLog.remove(traceLog.size() - 1);
        }
        currentState.set(state);
    }
}