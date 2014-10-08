package dk.dbc.dataio.jobstore.types.monitoring;

/**
 * Sequence analyser JMX monitoring MXBean interface
 */
public interface SequenceAnalyserMonitorMXBean {
    SequenceAnalyserMonitorSample getSample();
    void setSample(SequenceAnalyserMonitorSample sample);
}
