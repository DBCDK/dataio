package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.sink.es.entity.es.TaskPackageEntity;
import dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity;
import dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity.UpdateAction;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ESTaskPackageUtilIT extends SinkIT {

    private static final String DB_NAME = "dbname";
    private static final Charset ENCODING = Charset.defaultCharset();
    private static final int USER_ID = 2;
    private static final UpdateAction ACTION = UpdateAction.INSERT;


    private EntityManager em;

    @Before
    public void setUp() throws Exception {
        em = JPATestUtils.getIntegrationTestEntityManager("esIT");
        em.getTransaction().begin();
        em.createNativeQuery("delete from taskpackage").executeUpdate();
        em.createNativeQuery("delete from esinflight").executeUpdate();
        em.getTransaction().commit();
    }

    @Test
    public void insertTaskPackage_singleSimpleRecordInWorkload_happyPath() throws Exception {

        final String simpleAddiString = "1\na\n1\nb\n";
        final EsWorkload esWorkload = newEsWorkload(simpleAddiString);

        em.getTransaction().begin();
        int targetRefernce = ESTaskPackageUtil.insertTaskPackage(em, DB_NAME, esWorkload);
        em.getTransaction().commit();
        JPATestUtils.clearEntityManagerCache(em);

        TaskSpecificUpdateEntity resultTP = em.find(TaskSpecificUpdateEntity.class, targetRefernce);

        assertThat(targetRefernce, is(resultTP.getTargetreference().intValue()));
    }

    @Test(expected = NullPointerException.class)
    public void insertTaskPackage_connectionArgIsNull_throws() throws Exception {
        ESTaskPackageUtil.insertTaskPackage(null, DB_NAME, newEsWorkload(""));
    }

    @Test(expected = NullPointerException.class)
    public void insertTaskPackage_dbnameArgIsNull_throws() throws Exception {
        ESTaskPackageUtil.insertTaskPackage(em, null, newEsWorkload(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertTaskPackage_dbnameArgIsEmpty_throws() throws Exception {
        ESTaskPackageUtil.insertTaskPackage(em, "", newEsWorkload(""));
    }

    @Test(expected = NullPointerException.class)
    public void insertTaskPackage_esWorkloadArgIsNull_throws() throws Exception {
        ESTaskPackageUtil.insertTaskPackage(em, DB_NAME, null);
    }

    private EsWorkload newEsWorkload(String record) throws IOException {
        return new EsWorkload(new ChunkBuilder(Chunk.Type.DELIVERED).build(),
                Collections.singletonList(newAddiRecordFromString(record)), USER_ID, ACTION);
    }


    private AddiRecord newAddiRecordFromString(String record) throws IOException {
        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(record.getBytes(ENCODING)));
        return addiReader.getNextRecord();
    }


    @Test
    public void deleteTaskpackages_MAX_WHERE_IN_SIZE_exeeded_allTaskPackagesDeleted()
            throws SQLException, ClassNotFoundException, IOException, URISyntaxException {

        JPATestUtils.runSqlFromResource(em, this, "EsTaskPackageUtilIT_findCompletionStatus_testdata.sql");


        List<Integer> targetReferences = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            targetReferences.add(i);
        }

        em.getTransaction().begin();
        ESTaskPackageUtil.deleteTaskpackages(em, targetReferences);
        em.getTransaction().commit();

        final Map<Integer, ESTaskPackageUtil.TaskStatus> completionStatusForTaskpackages =
                ESTaskPackageUtil.findCompletionStatusForTaskpackages(JPATestUtils.getConnection(), targetReferences);

        assertThat(completionStatusForTaskpackages, is(notNullValue()));
        assertThat(completionStatusForTaskpackages.size(), is(0));
    }

    @Test
    public void findCompletionStatusForTaskpackages_MAX_WHERE_IN_SIZE_exeeded_allTaskPackagesFound()
            throws SQLException, ClassNotFoundException, IOException, URISyntaxException {

        JPATestUtils.runSqlFromResource(em, this, "EsTaskPackageUtilIT_findCompletionStatus_testdata.sql");

        ESTaskPackageUtil.MAX_WHERE_IN_SIZE = 6;

        List<Integer> targetReferences = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            targetReferences.add(i);
        }

        final Map<Integer, ESTaskPackageUtil.TaskStatus> completionStatusForTaskpackages =
                ESTaskPackageUtil.findCompletionStatusForTaskpackages(JPATestUtils.getConnection(), targetReferences);

        assertThat(completionStatusForTaskpackages, is(notNullValue()));
        assertThat(completionStatusForTaskpackages.size(), is(targetReferences.size()));
        for (Integer targetReference : targetReferences) {
            assertThat(completionStatusForTaskpackages.containsKey(targetReference), is(true));
            final ESTaskPackageUtil.TaskStatus taskStatus = completionStatusForTaskpackages.get(targetReference);
            assertThat(taskStatus.getTaskStatus(), is(TaskPackageEntity.TaskStatus.PENDING));
        }
    }


    @Test
    public void insertMultiblePackages() throws Exception {
        final String simpleAddiString = "1\na\n1\nb\n";
        List<Integer> targetRefences = new ArrayList<>();

        for (int i = 0; i < 10; ++i) {
            final EsWorkload esWorkload = new EsWorkload(new ChunkBuilder(Chunk.Type.DELIVERED).setJobId(i).build(),
                    Collections.singletonList(newAddiRecordFromString(simpleAddiString)), USER_ID, ACTION);


            em.getTransaction().begin();
            int targetRefernce = ESTaskPackageUtil.insertTaskPackage(em, DB_NAME, esWorkload);
            targetRefences.add(targetRefernce);
            em.getTransaction().commit();
        }
        JPATestUtils.clearEntityManagerCache(em);

        for (Integer targetRefernce : targetRefences) {
            TaskSpecificUpdateEntity resultTP = em.find(TaskSpecificUpdateEntity.class, targetRefernce);

            assertThat(targetRefernce, is(resultTP.getTargetreference().intValue()));
        }
        ;


    }
}
