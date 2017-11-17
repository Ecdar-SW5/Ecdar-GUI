package SW9.backend;

import SW9.Ecdar;
import SW9.abstractions.Component;
import SW9.abstractions.*;
import com.google.common.base.Strings;
import com.uppaal.model.core2.Document;
import com.uppaal.model.core2.Property;
import com.uppaal.model.core2.PrototypeDocument;
import com.uppaal.model.core2.Template;

import java.awt.*;
import java.util.*;
import java.util.List;

class EcdarDocument {
    private static final String DECLARATION_PROPERTY_TAG = "declaration"; // Global and local declarations
    private static final String NAME_PROPERTY_TAG = "name";
    private static final String INVARIANT_PROPERTY_TAG = "invariant";
    private static final String GUARD_PROPERTY_TAG = "guard";
    private static final String SYNC_PROPERTY_TAG = "synchronisation";
    private static final String UPDATE_PROPERTY_TAG = "assignment";
    private static final String SYSTEM_DCL_TAG = "system";

    private final Document xmlDocument = new Document(new PrototypeDocument());

    // Map to convert Ecdar locations to UPPAAL locations
    private final Map<Location, com.uppaal.model.core2.Location> ecdarToXmlLocations = new HashMap<>();

    // Map to convert back from UPPAAL to Ecdar items
    private final Map<com.uppaal.model.core2.Location, Location> xmlToEcdarLocations = new HashMap<>();

    // Map to convert back from UPPAAL edges to Ecdar edges
    private final Map<com.uppaal.model.core2.Edge, Edge> xmlToEcdarEdges = new HashMap<>();

    Location universalLocation;
    Location inconsistentLocation;

    EcdarDocument() throws BackendException {
        generateXmlDocument();
    }

    /**
     * Generate a xml document based on the models.
     * @throws BackendException if an error occurs during generation
     */
    private void generateXmlDocument() throws BackendException {
        // Create a template for each model
        for (final Component component : Ecdar.getProject().getComponents()) {
            generateAndAddTemplate(component);
        }

        // Set global declarations
        xmlDocument.setProperty(DECLARATION_PROPERTY_TAG, Ecdar.getProject().getGlobalDeclarations().getDeclarationsText());

        // Set the system declaration
        xmlDocument.setProperty(SYSTEM_DCL_TAG, Ecdar.getProject().getSystemDeclarations().getDeclarationsText());

    }

    /**
     * Generate XML name of a location.
     * @param location the location to be used
     * @return the XML name
     */
    private static String generateName(final Location location) {
        String result = "L";

        // Add the identifier for the location
        result += location.getId();

        // Return the result
        return result;
    }

    /**
     * Generates a template for a component and adds it to the xml document.
     * @param component the component to use
     * @throws BackendException if an error occurs during generation
     */
    private void generateAndAddTemplate(final Component component) throws BackendException {
        // Create empty template and insert it into the uppaal document
        final Template template = xmlDocument.createTemplate();

        template.setProperty(NAME_PROPERTY_TAG, component.getName());
        template.setProperty(DECLARATION_PROPERTY_TAG, component.getDeclarationsText());

        xmlDocument.insert(template, null);

        // Add an universal location
        universalLocation = new Location("U" + component.getName());
        addUniversalLocation(component, template, universalLocation);


        // Add an inconsistent location
        inconsistentLocation = new Location("I" + component.getName());
        addInconsistentLocation(component, template, inconsistentLocation);

        // Add all locations from the model to our conversion map and to the template
        for (final Location ecdarLocation : component.getLocations()) {
            // Add the location to the template
            final com.uppaal.model.core2.Location xmlLocation;

            if(ecdarLocation.getType() != Location.Type.UNIVERSAL && ecdarLocation.getType() != Location.Type.INCONSISTENT){
                xmlLocation = addLocation(template, ecdarLocation);
                // Populate the map
                addLocationsToMaps(ecdarLocation, xmlLocation);
            }
        }

        for (final Edge ecdarEdge : component.getEdges()) {
            // Draw edges that are purely location to location edges
            if (ecdarEdge.getSourceLocation() != null && ecdarEdge.getSourceLocation().getType() != Location.Type.UNIVERSAL && ecdarEdge.getTargetLocation() != null) {
                xmlToEcdarEdges.put(addEdge(template, ecdarEdge), ecdarEdge);
            }
        }

    }

