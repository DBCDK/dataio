package dk.dbc.dataio.querylanguage;

/**
 * Class representing an ORDER BY expression consisting of an
 * identifier on the form RESOURCE:FIELD followed by an {@link Order} sort order,
 * and an optional case conversion {@link SortCase}
 */
public class Ordering {
    public enum Order {ASC, DESC}

    public enum SortCase {LOWER, UPPER}

    private String identifier;
    private Order order;
    private SortCase sortCase;

    public String getIdentifier() {
        return identifier;
    }

    public Ordering withIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public Order getOrder() {
        return order;
    }

    public Ordering withOrder(Order order) {
        this.order = order;
        return this;
    }

    public SortCase getSortCase() {
        return sortCase;
    }

    public Ordering withSortCase(SortCase sortCase) {
        this.sortCase = sortCase;
        return this;
    }
}
