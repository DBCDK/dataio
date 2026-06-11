package dk.dbc.dataio.jobprocessorgjs.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import java.util.concurrent.atomic.AtomicReference;

@Liveness
@Default
@ApplicationScoped
public class ProcessorHealth implements HealthCheck {
    private final AtomicReference<String> unhealthyReason = new AtomicReference<>(null);

    @Override
    public HealthCheckResponse call() {
        String reason = unhealthyReason.get();
        if (reason != null) {
            return HealthCheckResponse.named("graaljs-processor").down()
                    .withData("reason", reason)
                    .build();
        }
        return HealthCheckResponse.named("graaljs-processor").up().build();
    }

    /** Signals a fatal condition that requires pod restart to recover. Once set, never cleared. */
    public void signalFatal(String reason) {
        unhealthyReason.compareAndSet(null, reason);
    }

    public void signalOutOfMemory() {
        signalFatal("Out of memory");
    }
}
