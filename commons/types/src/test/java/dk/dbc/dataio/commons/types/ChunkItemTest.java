package dk.dbc.dataio.commons.types;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ChunkItemTest {
    private static final String ID = "id";
    private static final String DATA = "data";
    private static final ChunkItem.Status STATUS = ChunkItem.Status.SUCCESS;

    @Test(expected = NullPointerException.class)
    public void constructor_idArgIsNull_throws() {
        new ChunkItem(null, DATA, STATUS);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_dataArgIsNull_throws() {
        new ChunkItem(ID, null, STATUS);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_statusArgIsNull_throws() {
        new ChunkItem(ID, DATA, null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        assertThat(new ChunkItem(ID, DATA, STATUS), is(notNullValue()));
    }

    public static ChunkItem newChunkItemInstance() {
        return new ChunkItem(ID, DATA, STATUS);
    }
}
