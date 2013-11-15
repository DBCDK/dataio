package dk.dbc.dataio.sink.es;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * EsSinkConfigurationBean unit tests.
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class EsSinkConfigurationBeanTest {
    @Test
    public void getEsResourceName_resourceIsSet_returnsValue() {
        final String resourceValue = "resourceName";
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        configuration.esResourceName = resourceValue;
        assertThat(configuration.getEsResourceName(), is(resourceValue));
    }

    @Test
    public void getEsDatabaseName_resourceIsSet_returnsValue() {
        final String resourceValue = "databaseName";
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        configuration.esDatabaseName = resourceValue;
        assertThat(configuration.getEsDatabaseName(), is(resourceValue));
    }

    @Test
    public void getEsRecordsCapacity_resourceIsSet_returnsValue() {
        final int resourceValue = 42;
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        configuration.esRecordsCapacity = resourceValue;
        assertThat(configuration.getRecordsCapacity(), is(resourceValue));
    }
}
