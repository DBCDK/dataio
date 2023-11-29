package dk.dbc.dataio.sink.worldcat;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ChecksumTest {
    @Test
    public void checksum() {
        final WorldCatAttributes attributes = new WorldCatAttributes()
                .withHoldings(Arrays.asList(
                        new Holding().withSymbol("ABC").withAction(Holding.Action.INSERT),
                        new Holding().withSymbol("DEF").withAction(Holding.Action.INSERT)));

        final ChunkItemWithWorldCatAttributes chunkItem =
                (ChunkItemWithWorldCatAttributes) new ChunkItemWithWorldCatAttributes()
                        .withWorldCatAttributes(attributes)
                        .withData("record data");

        final String referenceChecksum = Checksum.of(chunkItem);

        assertThat("reference checksum", referenceChecksum, is(notNullValue()));
        assertThat("checksum is reproducible", Checksum.of(chunkItem), is(referenceChecksum));
        assertThat("holdings order does not matter", Checksum.of(chunkItem.withWorldCatAttributes(
                new WorldCatAttributes().withHoldings(Arrays.asList(
                        new Holding().withSymbol("DEF").withAction(Holding.Action.INSERT),
                        new Holding().withSymbol("ABC").withAction(Holding.Action.INSERT))))), is(referenceChecksum));
        assertThat("holding content affects checksum", Checksum.of(chunkItem.withWorldCatAttributes(
                new WorldCatAttributes().withHoldings(Arrays.asList(
                        new Holding().withSymbol("ABC").withAction(Holding.Action.DELETE),
                        new Holding().withSymbol("DEF").withAction(Holding.Action.INSERT))))), is(not(referenceChecksum)));
        assertThat("LHR flag affects checksum", Checksum.of(chunkItem.withWorldCatAttributes(
                        new WorldCatAttributes().withLhr(true)
                                .withHoldings(Arrays.asList(
                                        new Holding().withSymbol("DEF").withAction(Holding.Action.INSERT),
                                        new Holding().withSymbol("ABC").withAction(Holding.Action.INSERT))))),
                is(not(referenceChecksum)));
        assertThat("chunk item data affects checksum", Checksum.of((ChunkItemWithWorldCatAttributes) chunkItem
                .withWorldCatAttributes(attributes).withData("updated record data")), is(not(referenceChecksum)));
    }
}
