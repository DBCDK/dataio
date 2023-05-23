package dk.dbc.dataio.sink.periodicjobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;

public abstract class ContainerTest extends IntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerTest.class);
    protected static final GenericContainer socks5Proxy;
    protected static final String PROXY_HOST;
    protected static final int PROXY_PORT;
    protected static final String PROXY_USER = "socksuser";
    protected static final String PROXY_PASSWORD = "sockspassword";

    static {
        Network network = Network.newNetwork();
        socks5Proxy = new GenericContainer("docker-metascrum.artifacts.dbccloud.dk/socks5proxy:latest")
                .withNetwork(network)
                .withNetworkAliases("proxy")
                .withExposedPorts(1080)
                .withEnv("USERNAME", PROXY_USER)
                .withEnv("PASSWORD", PROXY_PASSWORD)
                .waitingFor(Wait.forLogMessage("^.*v\\d+(\\.\\d+)+ running.*$", 5))
                .withStartupTimeout(Duration.ofMinutes(1));

        socks5Proxy.start();


        PROXY_HOST = socks5Proxy.getContainerIpAddress();
        PROXY_PORT = socks5Proxy.getMappedPort(1080);

        LOGGER.info(
                "Started suite.\n" +
                        "   PROXY: {} at port {} {}/{}",
                PROXY_HOST, PROXY_PORT, PROXY_USER, PROXY_PASSWORD);
    }

    protected String getLocalIPAddress() {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getProxyLog() {
        return socks5Proxy.getLogs();
    }
}
