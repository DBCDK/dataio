/*
 * DataIO - Data IO
 *
 * Copyright (C) 2017 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JobExporterIT extends AbstractJobStoreIT {
    /**
     * Given: a job with an item failed during delivery with an ERROR
     *        level diagnostic
     * When : exporting failed items content for items failed during
     *        delivery
     * Then : the exported content has the hasFatalItems flag set to false
     */
    @Test
    public void exportFailedItemsContent() throws JobStoreException {
        // Given...
        final JobEntity jobEntity = newJobEntity();
        jobEntity.getState()
                .updateState(new StateChange()
                    .setPhase(State.Phase.DELIVERING)
                    .incFailed(1));
        persist(jobEntity);

        newPersistedChunkEntity(new ChunkEntity.Key(0, jobEntity.getId()));

        final ItemEntity itemErrorDuringDelivery = newItemEntity(
                new ItemEntity.Key(jobEntity.getId(), 0, (short) 0));
        itemErrorDuringDelivery.getState()
                .updateState(new StateChange()
                    .setPhase(State.Phase.DELIVERING)
                    .incFailed(1));
        itemErrorDuringDelivery.setProcessingOutcome(new ChunkItemBuilder()
                .setData("processing output")
                .build());
        itemErrorDuringDelivery.setDeliveringOutcome(new ChunkItemBuilder()
                .setData("delivery output")
                .setDiagnostics(Collections.singletonList(
                        new Diagnostic(Diagnostic.Level.ERROR, "error")))
                .build());
        persist(itemErrorDuringDelivery);

        // When...
        final JobExporter jobExporter = new JobExporter(entityManager);
        final JobExporter.FailedItemsContent failedItemsContent =
                jobExporter.exportFailedItemsContent(jobEntity.getId(),
                        Collections.singletonList(State.Phase.DELIVERING),
                        ChunkItem.Type.STRING, StandardCharsets.UTF_8);

        // Then...
        assertThat(failedItemsContent.hasFatalItems(), is(false));
    }

    /**
     * Given: a job with an item failed during delivery with an ERROR
     *        level diagnostic and another item failed during delivery
     *        having multiple diagnostics, one of those with FATAL level
     * When : exporting failed items content for items failed during
     *        delivery
     * Then : the exported content has the hasFatalItems flag set to true
     */
    @Test
    public void exportFailedItemsContent_setsHasFatalItemsFlag() throws JobStoreException {
        // Given...
        final JobEntity jobEntity = newJobEntity();
        jobEntity.getState()
                .updateState(new StateChange()
                    .setPhase(State.Phase.DELIVERING)
                    .incFailed(1));
        persist(jobEntity);

        newPersistedChunkEntity(new ChunkEntity.Key(0, jobEntity.getId()));

        final ItemEntity itemErrorDuringDelivery = newItemEntity(
                new ItemEntity.Key(jobEntity.getId(), 0, (short) 0));
        itemErrorDuringDelivery.getState()
                .updateState(new StateChange()
                    .setPhase(State.Phase.DELIVERING)
                    .incFailed(1));
        itemErrorDuringDelivery.setProcessingOutcome(new ChunkItemBuilder()
                .setData("processing output")
                .build());
        itemErrorDuringDelivery.setDeliveringOutcome(new ChunkItemBuilder()
                .setData("delivery output")
                .setDiagnostics(Collections.singletonList(
                        new Diagnostic(Diagnostic.Level.ERROR, "error")))
                .build());
        persist(itemErrorDuringDelivery);

        final ItemEntity itemFatalDuringDelivery = newItemEntity(
                new ItemEntity.Key(jobEntity.getId(), 0, (short) 1));
        itemFatalDuringDelivery.getState()
                .updateState(new StateChange()
                    .setPhase(State.Phase.DELIVERING)
                    .incFailed(1));
        itemFatalDuringDelivery.setProcessingOutcome(new ChunkItemBuilder()
                .setData("processing output")
                .build());
        itemFatalDuringDelivery.setDeliveringOutcome(new ChunkItemBuilder()
                .setData("delivery output")
                .setDiagnostics(Arrays.asList(
                        new Diagnostic(Diagnostic.Level.ERROR, "error"),
                        new Diagnostic(Diagnostic.Level.FATAL, "died"),
                        new Diagnostic(Diagnostic.Level.ERROR, "error")))
                .build());
        persist(itemFatalDuringDelivery);

        // When...
        final JobExporter jobExporter = new JobExporter(entityManager);
        final JobExporter.FailedItemsContent failedItemsContent =
                jobExporter.exportFailedItemsContent(jobEntity.getId(),
                        Collections.singletonList(State.Phase.DELIVERING),
                        ChunkItem.Type.BYTES, StandardCharsets.UTF_8);

        // Then...
        assertThat(failedItemsContent.hasFatalItems(), is(true));
    }
}
