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
import dk.dbc.dataio.filestore.service.connector.MockedFileStoreServiceConnector;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.ChunkEntity;
import dk.dbc.dataio.jobstore.service.entity.ItemEntity;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.StateChange;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JobExporterIT extends AbstractJobStoreIT {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

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
        itemErrorDuringDelivery.setPartitioningOutcome(new ChunkItemBuilder()
                .setType(ChunkItem.Type.STRING)
                .setData("partitioning output")
                .build());
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
                        null, // get type from getExportType
                        StandardCharsets.UTF_8);

        // Then...
        assertThat("hasFatalItems", failedItemsContent.hasFatalItems(), is(false));
        assertThat("getType", failedItemsContent.getType(), is(ChunkItem.Type.BYTES));
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
        assertThat("hasFatalItems", failedItemsContent.hasFatalItems(), is(true));
        assertThat("getType", failedItemsContent.getType(), is(ChunkItem.Type.BYTES));
    }

    /**
     * Given: a job with 6 items, four of which were successful during the processing phase
     * When : exporting items for the processing phase to file-store
     * Then : an export file is uploaded to the file-store
     *  And : the export file contains content for the successful items
     *  And : the export file has metadata identifying is as a job-store export
     */
    @Test
    public void exportItemsDataToFileStore() throws JobStoreException, IOException {
        // Intercept export file with mocked FileStoreServiceConnectorBean
        final Path exportFile = tmpFolder.newFile().toPath();
        final MockedFileStoreServiceConnector fileStoreServiceConnector = new MockedFileStoreServiceConnector();
        fileStoreServiceConnector.destinations.add(exportFile);

        // Given...
        final JobEntity jobEntity = newPersistedJobEntity();

        newPersistedChunkEntity(new ChunkEntity.Key(0, jobEntity.getId()));

        final ItemEntity item0 = newItemEntity(new ItemEntity.Key(jobEntity.getId(), 0, (short) 0));
        item0.setProcessingOutcome(new ChunkItemBuilder()
                .setStatus(ChunkItem.Status.SUCCESS)
                .setData("0")
                .build());
        persist(item0);
        final ItemEntity item1 = newItemEntity(new ItemEntity.Key(jobEntity.getId(), 0, (short) 1));
        item1.setProcessingOutcome(new ChunkItemBuilder()
                .setStatus(ChunkItem.Status.SUCCESS)
                .setData("1")
                .build());
        persist(item1);
        final ItemEntity item2 = newItemEntity(new ItemEntity.Key(jobEntity.getId(), 0, (short) 2));
        item2.setProcessingOutcome(new ChunkItemBuilder()
                .setStatus(ChunkItem.Status.FAILURE)
                .setData("2")
                .build());
        persist(item2);
        final ItemEntity item3 = newItemEntity(new ItemEntity.Key(jobEntity.getId(), 0, (short) 3));
        item3.setProcessingOutcome(new ChunkItemBuilder()
                .setStatus(ChunkItem.Status.SUCCESS)
                .setData("3")
                .build());
        persist(item3);
        final ItemEntity item4 = newItemEntity(new ItemEntity.Key(jobEntity.getId(), 0, (short) 4));
        item4.setProcessingOutcome(new ChunkItemBuilder()
                .setStatus(ChunkItem.Status.FAILURE)
                .setData("4")
                .build());
        persist(item4);
        final ItemEntity item5 = newItemEntity(new ItemEntity.Key(jobEntity.getId(), 0, (short) 5));
        item5.setProcessingOutcome(new ChunkItemBuilder()
                .setStatus(ChunkItem.Status.SUCCESS)
                .setData("5")
                .build());
        persist(item5);

        // When...
        final JobExporter jobExporter = new JobExporter(entityManager);
        final String fileStoreURL = jobExporter.exportItemsDataToFileStore(
                jobEntity.getId(), State.Phase.PROCESSING, fileStoreServiceConnector);

        // Then...
        final String expectedFileStoreUrl = String.join("/",
                MockedFileStoreServiceConnector.BASEURL, "files", fileStoreServiceConnector.getCurrentFileId());
        assertThat("file-store file URL", fileStoreURL, is(expectedFileStoreUrl));

        // And...
        final String exportContent = new String(Files.readAllBytes(exportFile), StandardCharsets.UTF_8);
        assertThat("file-store file content", exportContent, is("0135"));

        // And...
        assertThat("file-store file metadata", fileStoreServiceConnector.metadata,
                is(JobExporter.FILE_STORE_METADATA));
    }
}
