/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.periodicjobs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * This class is not thread-safe.
 */
public class GroupHeaderProducer {
    // If thread safety is needed, use ConcurrentHashMap.newKeySet() instead
    private Set<ByteArrayWrapper> groupHeaders = new HashSet<>();

    /**
     * Returns group header for a datablock if it exists, and only if
     * the same header has not already been produced for another datablock.
     * @param dataBlock datablock to produce a group header for
     * @return optional group header
     */
    public Optional<byte[]> getGroupHeaderFor(PeriodicJobsDataBlock dataBlock) {
        final byte[] groupHeader = dataBlock.getGroupHeader();
        if (groupHeader != null) {
            final ByteArrayWrapper headerWrapper = new ByteArrayWrapper(groupHeader);
            if (groupHeaders.add(headerWrapper)) {
                return Optional.of(groupHeader);
            }
        }
        return Optional.empty();
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
