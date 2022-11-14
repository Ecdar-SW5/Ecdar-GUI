package ecdar.simulation;

import EcdarProtoBuf.ObjectProtos;
import EcdarProtoBuf.ObjectProtos.Federation;
import EcdarProtoBuf.ObjectProtos.State;
import EcdarProtoBuf.QueryProtos.SimulationInfo;
import ecdar.abstractions.Location;
import javafx.util.Pair;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class SimulationState {
    // key = component name, value = id
    private final ArrayList<Pair<String, String>> locations;
    private final ArrayList<Pair<String, String>> edges;
    private final State state;

    public SimulationState(ObjectProtos.DecisionPoint decisionPoint) {
        locations = new ArrayList<>();
        for (ObjectProtos.Location location : decisionPoint.getSource().getLocationTuple().getLocationsList()) {
            locations.add(new Pair<>(location.getSpecificComponent().getComponentName(), location.getId()));
        }

        edges = new ArrayList<>();
        for (ObjectProtos.Edge edge : decisionPoint.getEdgesList()) {
            edges.add(new Pair<>(edge.getSpecificComponent().getComponentName(), edge.getId()));
        }
        state = decisionPoint.getSource();
    }

    public ArrayList<Pair<String, String>> getLocations() {
        return locations;
    }
    public ArrayList<Pair<String, String>> getEdges() {
        return edges;
    }

    public State getState() {
        return state;
    }
}
