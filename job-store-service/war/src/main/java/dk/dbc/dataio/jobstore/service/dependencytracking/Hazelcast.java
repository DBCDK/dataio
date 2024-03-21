package dk.dbc.dataio.jobstore.service.dependencytracking;

import com.hazelcast.cluster.Member;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.LifecycleEvent;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class Hazelcast {
    private static final Logger LOGGER = LoggerFactory.getLogger(Hazelcast.class);
    private static HazelcastInstance INSTANCE;
    private static final AtomicBoolean STOPPING = new AtomicBoolean(false);

    private static HazelcastInstance startInstance() {
        String configFile = Optional.ofNullable(System.getenv("JOBSTORE_HZ_CONFIG"))
                .filter(s -> !s.isEmpty())
                .orElse("/opt/payara6/deployments/hz-data.xml");
        try(InputStream is = new FileInputStream(configFile)) {
            HazelcastInstance instance = com.hazelcast.core.Hazelcast.newHazelcastInstance(makeConfig(is));
            instance.getLifecycleService().addLifecycleListener(lifecycleEvent -> {
                LOGGER.warn("Hazelcast is shutting down");
                STOPPING.set(lifecycleEvent.getState() == LifecycleEvent.LifecycleState.SHUTTING_DOWN);
            });
            return instance;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start hazelcast data instance", e);
        }
    }

    public static Config makeConfig(InputStream configStream) {
        Config config = new XmlConfigBuilder(configStream).build();
        config.setInstanceName(System.getenv("HOSTNAME"));
        config.setClassLoader(Hazelcast.class.getClassLoader());
        return config;
    }

    private static HazelcastInstance getInstance() {
        if(INSTANCE != null) return INSTANCE;
        synchronized (Hazelcast.class) {
            if(INSTANCE == null) INSTANCE = startInstance();
        }
        return INSTANCE;
    }

    public static void testInstance(HazelcastInstance instance) {
        INSTANCE = instance;
    }

    public static boolean isMaster() {
        return INSTANCE.getCluster().getMembers().stream().findFirst().map(Member::localMember).orElse(false);
    }

    public static boolean isReady() {
        return !STOPPING.get() && Hazelcast.getInstance().getLifecycleService().isRunning() && Hazelcast.getInstance().getPartitionService().isClusterSafe();
    }

    public static void shutdownNode() {
        STOPPING.set(true);
        INSTANCE.shutdown();
    }

    public enum Objects {
        DEPENDENCY_TRACKING(() -> getInstance().getMap("dependencies")),
        ABORTED_JOBS(() -> getInstance().getSet("aborted.jobs")),
        SINK_STATUS(() -> getInstance().getMap("status.map"));

        private final Supplier<?> supplier;

        Objects(Supplier<?> supplier) {
            this.supplier = supplier;
        }

        @SuppressWarnings("unchecked")
        public <T> T get() {
            return (T)supplier.get();
        }
    }

    @Interceptor
    @ServiceUp
    public static class ShuttingDownInterceptor {
        @AroundInvoke
        public Object isStopping(InvocationContext ic) throws Exception {
            if(!STOPPING.get()) return ic.proceed();
            else throw new IllegalStateException("Server is shutting down");
        }
    }

    @InterceptorBinding
    @Retention(RUNTIME)
    @Target({METHOD, TYPE})
    public @interface ServiceUp {
    }

}
