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

package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.sink.es.entity.es.DiagnosticsEntity;
import dk.dbc.dataio.sink.es.entity.es.SuppliedRecordsEntity;
import dk.dbc.dataio.sink.es.entity.es.TaskPackageEntity;
import dk.dbc.dataio.sink.es.entity.es.TaskPackageRecordStructureEntity;
import dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ESTaskPackageUtilIT {
    private static Logger LOGGER = LoggerFactory.getLogger(ESTaskPackageUtilIT.class);

    private static String ES_DATABASE_NAME = "dbname";

    private static final String ADDI_OK = "1\na\n1\nb\n";
    private static final TaskSpecificUpdateEntity.UpdateAction ACTION = TaskSpecificUpdateEntity.UpdateAction.INSERT;


    @Test
    public void getChunkForTaskPackage() throws SQLException, ClassNotFoundException, IOException {
        final List<ChunkItem> items = new ArrayList<>(7);
        items.add(new ChunkItemBuilder().setId(0).setStatus(ChunkItem.Status.IGNORE).build());
        items.add(new ChunkItemBuilder().setId(1).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("3")).build());  // OK multiple
        items.add(new ChunkItemBuilder().setId(2).setStatus(ChunkItem.Status.FAILURE).build());
        items.add(new ChunkItemBuilder().setId(3).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("1")).build());  // queued
        items.add(new ChunkItemBuilder().setId(4).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("1")).build());  // in process
        items.add(new ChunkItemBuilder().setId(5).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("1")).build());  // failed with diagnostic
        items.add(new ChunkItemBuilder().setId(6).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("1")).build());  // OK single
        final ExternalChunk placeholderChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setItems(items).build();

        TaskSpecificUpdateEntity taskPackage = new TPCreator(ES_DATABASE_NAME)
                .addAddiRecordWithSuccess(ADDI_OK, "pid:1a")
                .addAddiRecordWithSuccess(ADDI_OK, "pid:1b")
                .addAddiRecordWithSuccess(ADDI_OK, "pid:1c")
                .addAddiRecordWithQueued(ADDI_OK)
                .addAddiRecordWithInprocess(ADDI_OK)
                .addAddiRecordWithFailed(ADDI_OK, "failed")
                .addAddiRecordWithSuccess(ADDI_OK, "pid:6")
                .createTaskpackageEntity();

        final ExternalChunk chunk = ESTaskPackageUtil.getChunkForTaskPackage( taskPackage, placeholderChunk);
        final Iterator<ChunkItem> iterator = chunk.iterator();
        ChunkItem next = iterator.next();
        assertThat("ChunkItem0.getStatus()", next.getStatus(), is(ChunkItem.Status.IGNORE));
        next = iterator.next();
        assertThat("ChunkItem1.getStatus()", next.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("ChunkItem1 content", StringUtil.asString(next.getData()), containsString("pid:1a"));
        assertThat("ChunkItem1 content", StringUtil.asString(next.getData()), containsString("pid:1b"));
        assertThat("ChunkItem1 content", StringUtil.asString(next.getData()), containsString("pid:1c"));
        next = iterator.next();
        assertThat("ChunkItem2.getStatus()", next.getStatus(), is(ChunkItem.Status.FAILURE));
        next = iterator.next();
        assertThat("ChunkItem3.getStatus()", next.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem3 content", StringUtil.asString(next.getData()), containsString("queued"));
        next = iterator.next();
        assertThat("ChunkItem4.getStatus()", next.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem4 content", StringUtil.asString(next.getData()), containsString("in process"));
        next = iterator.next();
        assertThat("ChunkItem5.getStatus()", next.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem5 content", StringUtil.asString(next.getData()), containsString("failed"));
        next = iterator.next();
        assertThat("ChunkItem6.getStatus()", next.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("ChunkItem6 content", StringUtil.asString(next.getData()), containsString("pid:6"));
    }

    @Test
    public void getChunkForTaskPackage_expectedItemContentIsMissingInEs_failsItem() throws SQLException, ClassNotFoundException, IOException {
        final List<ChunkItem> items = new ArrayList<>(2);
        items.add(new ChunkItemBuilder().setId(0).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("3")).build());
        items.add(new ChunkItemBuilder().setId(1).setStatus(ChunkItem.Status.SUCCESS).setData(StringUtil.asBytes("1")).build());
        final ExternalChunk placeholderChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setItems(items).build();

        TaskSpecificUpdateEntity taskPackage = new TPCreator(ES_DATABASE_NAME)
                .addAddiRecordWithSuccess(ADDI_OK, "pid:0a")
                .addAddiRecordWithSuccess(ADDI_OK, "pid:0b")
                .createTaskpackageEntity();

        final ExternalChunk chunk = ESTaskPackageUtil.getChunkForTaskPackage( taskPackage, placeholderChunk);
        final Iterator<ChunkItem> iterator = chunk.iterator();
        ChunkItem next = iterator.next();
        assertThat("ChunkItem0.getStatus()", next.getStatus(), is(ChunkItem.Status.FAILURE));
        next = iterator.next();
        assertThat("ChunkItem1.getStatus()", next.getStatus(), is(ChunkItem.Status.FAILURE));
    }

    @Test
    public void findCompletionStatusForTaskpackages_MAX_WHERE_IN_SIZE_exeeded_allTaskPackagesFound()
            throws SQLException, ClassNotFoundException, IOException, URISyntaxException {

        EntityManager esEntityManager=JPATestUtils.createEntityManagerForIntegrationTest("esIT");

        JPATestUtils.runSqlFromResource(esEntityManager, "EsTaskPackageUtilIT_findCompletionStatus_testdata.sql");

        ESTaskPackageUtil.MAX_WHERE_IN_SIZE = 6;

        List<Integer> targetReferences = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            targetReferences.add(i);
        }

        final Map<Integer, ESTaskPackageUtil.TaskStatus> completionStatusForTaskpackages =
                ESTaskPackageUtil.findCompletionStatusForTaskpackages(JPATestUtils.getEsConnection(), targetReferences);

        assertThat(completionStatusForTaskpackages, is(notNullValue()));
        assertThat(completionStatusForTaskpackages.size(), is(targetReferences.size()));
        for (Integer targetReference : targetReferences) {
            assertThat(completionStatusForTaskpackages.containsKey(targetReference), is(true));
            final ESTaskPackageUtil.TaskStatus taskStatus = completionStatusForTaskpackages.get(targetReference);
            assertThat(taskStatus.getTaskStatus(), is(TaskPackageEntity.TaskStatus.PENDING));
        }
    }

    @Test
    public void deleteTaskpackages_MAX_WHERE_IN_SIZE_exeeded_allTaskPackagesDeleted()
            throws SQLException, ClassNotFoundException, IOException, URISyntaxException {

        EntityManager esEntityManager=JPATestUtils.createEntityManagerForIntegrationTest("esIT");
        JPATestUtils.runSqlFromResource(esEntityManager, "EsTaskPackageUtilIT_findCompletionStatus_testdata.sql");


        ESTaskPackageUtil.MAX_WHERE_IN_SIZE = 6;

        List<Integer> targetReferences = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            targetReferences.add(i);
        }

        ESTaskPackageUtil.deleteTaskpackages(esEntityManager, targetReferences);

        final Map<Integer, ESTaskPackageUtil.TaskStatus> completionStatusForTaskpackages =
                ESTaskPackageUtil.findCompletionStatusForTaskpackages(JPATestUtils.getEsConnection(), targetReferences);

        assertThat(completionStatusForTaskpackages, is(notNullValue()));
        assertThat(completionStatusForTaskpackages.size(), is(0));
    }

    private static class TPCreator {

        private TaskSpecificUpdateEntity taskPackage=new TaskSpecificUpdateEntity();
        private List<SuppliedRecordsEntity> records=new ArrayList<>();
        private List<TaskPackageRecordStructureEntity> taskPackageRecordStructures = new ArrayList<>();


        public TPCreator(String dbname) {
            taskPackage.setDatabasename(dbname);
            taskPackage.setTargetreference( 1 );
        }

        public TPCreator addAddiRecordWithSuccess(String addi, String record_id) {
            if (addi == null || record_id == null) {
                throw new NullPointerException("Arguements to addAddiRecordWithSuccess can not be null!");
            }

            int lbnr=records.size();
            createRecordStructure(lbnr, record_id, TaskPackageRecordStructureEntity.RecordStatus.SUCCESS);

            createSuppliedRecord(lbnr, addi, record_id);

            return this;
        }


        private TPCreator addAddiRecordWithQueued(String addi) {
            if (addi == null) {
                throw new NullPointerException("Arguements to addAddiRecordWithQueued can not be null!");
            }

            int lbnr=records.size();
            createRecordStructure(lbnr, "", TaskPackageRecordStructureEntity.RecordStatus.QUEUED);
            createSuppliedRecord(lbnr, addi, "");

            return this;
        }

        private TPCreator addAddiRecordWithInprocess(String addi) {
            if (addi == null) {
                throw new NullPointerException("Arguements to addAddiRecordWithInprocess can not be null!");
            }
            int lbnr=records.size();
            createRecordStructure(lbnr, "", TaskPackageRecordStructureEntity.RecordStatus.IN_PROCESS);
            createSuppliedRecord(lbnr, addi, "");

            return this;
        }

        private TPCreator addAddiRecordWithFailed(String addi, String message) {
            if (addi == null || message == null) {
                throw new NullPointerException("Arguements to addAddiRecordWithFailed can not be null!");
            }
            int lbnr=records.size();
            createRecordStructure_withDiag(lbnr, "", TaskPackageRecordStructureEntity.RecordStatus.FAILURE, message);
            createSuppliedRecord(lbnr, addi, message);

            // missing Set failoure diagnostics

            return this;
        }

        public TaskSpecificUpdateEntity createTaskpackageEntity() throws IllegalStateException, NumberFormatException, IOException, SQLException {
            taskPackage.setSuppliedRecords(records);
            taskPackage.setTaskpackageRecordStructures(taskPackageRecordStructures);

            return taskPackage;
        }

        private void createSuppliedRecord(int lbnr, String addi, String record_id) {
            SuppliedRecordsEntity suppliedRecord=new SuppliedRecordsEntity();
            suppliedRecord.lbnr=lbnr;
            suppliedRecord.metaData = addi;
            suppliedRecord.record = "Missing".getBytes();
            records.add( suppliedRecord );
        }

        private void createRecordStructure(int lbnr, String record_id, TaskPackageRecordStructureEntity.RecordStatus recordStatus ) {
            TaskPackageRecordStructureEntity recordStructure=new TaskPackageRecordStructureEntity();
            recordStructure.lbnr = lbnr;
            recordStructure.recordStatus= recordStatus;
            recordStructure.record_id = record_id;
            taskPackageRecordStructures.add(recordStructure);
        }

        private void createRecordStructure_withDiag(int lbnr, String record_id, TaskPackageRecordStructureEntity.RecordStatus recordStatus, String message ) {
            TaskPackageRecordStructureEntity recordStructure=new TaskPackageRecordStructureEntity();
            recordStructure.lbnr = lbnr;
            recordStructure.recordStatus= recordStatus;
            recordStructure.record_id = record_id;
            recordStructure.diagnosticId = 1;
            List<DiagnosticsEntity> diags=new ArrayList<>();
            diags.add( new DiagnosticsEntity(0, message));


            recordStructure.setDiagnosticsEntities( diags );
            taskPackageRecordStructures.add( recordStructure);
        }

    }
}
