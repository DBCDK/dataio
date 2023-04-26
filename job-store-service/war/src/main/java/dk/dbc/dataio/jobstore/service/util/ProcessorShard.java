package dk.dbc.dataio.jobstore.service.util;

public class ProcessorShard {

    public enum Type {ACCTEST, BUSINESS}

    private static String shard;
    private String queue;

    public ProcessorShard(Type type) {
        shard = type.name().toLowerCase();
    }

    @Override
    public String toString() {
        return shard;
    }
}
