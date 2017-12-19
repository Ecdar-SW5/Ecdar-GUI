package SW9.abstractions;

import SW9.presentations.Grid;
import SW9.utility.colors.Color;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;

/**
 * Instance of a component.
 */
public class ComponentInstance implements SystemElement {
    public final static int WIDTH = Grid.GRID_SIZE * 22;
    public final static int HEIGHT = Grid.GRID_SIZE * 12;

    private final ObjectProperty<Component> component = new SimpleObjectProperty<>();
    private final ObjectProperty<Color> color = new SimpleObjectProperty<>();
    private final ObjectProperty<Color.Intensity> colorIntensity = new SimpleObjectProperty<>();
    private final Box box = new Box();
    private final StringProperty id = new SimpleStringProperty("");

    public ComponentInstance() {

    }

    public Component getComponent() {
        return component.get();
    }

    public void setComponent(final Component component) {
        this.component.set(component);
    }

    public Color getColor() {
        return color.get();
    }

    public void setColor(final Color color) {
        this.color.set(color);
    }

    public ObjectProperty<Color> getColorProperty() {
        return color;
    }

    public Color.Intensity getColorIntensity() {
        return colorIntensity.get();
    }

    public void setColorIntensity(final Color.Intensity intensity) {
        this.colorIntensity.set(intensity);
    }

    public ObjectProperty<Color.Intensity> getColorIntensityProperty() {
        return colorIntensity;
    }

    public Box getBox() {
        return box;
    }

    public String getId() {
        return id.get();
    }

    public StringProperty getIdProperty() {
        return id;
    }

    @Override
    public ObservableValue<? extends Number> getEdgeX() {
        return box.getXProperty().add(WIDTH / 2);
    }

    @Override
    public ObservableValue<? extends Number> getEdgeY() {
        return box.getYProperty().add(HEIGHT / 2);
    }
}
