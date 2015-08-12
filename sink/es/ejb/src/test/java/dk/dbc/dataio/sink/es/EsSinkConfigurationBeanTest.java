package dk.dbc.dataio.sink.es;

import dk.dbc.commons.es.ESUtil;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * EsSinkConfigurationBean unit tests.
 * The test methods of this class uses the following naming convention:
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class EsSinkConfigurationBeanTest {
    @Test
    public void getEsResourceName_esResourceNameResourceIsSet_returnsValue() {
        final String resourceValue = "resourceName";
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        configuration.esResourceName = resourceValue;
        assertThat(configuration.getEsResourceName(), is(resourceValue));
    }

    @Test
    public void getEsDatabaseName_esDatabaseNameResourceIsSet_returnsValue() {
        final String resourceValue = "databaseName";
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        configuration.esDatabaseName = resourceValue;
        assertThat(configuration.getEsDatabaseName(), is(resourceValue));
    }

    @Test
    public void getEsUserId_esUserIdResourceIsNotSet_returnsDefaultValue() {
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        assertThat(configuration.getEsUserId(), is(EsSinkConfigurationBean.DEFAULT_USER_ID));
    }

    @Test
    public void getEsUserId_esUserIdResourceIsSet_returnsValue() {
        final int userId = 42;
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        configuration.esUserId = userId;
        assertThat(configuration.getEsUserId(), is(userId));
    }

    @Test
    public void getEsPackageType_esPackageTypeResourceIsNotSet_returnsDefaultValue() {
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        assertThat(configuration.getEsPackageType(), is(ESUtil.PackageType.valueOf(EsSinkConfigurationBean.DEFAULT_PACKAGE_TYPE)));
    }

    @Test
    public void getEsPackageType_esPackageTypeResourceIsSet_returnsValue() {
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        configuration.esPackageType = ESUtil.PackageType.PERSISTENT_QUERY.name().toLowerCase();
        assertThat(configuration.getEsPackageType(), is(ESUtil.PackageType.PERSISTENT_QUERY));
    }

    @Test
    public void getEsAction_esActionResourceIsNotSet_returnsDefaultValue() {
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        assertThat(configuration.getEsAction(), is(ESUtil.Action.valueOf(EsSinkConfigurationBean.DEFAULT_ACTION)));
    }

    @Test
    public void getEsAction_esActionResourceIsSet_returnsValue() {
        final EsSinkConfigurationBean configuration = new EsSinkConfigurationBean();
        configuration.esAction = ESUtil.Action.INSERT.name().toLowerCase();
        assertThat(configuration.getEsAction(), is(ESUtil.Action.INSERT));
    }
}
