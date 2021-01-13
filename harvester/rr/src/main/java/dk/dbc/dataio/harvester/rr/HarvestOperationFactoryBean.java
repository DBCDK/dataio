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

package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.ocnrepo.OcnRepo;
import dk.dbc.phlog.PhLog;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.vipcore.libraryrules.VipCoreLibraryRulesConnector;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.SQLException;

@Stateless
public class HarvestOperationFactoryBean {
    @EJB
    public BinaryFileStoreBean binaryFileStoreBean;

    @EJB
    public FileStoreServiceConnectorBean fileStoreServiceConnectorBean;

    @EJB
    public JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @EJB
    public OcnRepo ocnRepo;

    @EJB
    public PhLog phLog;

    @EJB
    public TaskRepo taskRepo;

    @Inject
    private VipCoreLibraryRulesConnector vipCoreLibraryRulesConnector;

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry metricRegistry;

    public HarvestOperation createFor(RRHarvesterConfig config) {
        final HarvesterJobBuilderFactory harvesterJobBuilderFactory = new HarvesterJobBuilderFactory(binaryFileStoreBean,
                fileStoreServiceConnectorBean.getConnector(), jobStoreServiceConnectorBean.getConnector());
        try {
            switch (config.getContent().getHarvesterType()) {
                case IMS:
                    return new ImsHarvestOperation(config,
                        harvesterJobBuilderFactory, taskRepo,
                            vipCoreLibraryRulesConnector, metricRegistry);
                case WORLDCAT:
                    return new WorldCatHarvestOperation(config,
                        harvesterJobBuilderFactory, taskRepo, vipCoreLibraryRulesConnector,
                        ocnRepo, metricRegistry);
                case PH:
                    return new PhHarvestOperation(config,
                        harvesterJobBuilderFactory, taskRepo, vipCoreLibraryRulesConnector,
                        phLog, metricRegistry);
                default:
                    return new HarvestOperation(config,
                        harvesterJobBuilderFactory, taskRepo,
                            vipCoreLibraryRulesConnector, metricRegistry);
            }
        } catch(ConfigurationException | QueueException | SQLException e) {
            throw new IllegalStateException("ConfigurationException thrown", e);
        }
    }
}
