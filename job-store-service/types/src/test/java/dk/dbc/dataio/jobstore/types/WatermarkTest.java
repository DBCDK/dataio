package dk.dbc.dataio.jobstore.types;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThan;

class WatermarkTest {
    private final Watermark watermark = new Watermark(42, 7, (short) 3);

    @Test
    void compareTo_jobIdIsComparedFirst() {
        assertThat(new Watermark(41, 9, (short) 9).compareTo(watermark), is(lessThan(0)));
        assertThat(new Watermark(43, 0, (short) 0).compareTo(watermark), is(greaterThan(0)));
    }

    @Test
    void compareTo_chunkIdBreaksJobIdTies() {
        assertThat(new Watermark(42, 6, (short) 9).compareTo(watermark), is(lessThan(0)));
        assertThat(new Watermark(42, 8, (short) 0).compareTo(watermark), is(greaterThan(0)));
    }

    @Test
    void compareTo_itemIdBreaksChunkIdTies() {
        assertThat(new Watermark(42, 7, (short) 2).compareTo(watermark), is(lessThan(0)));
        assertThat(new Watermark(42, 7, (short) 4).compareTo(watermark), is(greaterThan(0)));
    }

    @Test
    void compareTo_equalVersionsCompareEqual() {
        assertThat(new Watermark(42, 7, (short) 3).compareTo(watermark), is(0));
    }
}
