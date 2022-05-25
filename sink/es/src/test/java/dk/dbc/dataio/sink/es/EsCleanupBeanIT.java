package dk.dbc.dataio.sink.es;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.sink.es.entity.inflight.EsInFlight;
import org.junit.Test;

import javax.jms.JMSException;
import javax.persistence.EntityTransaction;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class EsCleanupBeanIT extends SinkIT {

    @Test
    public void cleanup_noTaskPackagesAreMarkedAsFinished_leavesEsAndInFlightUnchanged() throws JSONBException, JMSException, IOException, URISyntaxException {

        JPATestUtils.runSqlFromResource(esInFlightEntityManager, this, "EsCleanUpBeanIT_noTaskPackagesAreMarkedAsFinished_testdata.sql");

        assertThat("Number of task packages in ES", findTaskPackages().size(), is(2));

        final EsCleanupBean esCleanupBean = getEsCleanupBean();
        EntityTransaction esTransaction = esCleanupBean.esConnector.entityManager.getTransaction();
        EntityTransaction inFlightTransaction = esInFlightEntityManager.getTransaction();
        esTransaction.begin();
        inFlightTransaction.begin();
        esCleanupBean.startup();
        esCleanupBean.cleanup();
        inFlightTransaction.commit();
        esTransaction.commit();

        // Assert that one ES task package is still in-flight
        assertThat("Number of ES task packages in-flight", listEsInFlight().size(), is(2));

        // Assert that one task package is still to be found in ES
        assertThat("Number of task packages in ES", findTaskPackages().size(), is(2));
    }

    @Test
    public void cleanup_taskPackagesAreMarkedAsFinished_removedFromEsAndInFlight() throws JSONBException, JMSException, SQLException, IOException, URISyntaxException {

        JPATestUtils.runSqlFromResource(esInFlightEntityManager, this, "EsCleanUpBeanIT_taskPackagesAreMarkedAsFinished_testdata.sql");
        jobStoreServiceConnector.jobInfoSnapshots.add(new JobInfoSnapshot());

        final List<EsInFlight> esInFlight = listEsInFlight();

        // Assert that one ES task package is in-flight
        assertThat("Number of ES task packages in-flight", esInFlight.size(), is(1));

        final EsCleanupBean esCleanupBean = getEsCleanupBean();
        EntityTransaction esTransaction = esCleanupBean.esConnector.entityManager.getTransaction();
        EntityTransaction inFlightTransaction = esInFlightEntityManager.getTransaction();
        inFlightTransaction.begin();
        esTransaction.begin();
        esCleanupBean.startup();
        esCleanupBean.cleanup();
        inFlightTransaction.commit();
        esTransaction.commit();


        JPATestUtils.clearEntityManagerCache(esInFlightEntityManager);
        // Assert that no ES task package are in-flight
        assertThat("Number of ES task packages in-flight", listEsInFlight().size(), is(0));

        // Assert that no task packages are to be found in ES
        assertThat("Number of task packages in ES", findTaskPackages().size(), is(0));
    }

}
