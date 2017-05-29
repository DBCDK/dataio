package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.Constants;

import java.util.BitSet;

public class IncludeFilter {
    private BitSet bitSet;
    private int virtualChunkId;
    private int virtualItemCounter = 0;

    /**
     * @param bitSet a bit set with indices to include
     */
    public IncludeFilter(BitSet bitSet) {
        this.bitSet = bitSet;
    }

    /**
     * Wrapper around BitSet.get
     *
     * @param index index to check
     * @return true if index included in bit set
     */
    public boolean include(int index) {
        return bitSet.get(index);
    }

    public IncludeFilter withVirtualChunkId(int virtualChunkId) {
        this.virtualChunkId = virtualChunkId;
        return this;
    }

    public IncludeFilter withVirtualItemCounter(int virtualItemCounter) {
        this.virtualItemCounter = virtualItemCounter;
        return this;
    }

    /**
     * Logic to keep track of virtual position for job only containing failed items.
     *
     * @return virtual item index
     */
    public int incrementVirtualCounter() {
        if(virtualItemCounter == Constants.CHUNK_MAX_SIZE) {
            virtualChunkId++;
            virtualItemCounter = 0;
        }
        return virtualChunkId * Constants.CHUNK_MAX_SIZE + virtualItemCounter++;
    }
}
