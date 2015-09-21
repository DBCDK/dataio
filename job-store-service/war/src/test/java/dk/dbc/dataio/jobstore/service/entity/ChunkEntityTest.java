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

import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import dk.dbc.dataio.jobstore.service.sequenceanalyser.ChunkIdentifier;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ChunkEntityTest {
    @Test
    public void toCollisionDetectionElement() {
        final int jobId = 42;
        final int chunkId = 1;
        final SequenceAnalysisData sequenceAnalysisData = new SequenceAnalysisData(new HashSet<>(Arrays.asList("key")));
        final ChunkEntity chunkEntity = new ChunkEntity();
        chunkEntity.setKey(new ChunkEntity.Key(chunkId, jobId));
        chunkEntity.setSequenceAnalysisData(sequenceAnalysisData);

        final CollisionDetectionElement cde = chunkEntity.toCollisionDetectionElement();
        final ChunkIdentifier chunkIdentifier = (ChunkIdentifier) cde.getIdentifier();
        assertThat("CollisionDetectionElement", cde, is(notNullValue()));
        assertThat("CollisionDetectionElement.getIdentifier()", cde.getIdentifier(), is(notNullValue()));
        assertThat("CollisionDetectionElement.getIdentifier().getJobId()", (int) chunkIdentifier.getJobId(), is(jobId));
        assertThat("CollisionDetectionElement.getIdentifier().getChunkId(),", (int) chunkIdentifier.getChunkId(), is(chunkId));
        assertThat("CollisionDetectionElement.getKeys()", cde.getKeys(), is(sequenceAnalysisData.getData()));
    }

}