/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.infomedia;

import dk.dbc.authornamesuggester.AuthorNameSuggesterConnector;
import dk.dbc.authornamesuggester.AuthorNameSuggesterConnectorFactory;
import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.AbstractHarvesterBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;
import dk.dbc.infomedia.InfomediaConnector;
import dk.dbc.infomedia.InfomediaConnectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;

@Singleton
public class HarvesterBean extends AbstractHarvesterBean<HarvesterBean, InfomediaHarvesterConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterBean.class);

    @EJB BinaryFileStoreBean binaryFileStoreBean;
    @EJB FileStoreServiceConnectorBean fileStoreServiceConnectorBean;
    @EJB FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;
    @EJB JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    /*
       Our current version of payara application server does not
       include the microprofile libraries, so for now we are not
       able to @Inject the InfomediaConnector.

       Therefore CDI scanning of the infomedia-connector
       jar dependency has to be disabled via scanning-exclude
       element in glassfish-web.xml
     */

    //@Inject
    InfomediaConnector infomediaConnector;

    /*
       Our current version of payara application server does not
       include the microprofile libraries, so for now we are not
       able to @Inject the AuthorNameSuggesterConnector.

       Therefore CDI scanning of the author-name-suggester-connector
       jar dependency has to be disabled via scanning-exclude
       element in glassfish-web.xml
     */

    //@Inject
    AuthorNameSuggesterConnector authorNameSuggesterConnector;

    @PostConstruct
    public void createRecordServiceConnector() {
        infomediaConnector = InfomediaConnectorFactory.create(
                System.getenv("INFOMEDIA_URL"),
                System.getenv("INFOMEDIA_USERNAME"),
                System.getenv("INFOMEDIA_PASSWORD"));
        authorNameSuggesterConnector = AuthorNameSuggesterConnectorFactory.create(
                System.getenv("AUTHOR_NAME_SUGGESTER_URL"));
    }

    @Override
    public int executeFor(InfomediaHarvesterConfig config) throws HarvesterException {
        return new HarvestOperation(config,
                binaryFileStoreBean,
                flowStoreServiceConnectorBean.getConnector(),
                fileStoreServiceConnectorBean.getConnector(),
                jobStoreServiceConnectorBean.getConnector(),
                infomediaConnector,
                authorNameSuggesterConnector)
                .execute();
    }

    @Override
    public HarvesterBean self() {
        return sessionContext.getBusinessObject(HarvesterBean.class);
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
