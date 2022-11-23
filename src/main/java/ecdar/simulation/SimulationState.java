package ecdar.simulation;

import EcdarProtoBuf.ObjectProtos;
import EcdarProtoBuf.ObjectProtos.State;
import ecdar.Ecdar;
import javafx.util.Pair;

import java.util.ArrayList;

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
        if (decisionPoint.getEdgesList().isEmpty()) {
            Ecdar.showToast("No available transitions.");
        }
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
