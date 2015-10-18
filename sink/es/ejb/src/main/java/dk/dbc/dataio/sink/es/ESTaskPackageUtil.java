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

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.es.ESUtil;
import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.sink.es.entity.es.DiagnosticsEntity;
import dk.dbc.dataio.sink.es.entity.es.SuppliedRecordsEntity;
import dk.dbc.dataio.sink.es.entity.es.TaskPackageEntity;
import dk.dbc.dataio.sink.es.entity.es.TaskPackageRecordStructureEntity;
import dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity;
import dk.dbc.dataio.sink.es.entity.es.TaskStatusConverter;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import javax.persistence.EntityManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class ESTaskPackageUtil {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(ESTaskPackageUtil.class);
    private static final String ES_TASKPACKAGE_CREATOR_FIELD_PREFIX = "dataio: ";
    // non-private/non-final to overwrite during integration tests
    static int MAX_WHERE_IN_SIZE = 1000;

    /**
     * Chops a list up into sublists of length sublistSize
     * @param list list to be chopped up
     * @param sublistSize maximum size of sublists
     * @param <T> the type of the object
     * @return list of sublists
     * @throws NullPointerException if given null-valued list
     * @throws IllegalArgumentException if given sublistSize less than or equal to zero
     */
    static <T> List<List<T>> chopUp(List<T> list, int sublistSize) throws NullPointerException, IllegalArgumentException {
        if (list == null) {
            throw new NullPointerException("list can not be null");
        }
        if (!(sublistSize > 0)) {
            throw new IllegalArgumentException("sublistSize must be larger than zero");
        }

        final List<List<T>> parts = new ArrayList<>();
        final int listSize = list.size();
        for (int i = 0; i < listSize; i += sublistSize) {
            parts.add(new ArrayList<>(list.subList(i, Math.min(listSize, i + sublistSize))));
        }
        return parts;
    }

    // Tested through integrationtests
    public static Map<Integer, TaskStatus> findCompletionStatusForTaskpackages(Connection conn, List<Integer> targetreferences) throws SQLException {
        LOGGER.entry();
        try {
            final Map<Integer, TaskStatus> taskStatuses = new HashMap<>();
            // Since the 'SELECT ... WHERE targetreference IN' construct used to get completion status
            // has an upper bound of 1000 members of the IN condition we split into multiple
            // iterations if necessary
            for (final List<Integer> trefs : chopUp(targetreferences, MAX_WHERE_IN_SIZE)) {
                final String retrieveStatement = "select targetreference, taskstatus from taskpackage where targetreference in ("
                        + commaSeparatedQuestionMarks(trefs.size()) + ")";
                PreparedStatement ps = null;
                ResultSet rs = null;
                try {
                    ps = JDBCUtil.query(conn, retrieveStatement, trefs.toArray());
                    rs = ps.getResultSet();
                    while (rs.next()) {
                        final int targetreference = rs.getInt(1);
                        final TaskPackageEntity.TaskStatus taskStatus = new TaskStatusConverter().convertToEntityAttribute( rs.getInt(2));
                        taskStatuses.put(targetreference, new TaskStatus(taskStatus, targetreference));
                    }
                } catch (SQLException ex) {
                    LOGGER.warn("SQLException caught while trying to find completion status for taskpackages with retrieve statement: {}", retrieveStatement, ex);
                    throw ex;
                } finally {
                    JDBCUtil.closeResultSet(rs);
                    JDBCUtil.closeStatement(ps);
                }
            }
            return taskStatuses;
        } finally {
            LOGGER.exit();
        }
    }

    // Tested through integrationtests
    public static void deleteTaskpackages(EntityManager em, List<Integer> targetReferences) {
        LOGGER.entry();
        try {
            if (!targetReferences.isEmpty()) {
                for( final Integer targetRef : targetReferences ) {
                    em.getTransaction().begin();
                    em.createNativeQuery(String.format("delete from taskpackage where targetreference=%1$d", targetRef)).executeUpdate();
                    em.getTransaction().commit();
                }
            }
        } finally {
            LOGGER.exit();
        }
    }

    private static String commaSeparatedQuestionMarks(int size) {
        LOGGER.entry();
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < size; i++) {
                sb.append(i < size - 1 ? "?, " : "?");
            }
            return sb.toString();
        } finally {
            LOGGER.exit();
        }
    }

    /**
     * Inserts the addi-records contained in given workload into the ES-base
     * with the given dbname.
     *
     * @param entityManager Database connection to the ES-base.
     * @param dbname ES internal database name in which the task package shall
     * be associated.
     * @param esWorkload Object containing both the addi-records to insert into
     * the ES-base and the originating chunk result.
     *
     * @throws SQLException if a database error occurs.
     * @throws IllegalStateException if the number of records in the chunk and
     * the task package differ.
     *
     * @return target reference
     */
    public static int insertTaskPackage(EntityManager entityManager, String dbname, EsWorkload esWorkload) throws IllegalStateException, SQLException {
        InvariantUtil.checkNotNullOrThrow(entityManager, "entityManager");
        InvariantUtil.checkNotNullNotEmptyOrThrow(dbname, "dbname");
        InvariantUtil.checkNotNullOrThrow(esWorkload, "esInFlight");
        final String creator = createCreatorString(esWorkload.getDeliveredChunk().getJobId(), esWorkload.getDeliveredChunk().getChunkId());

        final TaskSpecificUpdateEntity taskPackage=new TaskSpecificUpdateEntity();

        taskPackage.setCreator( creator );
        taskPackage.setDatabasename( dbname );
        taskPackage.setUserid( esWorkload.userId );
        taskPackage.setAction( esWorkload.getAction());
        entityManager.persist( taskPackage );


        List<SuppliedRecordsEntity> records=new ArrayList<>();
        int i=0;
        for( AddiRecord addi : esWorkload.getAddiRecords()) {
            SuppliedRecordsEntity suppliedRecord=new SuppliedRecordsEntity();
            suppliedRecord.metaData = new String(addi.getMetaData(), esWorkload.getDeliveredChunk().getEncoding());
            suppliedRecord.record = addi.getContentData();
            suppliedRecord.targetreference = taskPackage.getTargetreference();
            suppliedRecord.lbnr=++i;
            records.add( suppliedRecord );
        }
        taskPackage.setSuppliedRecords(records);

        return taskPackage.getTargetreference().intValue();
    }

    public static ExternalChunk getChunkForTaskPackage( TaskSpecificUpdateEntity taskSpecificUpdateEntity, ExternalChunk placeholderChunk) throws SQLException {
        final ExternalChunk chunk = new ExternalChunk(placeholderChunk.getJobId(),
                placeholderChunk.getChunkId(), ExternalChunk.Type.DELIVERED);
        chunk.setEncoding(placeholderChunk.getEncoding());
        final LinkedList<TaskPackageRecordStructureEntity> taskPackageRecordStructureEntityList =
                new LinkedList<>(taskSpecificUpdateEntity.getTaskpackageRecordStructures());
        for (ChunkItem chunkItem : placeholderChunk) {
            if (chunkItem.getStatus() == ChunkItem.Status.SUCCESS) {
                chunkItem = getChunkItemFromTaskPackageRecordStructureData(taskSpecificUpdateEntity, chunkItem, taskPackageRecordStructureEntityList);
            }
            chunk.insertItem(chunkItem);
        }
        return chunk;
    }

    private static ChunkItem getChunkItemFromTaskPackageRecordStructureData(TaskSpecificUpdateEntity taskSpecificUpdateEntity, ChunkItem placeholderChunkItem,
            LinkedList<TaskPackageRecordStructureEntity> taskPackageRecordStructureEntityList) throws NumberFormatException, SQLException {


        final int numberOfRecords = Integer.parseInt(StringUtil.asString(placeholderChunkItem.getData()));
        ChunkItem.Status status = ChunkItem.Status.SUCCESS;
        int targetReference=taskSpecificUpdateEntity.getTargetreference().intValue();
        final StringBuilder itemData = new StringBuilder(String.format("Task package: %d\n",targetReference));
        final String recordHeader = "Record %d: id=%s\n";
        final String recordResult = "\t%s\n";

        List<TaskPackageRecordStructureEntity> recordStructureMap=taskSpecificUpdateEntity.getTaskpackageRecordStructures();

        for (int i = 1; i <= numberOfRecords; i++) {
            TaskPackageRecordStructureEntity recordData;
            try {
                recordData = taskPackageRecordStructureEntityList.remove();
            } catch (NoSuchElementException e) {
                status = ChunkItem.Status.FAILURE;
                itemData.append(String.format(recordHeader, i, null));
                itemData.append(String.format(recordResult, "No record found in ES"));
                continue;
            }

            LOGGER.info("targetRef: {}  status: {} recordDiag: {}", targetReference, recordData.recordStatus.name(), recordData.diagnosticId);

            itemData.append(String.format(recordHeader, i, recordData.record_id));
            // A task package must only be completed if all its records have been completed.
            // Therefore: if the status of a record is anything but success or failure,
            // an error has occurred. The record will be accepted as failed, with a message
            // in the chunk item telling that the record was not completed.
            TaskPackageRecordStructureEntity recordStructure=recordStructureMap.get( recordData.lbnr );
            switch ( recordStructure.recordStatus ) {
                case SUCCESS:
                    itemData.append(String.format(recordResult, "OK"));
                    break;
                case QUEUED:
                    status = ChunkItem.Status.FAILURE;
                    itemData.append(String.format(recordResult, "lbnr " + recordData.lbnr + " is queued"));
                    break;
                case IN_PROCESS:
                    status = ChunkItem.Status.FAILURE;
                    itemData.append(String.format(recordResult, "lbnr " + recordData.lbnr + " is in process"));
                    break;
                case FAILURE:
                    status = ChunkItem.Status.FAILURE;
                    itemData.append(String.format(recordResult, getFailureDiagnostic(recordStructure)));
                    break;
                default:
                    status = ChunkItem.Status.FAILURE;
                    itemData.append(String.format(recordResult, "lbnr " + recordData.lbnr + " has unknown completion status " + recordData.recordStatus.name()));
            }
        }
        return new ChunkItem(placeholderChunkItem.getId(), StringUtil.asBytes(itemData.toString()), status);
    }



    private static String getFailureDiagnostic(TaskPackageRecordStructureEntity recordStructure) throws SQLException {
        List<DiagnosticsEntity> diags=recordStructure.getDiagnosticsEntities();
        String diagnostic = "";
        boolean first=true;
        for(DiagnosticsEntity diag: diags) {
            if( first) {
                diagnostic = diag.additionalInformation;
                first = false;
            } else {
                LOGGER.warn("unexpected diagnostic returned: id: [{}]  diagnostic: [{}]", recordStructure.diagnosticId, diag.additionalInformation);
            }

        }
        return diagnostic;
    }

    private static void validateTaskPackageState(ESUtil.AddiListInsertionResult insertionResult, EsWorkload esWorkload) throws IllegalStateException {
        final int recordSlots = esWorkload.getAddiRecords().size();
        if (recordSlots != insertionResult.getNumberOfInsertedRecords()) {
            throw new IllegalStateException(String.format("The number of records in the chunk and the number of records in the taskpackage differ. Chunk size: %d  TaskPackage size: %d", recordSlots, insertionResult.getNumberOfInsertedRecords()));
        }
    }

    private static String createCreatorString(long jobId, long chunkId) {
        final StringBuilder sb = new StringBuilder();
        sb.append(ES_TASKPACKAGE_CREATOR_FIELD_PREFIX);
        sb.append("[ jobid: ").append(jobId);
        sb.append(" , chunkId: ").append(chunkId).append(" ]");
        return sb.toString();
    }

    public static class TaskStatus {
        private final TaskPackageEntity.TaskStatus taskStatus;
        private final int targetreference;

        public TaskStatus(TaskPackageEntity.TaskStatus taskStatus, int targetreference) {
            this.taskStatus = taskStatus;
            this.targetreference = targetreference;
        }

        public TaskPackageEntity.TaskStatus getTaskStatus() {
            return taskStatus;
        }

        public int getTargetReference() {
            return targetreference;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TaskStatus that = (TaskStatus) o;

            if (targetreference != that.targetreference) {
                return false;
            }
            return taskStatus == that.taskStatus;
        }

        @Override
        public int hashCode() {
            int result = taskStatus.hashCode();
            result = 31 * result + targetreference;
            return result;
        }
    }
}
