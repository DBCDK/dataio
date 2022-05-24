package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.sink.es.entity.es.DiagnosticsEntity;
import dk.dbc.dataio.sink.es.entity.es.TaskPackageRecordStructureEntity;
import dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by ja7 on 12-10-15.
 *
 * Test The JPA mappings
 *
 */
public class JPAMapingIT {

    @Test
    public void LoadTaskPackage() throws Exception {
        EntityManager em=JPATestUtils.getIntegrationTestEntityManager("esIT");
        JPATestUtils.runSqlFromResource(em,this, "JPAMappingIT_load_testdata.sql");
        JPATestUtils.clearEntityManagerCache(em);

        TaskSpecificUpdateEntity tp=em.find(TaskSpecificUpdateEntity.class, 1);
        tp.loadDiagsIfExists(em);

        //
        assertThat("ChunkItem0.getStatus()", tp.getSuppliedRecords().size(), is(2));

        assertThat("ChunkItem0.getStatus()", tp.getSuppliedRecords().size(), is(2));

        // load of Tasksp
        List<TaskPackageRecordStructureEntity> taskpackageRecordStructureEntityMap=tp.getTaskpackageRecordStructures();


        TaskPackageRecordStructureEntity recordStructure=taskpackageRecordStructureEntityMap.get(0);
        assertThat("recordStructure(1).diagnosticId", recordStructure.diagnosticId, is(nullValue()));
        assertThat("ChunkItem0.getStatus()", tp.getSuppliedRecords().size(), is(2));

        recordStructure=taskpackageRecordStructureEntityMap.get(1);
        assertThat("recordStructure(1).diagnosticId", recordStructure.diagnosticId, is(notNullValue()));

        List<DiagnosticsEntity> diags=recordStructure.getDiagnosticsEntities( );
        assertThat("ChunkItem[1].diags.size()", diags.size(), is(2));
        assertThat("ChunkItem[1].diag[0]", diags.get(0).additionalInformation, is("diag1"));
        assertThat("ChunkItem[1].diag[1]", diags.get(1).additionalInformation, is("diag2"));

        assertThat("ChunkItem0.getStatus()", tp.getSuppliedRecords().size(), is(2));

    }
}
