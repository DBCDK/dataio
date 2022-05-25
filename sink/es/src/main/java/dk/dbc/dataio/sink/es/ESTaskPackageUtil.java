package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.sink.es.entity.es.DiagnosticsEntity;
import dk.dbc.dataio.sink.es.entity.es.SuppliedRecordsEntity;
import dk.dbc.dataio.sink.es.entity.es.TaskPackageEntity;
import dk.dbc.dataio.sink.es.entity.es.TaskPackageRecordStructureEntity;
import dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity;
import dk.dbc.dataio.sink.es.entity.es.TaskStatusConverter;
import dk.dbc.log.DBCTrackedLogContext;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ESTaskPackageUtil {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(ESTaskPackageUtil.class);
    private static final String ES_TASKPACKAGE_CREATOR_FIELD_PREFIX = "dataio: ";
    // non-private/non-final to overwrite during integration tests
    static int MAX_WHERE_IN_SIZE = 1000;
    private static final String RECORD_HEADER = "Record %d: id=%s\n";
    private static final String RECORD_RESULT = "\t%s\n";

    private static final Pattern referenceUnknownPattern = Pattern.compile(
        "reference in 014 00 a to \\d{8} unknown");

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
                for (final Integer targetRef : targetReferences) {
                    em.createNativeQuery(String.format("delete from taskpackage where targetreference=%1$d", targetRef)).executeUpdate();
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
        taskPackage.setPackagename( creator + String.valueOf(System.nanoTime()));
        taskPackage.setDatabasename( dbname );
        taskPackage.setUserid( esWorkload.userId );
        taskPackage.setAction( esWorkload.getAction());
        entityManager.persist( taskPackage );


        List<SuppliedRecordsEntity> records=new ArrayList<>();
        int i = 0;
        for( AddiRecord addi : esWorkload.getAddiRecords()) {
            SuppliedRecordsEntity suppliedRecord = new SuppliedRecordsEntity();
            suppliedRecord.metaData = new String(addi.getMetaData(), esWorkload.getDeliveredChunk().getEncoding());
            suppliedRecord.record = addi.getContentData();
            suppliedRecord.targetreference = taskPackage.getTargetreference();
            suppliedRecord.lbnr = i;
            records.add( suppliedRecord );
            i++;
        }
        taskPackage.setSuppliedRecords(records);

        return taskPackage.getTargetreference();
    }

    public static Chunk getChunkForTaskPackage(TaskSpecificUpdateEntity taskSpecificUpdateEntity, Chunk placeholderChunk) throws SQLException {
        final Chunk chunk = new Chunk(placeholderChunk.getJobId(),
                placeholderChunk.getChunkId(), Chunk.Type.DELIVERED);
        chunk.setEncoding(placeholderChunk.getEncoding());
        final LinkedList<TaskPackageRecordStructureEntity> taskPackageRecordStructureEntityList =
                new LinkedList<>(taskSpecificUpdateEntity.getTaskpackageRecordStructures());
        try {
            for (ChunkItem chunkItem : placeholderChunk) {
                DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                LOGGER.info("Handling item {} for chunk {} in job {}", chunkItem.getId(), chunk.getChunkId(), chunk.getJobId());
                if (chunkItem.getStatus() == ChunkItem.Status.SUCCESS) {
                    chunkItem = getChunkItemFromTaskPackageRecordStructureData(taskSpecificUpdateEntity, chunkItem, taskPackageRecordStructureEntityList);
                }
                chunk.insertItem(chunkItem);
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        return chunk;
    }

    private static ChunkItem getChunkItemFromTaskPackageRecordStructureData(TaskSpecificUpdateEntity taskSpecificUpdateEntity, ChunkItem placeholderChunkItem,
            LinkedList<TaskPackageRecordStructureEntity> taskPackageRecordStructureEntityList) throws NumberFormatException, SQLException {

        final int numberOfRecords = Integer.parseInt(StringUtil.asString(placeholderChunkItem.getData()));
        final int targetReference = taskSpecificUpdateEntity.getTargetreference();
        final StringBuilder sb = new StringBuilder(String.format("Task package: %d\n", targetReference));
        final List<TaskPackageRecordStructureEntity> recordStructureMap = taskSpecificUpdateEntity.getTaskpackageRecordStructures();
        final List<Diagnostic> chunkItemDiagnostics = new ArrayList<>();

        for (int i = 1; i <= numberOfRecords; i++) {
            TaskPackageRecordStructureEntity recordData;
            try {
                recordData = taskPackageRecordStructureEntityList.remove();
            } catch (NoSuchElementException e) {
                sb.append(String.format(RECORD_HEADER, i, null));
                sb.append(String.format(RECORD_RESULT, "No record found in ES"));
                chunkItemDiagnostics.add(ObjectFactory.buildFatalDiagnostic("No record found in ES", e));
                continue;
            }

            LOGGER.info("targetRef: {}  status: {} recordDiag: {}", targetReference, recordData.recordStatus.name(), recordData.diagnosticId);

            // Append record information to item data
            sb.append(String.format(RECORD_HEADER, i, recordData.record_id));

            // Build es diagnostics if any are present
            final Diagnostic esDiagnostic = buildEsDiagnostic(recordStructureMap.get(recordData.lbnr), recordData);

            // Add any es diagnostic created to the list of chunk item diagnostics and append to string builder
            if(esDiagnostic != null) {
                chunkItemDiagnostics.add(esDiagnostic);
                sb.append(String.format(RECORD_RESULT, esDiagnostic.getMessage()));
            } else {
                sb.append(String.format(RECORD_RESULT, "OK"));
            }
        }

        final ChunkItem chunkItem = ObjectFactory.buildSuccessfulChunkItem(placeholderChunkItem.getId(), sb.toString(), ChunkItem.Type.STRING, placeholderChunkItem.getTrackingId());
        chunkItem.appendDiagnostics(chunkItemDiagnostics);
        return chunkItem;
    }

    /**
     * Creates diagnostic for ES record status errors.
     * @param recordStructure containing the record status
     * @param recordData containing record information
     * @return null, if record status was success, otherwise a diagnostic.
     */
    private static Diagnostic buildEsDiagnostic(TaskPackageRecordStructureEntity recordStructure, TaskPackageRecordStructureEntity recordData) {
        // A task package must only be completed if all its records have been completed.
        // Therefore: if the status of a record is anything but success or failure,
        // an error has occurred. The record will be accepted as failed, with a message
        // in the chunk item telling that the record was not completed.
        switch (recordStructure.recordStatus) {
            case SUCCESS:
                return null;
            case QUEUED:
                return ObjectFactory.buildFatalDiagnostic("lbnr " + recordData.lbnr + " is queued");
            case IN_PROCESS:
                return ObjectFactory.buildFatalDiagnostic("lbnr " + recordData.lbnr + " is in process");
            case FAILURE:
                return wrapEsDiagnostic(recordStructure);
            default:
                return ObjectFactory.buildFatalDiagnostic("lbnr " + recordData.lbnr + " has unknown completion status " + recordData.recordStatus.name());
        }
    }

    private static Diagnostic wrapEsDiagnostic(TaskPackageRecordStructureEntity recordStructure) {
        List<DiagnosticsEntity> diagnosticsEntities = recordStructure.getDiagnosticsEntities();
        Diagnostic diagnostic = null;
        boolean first = true;
        for(DiagnosticsEntity entity: diagnosticsEntities) {
            if( first) {
                final String additionalInformation = entity.additionalInformation;
                Matcher matcher = referenceUnknownPattern.matcher(
                    additionalInformation);
                if ("delete nonexisting record".equals(additionalInformation) || matcher.find()) {
                    diagnostic = new Diagnostic(Diagnostic.Level.WARNING, additionalInformation);
                } else {
                    diagnostic = new Diagnostic(Diagnostic.Level.FATAL, additionalInformation);
                }
                first = false;
            } else {
                LOGGER.warn("unexpected diagnostic returned: id: [{}]  diagnostic: [{}]", recordStructure.diagnosticId, entity.additionalInformation);
            }
        }
        return diagnostic;
    }

    private static String createCreatorString(long jobId, long chunkId) {
        return ES_TASKPACKAGE_CREATOR_FIELD_PREFIX +
                "[ jobid: " + jobId +
                " , chunkId: " + chunkId + " ]";
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

            return targetreference == that.targetreference && taskStatus == that.taskStatus;
        }

        @Override
        public int hashCode() {
            int result = taskStatus.hashCode();
            result = 31 * result + targetreference;
            return result;
        }
    }
}
