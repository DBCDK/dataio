package dk.dbc.dataio.jobstore.service.dependencytracking;

import com.hazelcast.cluster.Cluster;
import com.hazelcast.cluster.Member;
import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HazelcastTest {
    private final Member master = mock(Member.class);
    private final IExecutorService executorService = mock(IExecutorService.class);

    @BeforeEach
    void setUpCluster() {
        final Member other = mock(Member.class);
        final Cluster cluster = mock(Cluster.class);
        when(cluster.getMembers()).thenReturn(new LinkedHashSet<>(List.of(master, other)));
        final HazelcastInstance hazelcastInstance = mock(HazelcastInstance.class);
        when(hazelcastInstance.getCluster()).thenReturn(cluster);
        when(hazelcastInstance.getExecutorService("default")).thenReturn(executorService);
        Hazelcast.testInstance(hazelcastInstance);
    }

    @AfterEach
    void resetHazelcastInstance() {
        Hazelcast.testInstance(null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void executeOnMaster_submitsToMasterWithFailureCallback() {
        final Callable<Void> task = () -> null;

        Hazelcast.executeOnMaster(task);

        final ArgumentCaptor<ExecutionCallback<Void>> callback = ArgumentCaptor.forClass(ExecutionCallback.class);
        verify(executorService).submitToMember(eq(task), eq(master), callback.capture());
        assertThat(callback.getValue()).isInstanceOf(Hazelcast.LoggingExecutionCallback.class);
    }

    @Test
    void loggingExecutionCallback_onFailure_doesNotThrow() {
        final Hazelcast.LoggingExecutionCallback<Void> callback =
                new Hazelcast.LoggingExecutionCallback<>(() -> null, master);

        assertThatCode(() -> callback.onFailure(new IllegalStateException("lookup failed")))
                .doesNotThrowAnyException();
        assertThatCode(() -> callback.onResponse(null)).doesNotThrowAnyException();
    }
}
