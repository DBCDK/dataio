package dk.dbc.dataio.sink.periodicjobs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * This class is not thread-safe.
 */
public class GroupHeaderIncludePredicate implements Predicate<PeriodicJobsDataBlock> {
    // If thread safety is needed, use ConcurrentHashMap.newKeySet() instead
    private Set<ByteArrayWrapper> groupHeaders = new HashSet<>();

    /**
     * Returns true if a group header for a datablock exists, and only if
     * the same header has not already been seen for another datablock.
     * @param dataBlock datablock to test for group header inclusion
     * @return true ir false on whether or not to include group header
     */
    @Override
    public boolean test(PeriodicJobsDataBlock dataBlock) {
        final byte[] groupHeader = dataBlock.getGroupHeader();
        if (groupHeader != null) {
            return groupHeaders.add(new ByteArrayWrapper(groupHeader));
        }
        return false;
    }

    private static class ByteArrayWrapper {
        private final byte[] bytes;

        ByteArrayWrapper(byte[] bytes) {
            this.bytes = Arrays.copyOf(bytes, bytes.length);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ByteArrayWrapper that = (ByteArrayWrapper) o;

            return Arrays.equals(bytes, that.bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes);
        }
    }
}
