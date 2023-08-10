package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;
import java.util.Objects;

/**
 * SinkContent DTO class.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SinkContent implements Serializable {
    private static final long serialVersionUID = -3413557101203220951L;

    public enum SinkType {
        DPF,
        DUMMY,
        ES,
        HIVE,
        HOLDINGS_ITEMS,
        IMS,
        MARCCONV,
        OPENUPDATE,
        PERIODIC_JOBS,
        TICKLE,
        VIP,
        WORLDCAT,
        DMAT,
        DIFF_SINK
    }

    public enum SequenceAnalysisOption {ALL, ID_ONLY}

    private static final SinkType NULL_TYPE = null;
    private static final SinkConfig NULL_CONFIG = null;

    private final String name;
    private final String queue;
    private final String description;
    private final SinkType sinkType;
    private final SinkConfig sinkConfig;
    private final SequenceAnalysisOption sequenceAnalysisOption;
    private final int timeout;

    /**
     * Class constructor
     *
     * @param name                   sink name
     * @param queue                  sink queue
     * @param description            sink description
     * @param sinkType               sink type
     * @param sinkConfig             sink config
     * @param sequenceAnalysisOption options for sequence analysis
     * @param timeout                chunk timeout (in hours) for the sink
     * @throws NullPointerException     if given null-valued name or resource argument
     * @throws IllegalArgumentException if given empty-valued name or resource argument
     */
    @JsonCreator
    public SinkContent(@JsonProperty("name") String name,
                       @JsonProperty(value = "queue") String queue,
                       @JsonProperty("description") String description,
                       @JsonProperty("sinkType") SinkType sinkType,
                       @JsonProperty("sinkConfig") SinkConfig sinkConfig,
                       @JsonProperty("sequenceAnalysisOption") SequenceAnalysisOption sequenceAnalysisOption,
                       @JsonProperty("timeout") int timeout) {

        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
        this.queue = queue;
        this.description = description;
        this.sinkType = sinkType;
        this.sinkConfig = sinkConfig;
        this.sequenceAnalysisOption = InvariantUtil.checkNotNullOrThrow(sequenceAnalysisOption, "sequenceAnalysisOption");
        this.timeout = Math.max(1, timeout);
    }

    public SinkContent(String name, String queue, String description, SinkType sinkType, SinkConfig sinkConfig, SequenceAnalysisOption sequenceAnalysisOption) {
        this(name, queue, description, sinkType, sinkConfig, sequenceAnalysisOption, 1);
    }

    public SinkContent(String name, String queue, String description, SequenceAnalysisOption sequenceAnalysisOption) {
        this(name, queue, description, NULL_TYPE, NULL_CONFIG, sequenceAnalysisOption, 1);
    }

//    public SinkContent(String name, String description, SequenceAnalysisOption sequenceAnalysisOption) {
//        this(name, "jmsDataioSinks", description, NULL_TYPE, NULL_CONFIG, sequenceAnalysisOption);
//    }

    public String getName() {
        return name;
    }

    public String getQueue() {
        return queue;
    }

    public String getDescription() {
        return description;
    }

    public SinkType getSinkType() {
        return sinkType;
    }

    public SinkConfig getSinkConfig() {
        return sinkConfig;
    }

    public SequenceAnalysisOption getSequenceAnalysisOption() {
        return sequenceAnalysisOption;
    }

    public int getTimeout() {
        return timeout;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SinkContent that = (SinkContent) o;
        return name.equals(that.name)
                && queue.equals(that.queue)
                && description.equals(that.description)
                && sinkType == that.sinkType
                && Objects.equals(sinkConfig, that.sinkConfig)
                && sequenceAnalysisOption == that.sequenceAnalysisOption;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + queue.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + (sinkType != null ? sinkType.hashCode() : 0);
        result = 31 * result + (sinkConfig != null ? sinkConfig.hashCode() : 0);
        result = 31 * result + sequenceAnalysisOption.hashCode();
        return result;
    }
}
