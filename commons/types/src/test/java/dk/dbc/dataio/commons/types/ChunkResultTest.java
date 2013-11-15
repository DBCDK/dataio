package dk.dbc.dataio.commons.types;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.Test;

public class ChunkResultTest {

    private static final long JOBID = 31L;
    private static final long CHUNKID = 17L;
    private static final Charset ENCODING = Charset.forName("UTF-8");

    @Test(expected = NullPointerException.class)
    public void constructor_encodingArgIsNull_throws() {
        new ChunkResult(JOBID, CHUNKID, null, Collections.EMPTY_LIST);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_resultsArgIsNull_throws() {
        new ChunkResult(JOBID, CHUNKID, ENCODING, null);
    }

//    @Test(expected = IllegalArgumentException.class)
//    public
}
