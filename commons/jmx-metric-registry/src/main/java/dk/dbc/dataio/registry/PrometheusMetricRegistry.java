package dk.dbc.dataio.registry;

public class PrometheusMetricRegistry extends JMXMetricRegistry {
    private static final PrometheusMetricRegistry INSTANCE = new PrometheusMetricRegistry();
    private PrometheusMetricRegistry() {
        super(false);
    }

    public static PrometheusMetricRegistry create() {
        return INSTANCE;
    }
}
