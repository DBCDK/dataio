package dk.dbc.dataio.jobprocessor2;

import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.jobprocessor2.service.ChunkProcessor;
import dk.dbc.dataio.jobprocessor2.service.HealthService;
import dk.dbc.dataio.jobprocessor2.service.HttpService;
import dk.dbc.dataio.jobprocessor2.service.MetricsService;
import dk.dbc.dataio.registry.PrometheusMetricRegistry;
import org.eclipse.microprofile.metrics.MetricRegistry;

import javax.ws.rs.client.ClientBuilder;


public class ServiceHub implements AutoCloseable {
    public final MetricRegistry metricRegistry;
    public final HttpService httpService;
    public final HealthService healthService;
    public final MetricsService metricsService;
    public final ChunkProcessor chunkProcessor;
    public final JobStoreServiceConnector jobStoreServiceConnector;

    private ServiceHub(MetricRegistry metricRegistry, HttpService httpService, HealthService healthService, MetricsService metricsService, ChunkProcessor chunkProcessor,
                       JobStoreServiceConnector jobStoreServiceConnector) {
        this.metricRegistry = metricRegistry;
        this.httpService = httpService;
        this.healthService = healthService;
        this.metricsService = metricsService;
        this.chunkProcessor = chunkProcessor;
        this.jobStoreServiceConnector = jobStoreServiceConnector;
    }

    @Override
    public void close() throws Exception {
        httpService.close();
    }

    public static class Builder {
        private MetricRegistry metricRegistry = PrometheusMetricRegistry.create();
        private HttpService httpService = new HttpService(Config.WEB_PORT.asInteger());
        private HealthService healthService = new HealthService(httpService);
        private MetricsService metricsService = new MetricsService(httpService, metricRegistry);
        private ChunkProcessor chunkProcessor = new ChunkProcessor(healthService);
        private JobStoreServiceConnector jobStoreServiceConnector = Config.JOB_STORE.asOptionalString().map(js -> new JobStoreServiceConnector(ClientBuilder.newClient(), js)).orElse(null);

        public ServiceHub build() {
            return new ServiceHub(metricRegistry, httpService, healthService, metricsService, chunkProcessor, jobStoreServiceConnector);
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

        public Builder withChunkProcessor(ChunkProcessor chunkProcessor) {
            this.chunkProcessor = chunkProcessor;
            return this;
        }

        public Builder withJobStoreServiceConnector(JobStoreServiceConnector jobStoreServiceConnector) {
            this.jobStoreServiceConnector = jobStoreServiceConnector;
            return this;
        }
    }
}
