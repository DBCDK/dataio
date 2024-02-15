package dk.dbc.dataio.jobstore.service.dependencytracking.hzqueries;

import com.hazelcast.query.Predicate;
import dk.dbc.dataio.commons.utils.lang.Hashcode;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTracking;
import dk.dbc.dataio.jobstore.service.dependencytracking.TrackingKey;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChunksToWaitFor implements Predicate<TrackingKey, DependencyTracking> {
    private final int sinkId;
    private final int submitterNumber;
    private final Set<Integer> hashes;

    public ChunksToWaitFor(int sinkId, int submitterNumber, Integer[] hashes, String barrierMatchKey) {
        this.sinkId = sinkId;
        this.submitterNumber = submitterNumber;
        Objects.requireNonNull(hashes);
        if (barrierMatchKey == null) this.hashes = Set.of(hashes);
        else this.hashes = Stream.concat(Arrays.stream(hashes), Stream.of(Hashcode.of(barrierMatchKey)))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean apply(Map.Entry<TrackingKey, DependencyTracking> entry) {
        DependencyTracking value = entry.getValue();
        return value.getSinkId() == sinkId &&
                value.getSubmitterNumber() == submitterNumber &&
                Arrays.stream(value.getHashes()).anyMatch(hashes::contains);
    }
}
