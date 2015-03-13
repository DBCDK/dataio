package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.jobstore.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.types.ChunkResult;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.commons.types.JobCompletionState;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.jobstore.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.SupplementaryProcessData;
import dk.dbc.dataio.jobstore.types.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowComponentContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.FlowContentBuilder;
import dk.dbc.dataio.commons.utils.test.model.JavaScriptBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.JobStore;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static dk.dbc.dataio.jobstore.util.Base64Util.base64encode;

@LocalBean
@Singleton
@Startup
public class JobStoreBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobStoreBean.class);

    private JobStoreImpl jobStore;

    @PostConstruct
    public void setupJobStore() {
        try {
            LOGGER.info("Setting up mocked job-store");
            jobStore = new JobStoreImpl();
            jobStore.setupJob42();
        } catch (Exception e) {
            throw new EJBException(e);
        }
    }

    public JobStore getJobStore() {
        return jobStore;
    }

    public Job createAndScheduleJob(long jobId, JobSpecification jobSpec, FlowBinder flowBinder, Flow flow, Sink sink, InputStream jobInputStream) throws JobStoreException {
        return jobStore.createJob(jobSpec, flowBinder, flow, sink, jobInputStream, null);
    }

    private static class JobStoreImpl implements JobStore {
        public static final String RECORD_42_1 = "one";
        public static final String RECORD_42_2 = "two";

        private static final String MODULES_INFO_MODULE_RESOURCE = "/ModulesInfo.js";
        private static final String USE_MODULE_RESOURCE = "/Use.js";
        private static final String JAVA_SCRIPT_INVOCATION_METHOD = "toUpper";

        private final AtomicLong jobIdSequence = new AtomicLong(42);

        // <jobId, <chunkId, Chunk>>
        private final Map<Long, Map<Long, Chunk>> inMemoryJobStoreChunks = new HashMap<>();

        // <jobId, JobInfo>
        private final Map<Long, JobInfo> inMemoryJobsCreated = new HashMap<>();

        // <jobId, Sink>
        private final Map<Long, Sink> inMemorySinks = new HashMap<>();

        // <jobId, Flow>
        private final Map<Long, Flow> inMemoryFlows = new HashMap<>();

        // <jobId, SupplementaryProcessData>
        private final Map<Long, SupplementaryProcessData> inMemorySupplementaryProcessData = new HashMap<>();

        @Override
        public Job createJob(JobSpecification jobSpec, FlowBinder flowBinder, Flow flow, Sink sink, InputStream jobInputStream, SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator) throws JobStoreException {
            final long jobId = jobIdSequence.incrementAndGet();
            final JobInfo jobInfo = new JobInfo(jobId, jobSpec, System.currentTimeMillis());
            jobInfo.setJobRecordCount(42);
            inMemoryJobsCreated.put(jobId, jobInfo);
            return new Job(jobInfo, new JobState(), flow);
        }

        @Override
        public void updateJobInfo(Job job, JobInfo jobInfo) throws JobStoreException { }

        @Override
        public List<JobInfo> getAllJobInfos() throws JobStoreException {
            return new ArrayList<>(inMemoryJobsCreated.values());
        }

        @Override
        public long getNumberOfChunksInJob(long jobId) throws JobStoreException {
            return 0;
        }

        @Override
        public Chunk getChunk(long jobId, long chunkId) throws JobStoreException {
            final Map<Long, Chunk> chunks = inMemoryJobStoreChunks.get(jobId);
            if (chunks == null) {
                LOGGER.warn("Job {} not found", jobId);
                return null;
            } else {
                return chunks.get(chunkId);
            }
        }

        @Override
        public void addProcessorResult(ChunkResult processorResult) throws JobStoreException { }

        @Override
        public ChunkResult getProcessorResult(long jobId, long chunkId) throws JobStoreException {
            return null;
        }

        @Override
        public void addSinkResult(SinkChunkResult sinkResult) throws JobStoreException { }

        @Override
        public SinkChunkResult getSinkResult(long jobId, long chunkId) throws JobStoreException {
            return null;
        }

        @Override
        public Sink getSink(long jobId) throws JobStoreException {
            return inMemorySinks.get(jobId);
        }

        @Override
        public JobState getJobState(long jobId) throws JobStoreException {
            return null;
        }

        @Override
        public JobCompletionState getJobCompletionState(long jobId) throws JobStoreException {
            return null;
        }

        @Override
        public Flow getFlow(long jobId) throws JobStoreException {
            return inMemoryFlows.get(jobId);
        }
        
        @Override
        public SupplementaryProcessData getSupplementaryProcessData(long jobId) throws JobStoreException {
            LOGGER.info("HELLEDUSSEDA: requesting jobId: " + jobId);
            return inMemorySupplementaryProcessData.get(jobId);
        }

        private void setupJob42() throws Exception {
            inMemoryJobStoreChunks.put(42L, buildChunksForJob42());
            inMemorySinks.put(42L, new SinkBuilder().build());
            inMemoryFlows.put(42L, buildToUpperFlow());
            inMemorySupplementaryProcessData.put(42L, new SupplementaryProcessData(424242L, "something"));
        }

        private Map<Long, Chunk> buildChunksForJob42() throws Exception {
            final Map<Long, Chunk> chunks  = new HashMap<>(2);
            final Flow toUpperFlow = buildToUpperFlow();
            chunks.put(1L, buildChunk(42L, 1L, toUpperFlow,
                    new ChunkItemBuilder().setData(base64encode(RECORD_42_1)).build()));
            chunks.put(2L, buildChunk(42L, 2L, toUpperFlow,
                    new ChunkItemBuilder().setData(base64encode(RECORD_42_2)).build()));
            return chunks;
        }

        private Chunk buildChunk(long jobId, long chunkId, Flow flow, ChunkItem... items) {
            return new ChunkBuilder()
                    .setJobId(jobId)
                    .setChunkId(chunkId)
                    .setFlow(flow)
                    .setItems(Arrays.asList(items))
                    .build();
        }

        private Flow buildToUpperFlow() throws Exception {
            return new FlowBuilder()
                    .setContent(buildToUpperFlowContent())
                    .build();
        }

        private FlowContent buildToUpperFlowContent() throws Exception {
            return new FlowContentBuilder()
                    .setComponents(Arrays.asList(buildToUpperFlowComponent()))
                    .build();
        }

        private FlowComponent buildToUpperFlowComponent() throws Exception {
            return new FlowComponentBuilder()
                    .setContent(buildToUpperFlowComponentContent())
                    .build();
        }

        private FlowComponentContent buildToUpperFlowComponentContent() throws Exception {
            return new FlowComponentContentBuilder()
                    .setInvocationMethod(JAVA_SCRIPT_INVOCATION_METHOD)
                    .setJavascripts(Arrays.asList(
                            buildJavaScript(buildJavaScriptToUpperFunction(), ""),
                            buildJavaScript(resourceToString(MODULES_INFO_MODULE_RESOURCE), "ModulesInfo"),
                            buildJavaScript(resourceToString(USE_MODULE_RESOURCE), "Use")))
                    .build();
        }

        private JavaScript buildJavaScript(String javaScript, String moduleName) {
            return new JavaScriptBuilder()
                    .setModuleName(moduleName)
                    .setJavascript(base64encode(javaScript))
                    .build();
        }

        private String buildJavaScriptToUpperFunction() {
            return ""
                    + "function " + JAVA_SCRIPT_INVOCATION_METHOD + "(str) {\n"
                    + "    return str.toUpperCase();\n"
                    + "}\n";
        }

        private String resourceToString(String resourceName) throws Exception {
            // Fiddling with the class loader like this is probably a violation of
            // the EJB specification, but since we are in mocking/testing territory,
            // I guess we can live with it.
            final java.net.URL url = this.getClass().getResource(resourceName);
            final java.nio.file.Path resPath = java.nio.file.Paths.get(url.toURI());
            return new String(java.nio.file.Files.readAllBytes(resPath), StandardCharsets.UTF_8);
        }
    }
}