    private void addInconsistentLocation(Component component, Template template, Location inconsistentLocation) {
        inconsistentLocation.setUrgency(Location.Urgency.URGENT);
        final com.uppaal.model.core2.Location xmlInconsistentLocation = addLocation(template, inconsistentLocation);
        addLocationsToMaps(inconsistentLocation, xmlInconsistentLocation);
    }

    private void addUniversalLocation(Component component, Template template, Location universalLocation) throws BackendException {
        final com.uppaal.model.core2.Location xmlUniversalLocation = addLocation(template, universalLocation);
        addLocationsToMaps(universalLocation, xmlUniversalLocation);
        for (String input : component.getInputStrings()) {
            Edge edge = new Edge(universalLocation, universalLocation, input, EdgeStatus.INPUT);
            xmlToEcdarEdges.put(addEdge(template, edge), edge);

        }
        for (String output : component.getOutputStrings()) {
            Edge edge = new Edge(universalLocation, universalLocation, output, EdgeStatus.INPUT);
            xmlToEcdarEdges.put(addEdge(template, edge), edge);
        }
    }

    /**
     * Adds a pair of locations to maps.
     * @param ecdarLocation ecdar location to add
     * @param xmlLocation xml location to add
     */
    private void addLocationsToMaps(final Location ecdarLocation, final com.uppaal.model.core2.Location xmlLocation) {
        ecdarToXmlLocations.put(ecdarLocation, xmlLocation);
        xmlToEcdarLocations.put(xmlLocation, ecdarLocation);
    }

    /**
     * Generates an xml location from an Ecdar location and adds it to a template.
     * @param template the template
     * @param ecdarLocation the Ecdar location
     * @return the xml location added
     */
    private static com.uppaal.model.core2.Location addLocation(final Template template, final Location ecdarLocation) {
        final int x = (int) ecdarLocation.xProperty().get();
        final int y = (int) ecdarLocation.yProperty().get();
        final Color color = ecdarLocation.getColor().toAwtColor(ecdarLocation.getColorIntensity());

        // Create new UPPAAL location and insert it into the template
        final com.uppaal.model.core2.Location xmlLocation = template.createLocation();
        template.insert(xmlLocation, null);

        // Set name of the location
        xmlLocation.setProperty(NAME_PROPERTY_TAG, ecdarLocation.getId());

        // Set the invariant if any
        if (ecdarLocation.getInvariant() != null) {
            xmlLocation.setProperty(INVARIANT_PROPERTY_TAG, ecdarLocation.getInvariant());
        }

        // Add urgent property if location is urgent
        if (ecdarLocation.getUrgency().equals(Location.Urgency.URGENT)) {
            xmlLocation.setProperty("urgent", true);
        }

        // Add initial property if location is initial
        if (ecdarLocation.getType().equals(Location.Type.INITIAL)) {
            xmlLocation.setProperty("init", true);
        }

        // Update the placement of the name label
        final Property p = xmlLocation.getProperty(NAME_PROPERTY_TAG);
        p.setProperty("x", x);
        p.setProperty("y", y - 30);

        // Set the color of the location
        xmlLocation.setProperty("color", color);

        // Set the x and y properties
        xmlLocation.setProperty("x", x);
        xmlLocation.setProperty("y", y);

        return xmlLocation;
    }

    /**
     * Generates an xml edge from an Ecdar edge and adds it to a template.
     * @param template the template
     * @param ecdarEdge the Ecdar edge
     * @return the XML edge generated
     * @throws BackendException iff an edge has no source or target location
     */
    private com.uppaal.model.core2.Edge addEdge(final Template template, final Edge ecdarEdge) throws BackendException {
        // Create new UPPAAL edge and insert it into the template
        final com.uppaal.model.core2.Edge xmlEdge = template.createEdge();
        template.insert(xmlEdge, null);

        final com.uppaal.model.core2.Location sourceULocation;
        final com.uppaal.model.core2.Location targetULocation;

        // Find the source locations
        if (ecdarEdge.getSourceLocation() != null) {
            sourceULocation = ecdarToXmlLocations.get(ecdarEdge.getSourceLocation());
        } else {
            throw new BackendException("An edge has no source location");
        }

        // Find the target locations
        if (ecdarEdge.getTargetLocation() != null) {
            if(ecdarEdge.getTargetLocation().getType() == Location.Type.UNIVERSAL){
                targetULocation = ecdarToXmlLocations.get(universalLocation);
            } else if(ecdarEdge.getTargetLocation().getType() == Location.Type.INCONSISTENT){
                targetULocation = ecdarToXmlLocations.get(inconsistentLocation);
            } else {
                targetULocation = ecdarToXmlLocations.get(ecdarEdge.getTargetLocation());
            }
        } else {
            throw new BackendException("An edge has no target location");
        }

        // Add the to the edge
        xmlEdge.setSource(sourceULocation);
        xmlEdge.setTarget(targetULocation);

        annotateEdge(xmlEdge, ecdarEdge);

        return xmlEdge;
    }

