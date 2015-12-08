package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JobExporterTest {
    final JobExporter jobExporter = new JobExporter();

    @Test
    public void phaseToCriteriaField_phaseArgIsPartitioning_returnsField() {
        assertThat(jobExporter.phaseToCriteriaField(State.Phase.PARTITIONING), is(ItemListCriteria.Field.PARTITIONING_FAILED));
    }

    @Test
    public void phaseToCriteriaField_phaseArgIsProcessing_returnsField() {
        assertThat(jobExporter.phaseToCriteriaField(State.Phase.PROCESSING), is(ItemListCriteria.Field.PROCESSING_FAILED));
    }

    @Test
    public void phaseToCriteriaField_phaseArgIsDelivering_returnsField() {
        assertThat(jobExporter.phaseToCriteriaField(State.Phase.DELIVERING), is(ItemListCriteria.Field.DELIVERY_FAILED));
    }
}