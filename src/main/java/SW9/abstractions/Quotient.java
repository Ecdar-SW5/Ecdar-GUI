package SW9.abstractions;

/**
 * Model of a Quotient operator, extends ComponentOperator
 */
public class Quotient extends ComponentOperator {
    public Quotient(final EcdarSystem system) {
        super(system);
        label.setValue("A//B");
    }

    @Override
    public String getJsonType() {
        return "quotient";
    }
}
