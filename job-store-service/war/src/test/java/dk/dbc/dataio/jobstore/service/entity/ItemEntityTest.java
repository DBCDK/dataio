/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

public class ItemEntityTest {
    final static ItemEntity.Key KEY = new ItemEntity.Key(2, 1, (short) 0);

    @Test
    public void getChunkItemForPhase_phaseArgIsNull_throws() {
        final ItemEntity entity = new ItemEntity();
        entity.setKey(KEY);
        entity.setPartitioningOutcome(new ChunkItemBuilder().build());
        final State state = new State();
        state.getPhase(State.Phase.PARTITIONING).setSucceeded(1);
        entity.setState(state);
        try {
            entity.getChunkItemForPhase(null);
            fail("No Exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void getChunkItemForPhase_calledWithPartitioningPhase_returnsChunkItem() {
        final ChunkItem expectedChunkItem = new ChunkItemBuilder().setId(KEY.getId()).setStatus(ChunkItem.Status.SUCCESS).build();
        final ItemEntity entity = new ItemEntity();
        entity.setKey(KEY);
        entity.setPartitioningOutcome(expectedChunkItem);
        final State state = new State();
        state.getPhase(State.Phase.PARTITIONING).setSucceeded(1);
        entity.setState(state);
        assertThat(entity.getChunkItemForPhase(State.Phase.PARTITIONING), is(expectedChunkItem));
    }

    @Test
    public void getChunkItemForPhase_calledWithProcessingPhase_returnsChunkItem() {
        final ChunkItem expectedChunkItem = new ChunkItemBuilder().setId(KEY.getId()).setStatus(ChunkItem.Status.FAILURE).build();
        final ItemEntity entity = new ItemEntity();
        entity.setKey(KEY);
        entity.setProcessingOutcome(expectedChunkItem);
        final State state = new State();
        state.getPhase(State.Phase.PROCESSING).setFailed(1);
        entity.setState(state);
        assertThat(entity.getChunkItemForPhase(State.Phase.PROCESSING), is(expectedChunkItem));
    }

    @Test
    public void getChunkItemForPhase_calledWithDeliveringPhase_returnsChunkItem() {
        final ChunkItem expectedChunkItem = new ChunkItemBuilder().setId(KEY.getId()).setStatus(ChunkItem.Status.IGNORE).build();
        final ItemEntity entity = new ItemEntity();
        entity.setKey(KEY);
        entity.setDeliveringOutcome(expectedChunkItem);
        final State state = new State();
        state.getPhase(State.Phase.DELIVERING).setIgnored(1);
        entity.setState(state);
        assertThat(entity.getChunkItemForPhase(State.Phase.DELIVERING), is(expectedChunkItem));
    }
}