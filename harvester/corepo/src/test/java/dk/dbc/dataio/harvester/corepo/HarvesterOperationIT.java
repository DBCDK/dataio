package dk.dbc.dataio.harvester.corepo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.status;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class HarvesterOperationIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterOperationIT.class);
    private static final Logger HLOGGER = LoggerFactory.getLogger("corepo-harvester");
    private static final Network network = Network.newNetwork();
    private static DBCPostgreSQLContainer db = startCorepoDB(network);
    private static final WireMockServer wireMockServer = makeWireMockServer(db);
    private static GenericContainer<?> harvester = startHarvester(network);


    @Test @Ignore("A somewhat slow test for special occasions")
    public void testHarvesterRequests() throws InterruptedException {
        List<String> requestBodies = List.of();
        while (requestBodies.isEmpty()) {
            requestBodies = getUpdateRequestBodies(wireMockServer.getAllServeEvents());
            Thread.sleep(1000);
        }
        Assert.assertTrue("All requests must have the type attribute", requestBodies.stream().allMatch(s -> s.matches(".*\"type\":\"dk\\.dbc\\.dataio\\.harvester\\.types\\.HarvestRecordsRequest\".*")));
        Assert.assertTrue("All requests must have records", requestBodies.stream().allMatch(s -> s.matches(".*\"records\":\\[\\{\".*")));
    }

    private List<String> getUpdateRequestBodies(List<ServeEvent> allServeEvents) {
        return allServeEvents.stream()
                .map(ServeEvent::getRequest)
                .filter(r -> r.getUrl().startsWith("/dataio/harvester/"))
                .map(LoggedRequest::getBody)
                .map(String::new)
                .collect(Collectors.toList());
    }


    private static WireMockServer makeWireMockServer(DBCPostgreSQLContainer db) {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.stubFor(post(urlMatching("/dataio/harvester/.*")).willReturn(
                status(201).withHeader("location", "barbados")));
        server.stubFor(get(urlMatching("/dataio/flow-store-service/harvester-configs/types/.+/enabled")).willReturn(
                status(200)
                        .withHeader("content-type", "application/json")
                        .withHeader("encoding", "utf-8")
                        .withBody(readConfig("harvester-config-enabled.json", db))));
        server.stubFor(any(urlMatching("/dataio/flow-store-service/harvester-configs/\\d+")).willReturn(
                status(200)
                        .withHeader("content-type", "application/json")
                        .withHeader("encoding", "utf-8")
                        .withBody(readConfig("harvester-config-update.json", db))));
        server.stubFor(post(urlMatching("/1.0/api/libraryrules")).willReturn(
                status(200)
                        .withHeader("content-type", "application/json")
                        .withHeader("encoding", "utf-8")
                        .withBody(readConfig("vipcore-libs.json", db))));

//        server.addMockServiceRequestListener((request, response) -> {
//            System.out.println(request.getMethod() + ":" + request.getUrl());
//        });
        server.start();
        configureFor("localhost", server.port());
        Testcontainers.exposeHostPorts(server.port());
        return server;
    }

    private static DBCPostgreSQLContainer startCorepoDB(Network network) {
        DBCPostgreSQLContainer container = new DBCPostgreSQLContainer()
                .withNetwork(network)
                .withReuse(false)
                .withNetworkAliases("corepo-db")
                .withInitScript("sql/corepo.sql");
        container.start();
        container.exposeHostPort();
        return container;
    }

    private static String readConfig(String name, DBCPostgreSQLContainer db) {
        try {
            Instant instant = Instant.now().minusSeconds(300);
            LOGGER.info("Setting start time to: {}", instant);
            return Files.readString(Path.of("src/test/resources/" + name))
                    .replaceAll("\\$\\{DB_RESOURCE}", "postgresql://" + db.getPayaraDockerJdbcUrl()).replaceAll("\\$\\{START_TIME}", Long.toString(instant.toEpochMilli()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static GenericContainer<?> startHarvester(Network network) {
        final GenericContainer<?> container = new GenericContainer<>("docker-metascrum.artifacts.dbccloud.dk/dataio-harvester-corepo:devel")
                .withNetwork(network)
                .withNetworkAliases("dataio-harvester-corepo")
                .withLogConsumer(new Slf4jLogConsumer(HLOGGER))
                .withEnv("LOG_FORMAT", "text")
                .withEnv("LOG_LEVEL", "info")
                .withEnv("JAVA_MAX_HEAP_SIZE", "2G")
                .withEnv("FLOWSTORE_URL", "http://host.testcontainers.internal:" + wireMockServer.port() + "/dataio/flow-store-service")
                .withEnv("VIPCORE_ENDPOINT", "http://host.testcontainers.internal:" + wireMockServer.port())
                .withEnv("RAWREPO_HARVESTER_URL", "http://host.testcontainers.internal:" + wireMockServer.port() + "/dataio/harvester/rr")
                .withEnv("TZ", "Europe/Copenhagen")
                .withExposedPorts(4848, 8080)
                .waitingFor(Wait.forHttp("/dataio/harvester/corepo/status")
                        .withReadTimeout(Duration.of(10, ChronoUnit.SECONDS)))
                .withStartupTimeout(Duration.ofMinutes(10));
        container.start();
        return container;
    }
}
