package dk.dbc.dataio.jobprocessor2.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Optional;

public class HealthService {
    public final static Duration MAXIMUM_TIME_TO_PROCESS = Duration.ofMinutes(3);
    private final EnumSet<HealthFlag> signals = EnumSet.noneOf(HealthFlag.class);
    public HealthService(HttpService httpService) {
        httpService.addServlet(this::doGet, "/health/*");
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Optional<HealthFlag> flag = signals.stream().findFirst();
        resp.setStatus(flag.map(s -> s.statusCode).orElse(200));
        resp.getWriter().println(flag.map(f -> f.message).orElse("I'm not quite dead yet"));
    }

    public boolean isUp() {
        return signals.isEmpty();
    }

    public void signal(HealthFlag flag) {
        signals.add(flag);
    }

}
