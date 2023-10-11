package dk.dbc.dataio.jse.artemis.common.service;

import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.jse.artemis.common.Config;
import dk.dbc.dataio.registry.PrometheusMetricRegistry;
import jakarta.ws.rs.client.ClientBuilder;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.glassfish.jersey.jackson.JacksonFeature;


public class ServiceHub implements AutoCloseable {
    public final MetricRegistry metricRegistry;
    public final HttpService httpService;
    public final HealthService healthService;
    public final MetricsService metricsService;
    public final ZombieWatch zombieWatch;
    public final JobStoreServiceConnector jobStoreServiceConnector;

    private ServiceHub(MetricRegistry metricRegistry, HttpService httpService, HealthService healthService, MetricsService metricsService, ZombieWatch zombieWatch, JobStoreServiceConnector jobStoreServiceConnector) {
        this.metricRegistry = metricRegistry;
        this.httpService = httpService;
        this.healthService = healthService;
        this.metricsService = metricsService;
        this.zombieWatch = zombieWatch;
        this.jobStoreServiceConnector = jobStoreServiceConnector;
    }

    @SuppressWarnings("unused")
    public static ServiceHub defaultHub() {
        return new ServiceHub.Builder().build();
    }

    @Override
    public void close() throws Exception {
        httpService.close();
    }

    @SuppressWarnings("unused")
    public static class Builder {
        private MetricRegistry metricRegistry = PrometheusMetricRegistry.create();
        private HttpService httpService = null;
        private HealthService healthService = null;
        private MetricsService metricsService = null;
        private ZombieWatch zombieWatch = null;
        private JobStoreServiceConnector jobStoreServiceConnector = Config.JOBSTORE_URL.asOptionalString().map(js -> new JobStoreServiceConnector(ClientBuilder.newClient().register(new JacksonFeature()), js)).orElse(null);

        public ServiceHub build() {
            if(httpService == null) httpService = new HttpService(Config.WEB_PORT.asInteger());
            if(healthService == null) healthService = new HealthService(httpService);
            if(metricsService == null) metricsService = new MetricsService(httpService, metricRegistry);
            if(zombieWatch == null) zombieWatch = new ZombieWatch(healthService);
            return new ServiceHub(metricRegistry, httpService, healthService, metricsService, zombieWatch, jobStoreServiceConnector);
        }

        public ServiceHub test() {
            return new ServiceHub(metricRegistry, httpService, healthService, metricsService, zombieWatch, jobStoreServiceConnector);
        }

        public Builder withMetricRegistry(MetricRegistry metricRegistry) {
            this.metricRegistry = metricRegistry;
            return this;
        }

        public Builder withHttpService(HttpService httpService) {
            this.httpService = httpService;
            return this;
        }

        public Builder withHealthService(HealthService healthService) {
            this.healthService = healthService;
            return this;
        }

        public Builder withMetricsService(MetricsService metricsService) {
            this.metricsService = metricsService;
            return this;
        }

        public Builder withZombieWatch(ZombieWatch zombieWatch) {
            this.zombieWatch = zombieWatch;
            return this;
        }

        public Builder withJobStoreServiceConnector(JobStoreServiceConnector jobStoreServiceConnector) {
            this.jobStoreServiceConnector = jobStoreServiceConnector;
            return this;
        }
    }
}
