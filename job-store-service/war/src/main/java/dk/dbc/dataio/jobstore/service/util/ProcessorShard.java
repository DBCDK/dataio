package dk.dbc.dataio.jobstore.service.util;

public class ProcessorShard {

    public enum Type {ACCTEST, BUSINESS}

    private static String shard;

    public ProcessorShard(Type type) {
        shard = type.name().toLowerCase();
    }

    @Override
    public String toString() {
        return shard;
    }
}
