package dk.dbc.dataio.jse.artemis.common.service;

import dk.dbc.dataio.jse.artemis.common.Health;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class HealthService {
    public final static Duration MAXIMUM_TIME_TO_PROCESS = Duration.ofMinutes(3);
    private final Set<Health> signals = new HashSet<>();
    public HealthService(HttpService httpService) {
        httpService.addServlet(this::doGet, "/health/*");
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Optional<Health> flag = signals.stream().findFirst();
        resp.setStatus(flag.map(Health::getStatusCode).orElse(200));
        resp.getWriter().println(flag.map(Health::getMessage).orElse("I'm not quite dead yet"));
    }

    public boolean isUp() {
        return signals.isEmpty();
    }

    public void signal(Health flag) {
        signals.add(flag);
    }

}
