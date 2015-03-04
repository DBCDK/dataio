package dk.dbc.dataio.harvester.rr2datawell;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ejb.EJBException;
import javax.naming.Context;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class HarvesterConfigurationBeanTest {
    @BeforeClass
    public static void setup() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @Test
    public void initialize_jndiLookupThrowsNamingException_throws() {
        InMemoryInitialContextFactory.clear();
        final HarvesterConfigurationBean bean = getHarvesterConfigurationBean();
        try {
            bean.initialize();
            fail("No exception thrown");
        } catch (EJBException e) {
        }
    }

    @Test
    public void initialize_jndiLookupReturnsInvalidJson_throws() {
        InMemoryInitialContextFactory.bind(JndiConstants.CONFIG_RESOURCE_HARVESTER_RR, "");
        final HarvesterConfigurationBean bean = getHarvesterConfigurationBean();
        try {
            bean.initialize();
            fail("No exception thrown");
        } catch (EJBException e) {
        }
    }

    @Test
    public void initialize_jndiLookupReturnsValidJson_setsConfig() throws JSONBException {
        final RawRepoHarvesterConfig rawRepoHarvesterConfig = new RawRepoHarvesterConfig();
        InMemoryInitialContextFactory.bind(JndiConstants.CONFIG_RESOURCE_HARVESTER_RR,
                new JSONBContext().marshall(rawRepoHarvesterConfig));

        final HarvesterConfigurationBean bean = getHarvesterConfigurationBean();
        assertThat("config before initialize", bean.config, is(nullValue()));
        bean.initialize();
        assertThat("config after initialize", bean.config, is(notNullValue()));
    }

    @Test
    public void reload_jndiLookupThrowsNamingException_throws() {
        InMemoryInitialContextFactory.clear();
        final HarvesterConfigurationBean bean = getHarvesterConfigurationBean();
        try {
            bean.reload();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void reload_jndiLookupReturnsInvalidJson_throws() {
        InMemoryInitialContextFactory.bind(JndiConstants.CONFIG_RESOURCE_HARVESTER_RR, "");
        final HarvesterConfigurationBean bean = getHarvesterConfigurationBean();
        try {
            bean.reload();
            fail("No exception thrown");
        } catch (HarvesterException e) {
        }
    }

    @Test
    public void reload_jndiLookupReturnsValidJson_setsConfig() throws JSONBException, HarvesterException {
        final RawRepoHarvesterConfig rawRepoHarvesterConfig = new RawRepoHarvesterConfig();
        InMemoryInitialContextFactory.bind(JndiConstants.CONFIG_RESOURCE_HARVESTER_RR,
                new JSONBContext().marshall(rawRepoHarvesterConfig));

        final HarvesterConfigurationBean bean = getHarvesterConfigurationBean();
        assertThat("config before reload", bean.config, is(nullValue()));
        bean.reload();
        assertThat("config after reload", bean.config, is(notNullValue()));
    }

    @Test
    public void get_returnsConfig() throws JSONBException {
        final RawRepoHarvesterConfig rawRepoHarvesterConfig = new RawRepoHarvesterConfig();
        InMemoryInitialContextFactory.bind(JndiConstants.CONFIG_RESOURCE_HARVESTER_RR,
                new JSONBContext().marshall(rawRepoHarvesterConfig));
        final HarvesterConfigurationBean bean = getHarvesterConfigurationBean();
        bean.initialize();
        assertThat(bean.get(), is(bean.config));
    }

    private HarvesterConfigurationBean getHarvesterConfigurationBean() {
        final HarvesterConfigurationBean bean = new HarvesterConfigurationBean();
        bean.jsonbBean = new JSONBBean();
        bean.jsonbBean.initialiseContext();
        return bean;
    }
}