/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.periodicjobs;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class GroupHeaderProducerTest {
    @Test
    public void test() {
        final PeriodicJobsDataBlock block1 = new PeriodicJobsDataBlock();
        block1.setGroupHeader("group one".getBytes(StandardCharsets.UTF_8));
        final PeriodicJobsDataBlock block2 = new PeriodicJobsDataBlock();
        block2.setGroupHeader("group two".getBytes(StandardCharsets.UTF_8));
        final PeriodicJobsDataBlock block3 = new PeriodicJobsDataBlock();
        block3.setGroupHeader("group one".getBytes(StandardCharsets.UTF_8));

        final GroupHeaderProducer producer = new GroupHeaderProducer();
        assertThat("group one first call of producer", producer.getGroupHeaderFor(block1).get(),
                is(block1.getGroupHeader()));
        assertThat("group two first call of producer", producer.getGroupHeaderFor(block2).get(),
                is(block2.getGroupHeader()));
        assertThat("group one subsequent call same instance", producer.getGroupHeaderFor(block1),
                is(Optional.empty()));
        assertThat("group two subsequent call same instance", producer.getGroupHeaderFor(block2),
                is(Optional.empty()));
        assertThat("group one subsequent call another instance", producer.getGroupHeaderFor(block3),
                is(Optional.empty()));
    }
}