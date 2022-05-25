package dk.dbc.dataio.sink.periodicjobs;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class GroupHeaderIncludePredicateTest {
    @Test
    public void test() {
        final PeriodicJobsDataBlock block1 = new PeriodicJobsDataBlock();
        block1.setGroupHeader("group one".getBytes(StandardCharsets.UTF_8));
        final PeriodicJobsDataBlock block2 = new PeriodicJobsDataBlock();
        block2.setGroupHeader("group two".getBytes(StandardCharsets.UTF_8));
        final PeriodicJobsDataBlock block3 = new PeriodicJobsDataBlock();
        block3.setGroupHeader("group one".getBytes(StandardCharsets.UTF_8));

        final GroupHeaderIncludePredicate predicate = new GroupHeaderIncludePredicate();
        assertThat("group one first test", predicate.test(block1), is(true));
        assertThat("group two first test", predicate.test(block2), is(true));
        assertThat("group one subsequent test same instance", predicate.test(block1), is(false));
        assertThat("group two subsequent test same instance", predicate.test(block2), is(false));
        assertThat("group one subsequent test another instance", predicate.test(block3), is(false));
    }
}
