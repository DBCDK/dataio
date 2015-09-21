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
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.ItemData;
import dk.dbc.dataio.jobstore.types.State;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ItemEntityTest {
    final static ItemEntity.Key KEY = new ItemEntity.Key(2, 1, (short) 0);
    final static ItemData DATA = new ItemData(StringUtil.base64encode("data"), StandardCharsets.UTF_8);

    @Test
    public void toChunkItem_entityContainsNoData_throws() {
        final ItemEntity entity = new ItemEntity();
        entity.setKey(KEY);
        final State state = new State();
        state.getPhase(State.Phase.PARTITIONING).setSucceeded(1);
        entity.setState(state);
        try {
            entity.toChunkItem(State.Phase.PARTITIONING);
            fail("No Exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void toChunkItem_entityContainsNoState_throws() {
        final ItemEntity entity = new ItemEntity();
        entity.setKey(KEY);
        entity.setPartitioningOutcome(DATA);
        try {
            entity.toChunkItem(State.Phase.PARTITIONING);
            fail("No Exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void toChunkItem_phaseArgIsNull_throws() {
        final ItemEntity entity = new ItemEntity();
        entity.setKey(KEY);
        entity.setPartitioningOutcome(DATA);
        final State state = new State();
        state.getPhase(State.Phase.PARTITIONING).setSucceeded(1);
        entity.setState(state);
        try {
            entity.toChunkItem(null);
            fail("No Exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void toChunkItem_calledWithPartitioningPhase_returnsChunkItem() {
        final ChunkItem expectedChunkItem = new ChunkItem(KEY.getId(),
                StringUtil.asBytes(StringUtil.base64decode(DATA.getData())),
                ChunkItem.Status.SUCCESS);
        final ItemEntity entity = new ItemEntity();
        entity.setKey(KEY);
        entity.setPartitioningOutcome(DATA);
        final State state = new State();
        state.getPhase(State.Phase.PARTITIONING).setSucceeded(1);
        entity.setState(state);
        assertThat(entity.toChunkItem(State.Phase.PARTITIONING), is(expectedChunkItem));
    }

    @Test
    public void toChunkItem_calledWithProcessingPhase_returnsChunkItem() {
        final ChunkItem expectedChunkItem = new ChunkItem(KEY.getId(),
                StringUtil.asBytes(StringUtil.base64decode(DATA.getData())),
                ChunkItem.Status.FAILURE);
        final ItemEntity entity = new ItemEntity();
        entity.setKey(KEY);
        entity.setProcessingOutcome(DATA);
        final State state = new State();
        state.getPhase(State.Phase.PROCESSING).setFailed(1);
        entity.setState(state);
        assertThat(entity.toChunkItem(State.Phase.PROCESSING), is(expectedChunkItem));
    }

    @Test
    public void toChunkItem_calledWithDeliveringPhase_returnsChunkItem() {
        final ChunkItem expectedChunkItem = new ChunkItem(KEY.getId(),
                StringUtil.asBytes(StringUtil.base64decode(DATA.getData())),
                ChunkItem.Status.IGNORE);
        final ItemEntity entity = new ItemEntity();
        entity.setKey(KEY);
        entity.setDeliveringOutcome(DATA);
        final State state = new State();
        state.getPhase(State.Phase.DELIVERING).setIgnored(1);
        entity.setState(state);
        assertThat(entity.toChunkItem(State.Phase.DELIVERING), is(expectedChunkItem));
    }

    @Test
    public void getEncodingForPhase_itemDataSet_returnsEncoding() {
        final ItemEntity entity = new ItemEntity();
        entity.setPartitioningOutcome(DATA);
        assertThat(entity.getEncodingForPhase(State.Phase.PARTITIONING), is(DATA.getEncoding()));
    }

    @Test
    public void getEncodingForPhase_noItemDataSet_returnsNull() {
        final ItemEntity entity = new ItemEntity();
        assertThat(entity.getEncodingForPhase(State.Phase.PARTITIONING), is(nullValue()));
    }
}