    /**
     * Annotates an XML edge based on the corresponding Ecdar edge.
     * @param xmlEdge the XML edge
     * @param ecdarEdge the corresponding Ecdar edge
     */
    private static void annotateEdge(final com.uppaal.model.core2.Edge xmlEdge, final Edge ecdarEdge) {
        final List<Nail> reversedNails = new ArrayList<>();
        ecdarEdge.getNails().forEach(nail -> reversedNails.add(0, nail));

        // Annotate with controllable if output edge
        if (ecdarEdge.getStatus() == EdgeStatus.OUTPUT){
            xmlEdge.setProperty("controllable", false);
        }

        for (final Nail ecdarNail : reversedNails) {
            // Create a Uppaal nail
            final com.uppaal.model.core2.Nail xmlNail = xmlEdge.createNail();
            xmlEdge.insert(xmlNail, null);

            final int x = (int) ecdarNail.getX();
            final int y = ((int) ecdarNail.getY());

            // If the nail is a property nail and the edge have this property set, add it to the view
            if (!Strings.isNullOrEmpty(ecdarEdge.getSelect()) && ecdarNail.getPropertyType().equals(Edge.PropertyType.SELECTION)) {
                xmlEdge.setProperty("select", ecdarEdge.getSelect());
                final Property p = xmlEdge.getProperty("select");
                p.setProperty("x", x + ((int) ecdarNail.getPropertyX()));
                p.setProperty("y", y + ((int) ecdarNail.getPropertyY()));
            }

            if (!Strings.isNullOrEmpty(ecdarEdge.getGuard()) && ecdarNail.getPropertyType().equals(Edge.PropertyType.GUARD)) {
                xmlEdge.setProperty(GUARD_PROPERTY_TAG, ecdarEdge.getGuard());
                final Property p = xmlEdge.getProperty(GUARD_PROPERTY_TAG);
                p.setProperty("x", x + ((int) ecdarNail.getPropertyX()));
                p.setProperty("y", y + ((int) ecdarNail.getPropertyY()));
            }

            if (!Strings.isNullOrEmpty(ecdarEdge.getSync()) && ecdarNail.getPropertyType().equals(Edge.PropertyType.SYNCHRONIZATION)) {
                xmlEdge.setProperty(SYNC_PROPERTY_TAG, ecdarEdge.getSyncWithSymbol());
                final Property p = xmlEdge.getProperty(SYNC_PROPERTY_TAG);
                p.setProperty("x", x + ((int) ecdarNail.getPropertyX()));
                p.setProperty("y", y + ((int) ecdarNail.getPropertyY()));
            }

            if (!Strings.isNullOrEmpty(ecdarEdge.getUpdate()) && ecdarNail.getPropertyType().equals(Edge.PropertyType.UPDATE)) {
                xmlEdge.setProperty(UPDATE_PROPERTY_TAG, ecdarEdge.getUpdate());
                final Property p = xmlEdge.getProperty(UPDATE_PROPERTY_TAG);
                p.setProperty("x", x + ((int) ecdarNail.getPropertyX()));
                p.setProperty("y", y + ((int) ecdarNail.getPropertyY()));
            }

            // Add the position of the nail
            xmlNail.setProperty("x", x);
            xmlNail.setProperty("y", y);

        }
    }

    /**
     * Gets the XML document generated.
     * @return the XML document
     */
    Document toXmlDocument() {
        return xmlDocument;
    }

    /**
     * Gets a corresponding Ecdar location from an XML location.
     * @param xmlLocation the XML location
     * @return the Ecdar location
     */
    Location getLocation(final com.uppaal.model.core2.Location xmlLocation) {
        return xmlToEcdarLocations.get(xmlLocation);
    }

    /**
     * Gets the corresponding Ecdar edge from an XML edge.
     * @param xmlEdge the XML edge
     * @return the Ecdar edge
     */
    Edge getEdge(final com.uppaal.model.core2.Edge xmlEdge) {
        return xmlToEcdarEdges.get(xmlEdge);
    }

}
