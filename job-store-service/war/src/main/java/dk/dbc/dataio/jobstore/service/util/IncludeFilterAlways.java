package dk.dbc.dataio.jobstore.service.util;

import java.util.BitSet;

public class IncludeFilterAlways extends IncludeFilter {
    public IncludeFilterAlways() {
        super(new BitSet());
    }

    /**
     * Always returns true.
     * This is to avoid having a null bit set and checking its value for every key
     *
     * @param index key to check
     * @return true
     */
    @Override
    public boolean include(int index) {
        return true;
    }
}
