package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.sink.SinkException;
import dk.dbc.dataio.sink.es.ESTaskPackageUtil.TaskStatus;
import dk.dbc.dataio.sink.es.entity.EsInFlight;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This is a simple white-box test of the EsScheduledCleanupBean.cleanup() method.
 */
public class EsScheduledCleanupBeanTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EsSinkConfigurationBeanTest.class);

    private EsScheduledCleanupBean cleanupBean;
    private EsInFlightBean esInFlightAdmin;
    private EsConnectorBean esConnector;
    private EsThrottlerBean esThrottler;
    private ConnectionFactory jobProcessorQueueConnectionFactory;
    private JMSContext jmsContext;
    private JMSProducer jmsProducer;
    private TextMessage textMessage;
    private EsInFlight esInFlight41_1;
    private EsInFlight esInFlight42_1;
    private EsInFlight esInFlight42_2;
    private EsInFlight esInFlight43_1;
    private TaskStatus taskStatus_122;
    private TaskStatus taskStatus_123;
    private TaskStatus taskStatus_124;
    private TaskStatus taskStatus_125;
    private List<EsInFlight> emptyEsInFlightList = Collections.EMPTY_LIST;
    private List<TaskStatus> emptyTaskStatusList = Collections.EMPTY_LIST;

    @Before
    public void setup() {

        cleanupBean = new EsScheduledCleanupBean();

        esInFlightAdmin = mock(EsInFlightBean.class);
        cleanupBean.esInFlightAdmin = esInFlightAdmin;

        esConnector = mock(EsConnectorBean.class);
        cleanupBean.esConnector = esConnector;

        esThrottler = mock(EsThrottlerBean.class);
        cleanupBean.esThrottler = esThrottler;

        jobProcessorQueueConnectionFactory = mock(ConnectionFactory.class);
        cleanupBean.jobHandlerQueueConnectionFactory = jobProcessorQueueConnectionFactory;

        jmsContext = mock(JMSContext.class);
        jmsProducer = mock(JMSProducer.class);
        textMessage = mock(TextMessage.class);

        esInFlight41_1 = new EsInFlight();
        esInFlight41_1.setJobId(41L);
        esInFlight41_1.setChunkId(1L);
        esInFlight41_1.setTargetReference(122);
        esInFlight41_1.setRecordSlots(3);
        esInFlight41_1.setResourceName("resource 1");

        esInFlight42_1 = new EsInFlight();
        esInFlight42_1.setJobId(42L);
        esInFlight42_1.setChunkId(1L);
        esInFlight42_1.setTargetReference(123);
        esInFlight42_1.setRecordSlots(10);
        esInFlight42_1.setResourceName("resource 1");

        esInFlight42_2 = new EsInFlight();
        esInFlight42_2.setJobId(42L);
        esInFlight42_2.setChunkId(2L);
        esInFlight42_2.setTargetReference(124);
        esInFlight42_2.setRecordSlots(5);
        esInFlight42_2.setResourceName("resource 1");

        esInFlight43_1 = new EsInFlight();
        esInFlight43_1.setJobId(43L);
        esInFlight43_1.setChunkId(1L);
        esInFlight43_1.setTargetReference(125);
        esInFlight43_1.setRecordSlots(10);
        esInFlight43_1.setResourceName("resource 1");

        taskStatus_122 = new TaskStatus(3, 122);
        taskStatus_123 = new TaskStatus(2, 123);
        taskStatus_124 = new TaskStatus(1, 124);
        taskStatus_125 = new TaskStatus(0, 125);
    }

    @Test
    public void startup_elementsInFlight_throtlerIsCountedDown() throws IllegalArgumentException, InterruptedException {
        when(esInFlightAdmin.listEsInFlight()).thenReturn(Arrays.asList(esInFlight41_1, esInFlight42_1, esInFlight42_2, esInFlight43_1));

        cleanupBean.startup();

        verify(esThrottler).acquireRecordSlots(esInFlight41_1.getRecordSlots() + esInFlight42_1.getRecordSlots() + esInFlight42_2.getRecordSlots() + esInFlight43_1.getRecordSlots());
    }

    @Test
    public void startup_noElementsInFlight_throtlerIsNotCountedDown() throws IllegalArgumentException, InterruptedException {
        when(esInFlightAdmin.listEsInFlight()).thenReturn(emptyEsInFlightList);

        cleanupBean.startup();

        verify(esThrottler).acquireRecordSlots(0);
    }

    @Test
    public void cleanup_emptyEsInFlight_nothingHappens() {
        when(esInFlightAdmin.listEsInFlight()).thenReturn(emptyEsInFlightList);

        cleanupBean.cleanup();
    }

    @Test
    public void cleanup_noFinsihedInFlight_nothingHappens() throws SinkException {
        when(esInFlightAdmin.listEsInFlight()).thenReturn(Arrays.asList(esInFlight42_2));
        when(esConnector.getCompletionStatusForESTaskpackages(any(List.class))).thenReturn(Arrays.asList(taskStatus_124));

        cleanupBean.cleanup();
    }

    @Test
    public void cleanup_AbortedCompletedActivePendingInFlight_AbortedCompletedIsCleanedUp() throws SinkException, JMSException {
        when(esInFlightAdmin.listEsInFlight()).thenReturn(Arrays.asList(esInFlight41_1, esInFlight42_1, esInFlight42_2, esInFlight43_1));
        when(esConnector.getCompletionStatusForESTaskpackages(any(List.class))).thenReturn(Arrays.asList(taskStatus_122, taskStatus_123, taskStatus_124, taskStatus_125));
        when(jobProcessorQueueConnectionFactory.createContext()).thenReturn(jmsContext);
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(textMessage);
        when(jmsContext.createProducer()).thenReturn(jmsProducer);

        cleanupBean.cleanup();

        verify(esInFlightAdmin).removeEsInFlight(eq(esInFlight42_1));
        verify(esConnector).deleteESTaskpackages(any(List.class));
        verify(esThrottler).releaseRecordSlots(esInFlight42_1.getRecordSlots() + esInFlight41_1.getRecordSlots());
        verify(textMessage, times(2)).setStringProperty(any(String.class), any(String.class));
        verify(jmsProducer, times(2)).send(any(Destination.class), any(TextMessage.class));
    }
}
