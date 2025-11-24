package dk.dbc.dataio.harvester.types;

import java.util.List;

public interface WorkerKey {
    String getConsumerId();

    default List<String> workerKeys() {
        return priorities().stream().map(p -> getConsumerId() + ":" + p.name().toLowerCase()).toList();
    }

    default List<Priority> priorities() {
        return List.of(Priority.MEDIUM);
    }

    enum Priority {
        LOW,
        MEDIUM,
        HIGH
    }
}
