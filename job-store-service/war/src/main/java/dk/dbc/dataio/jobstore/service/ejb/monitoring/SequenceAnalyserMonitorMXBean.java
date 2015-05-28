package dk.dbc.dataio.jobstore.service.ejb.monitoring;

/**
 * Sequence analyser JMX monitoring MXBean interface
 */
public interface SequenceAnalyserMonitorMXBean {
    SequenceAnalyserMonitorSample getSample();
    void setSample(SequenceAnalyserMonitorSample sample);
}
