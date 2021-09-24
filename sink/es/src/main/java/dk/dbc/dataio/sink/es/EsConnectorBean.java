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

package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.EsSinkConfig;
import dk.dbc.dataio.sink.es.entity.es.TaskPackageEntity;
import dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity;
import dk.dbc.dataio.sink.types.SinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateless
public class EsConnectorBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(EsConnectorBean.class);

    @PersistenceContext(unitName = "esPU")
    EntityManager entityManager;

    public int insertEsTaskPackage(EsWorkload esWorkload, EsSinkConfig sinkConfig) throws SinkException {
        LOGGER.debug("Getting connection");
        try {
            LOGGER.debug("Inserting task package");
            return ESTaskPackageUtil.insertTaskPackage(entityManager, sinkConfig.getDatabaseName(), esWorkload);
        } catch (SQLException e) {
            throw new SinkException("Failed to insert ES task package", e);
        }
    }

    public Map<Integer, ESTaskPackageUtil.TaskStatus> getCompletionStatusForESTaskpackages(List<Integer> targetReferences) throws SinkException {
        Map<Integer, ESTaskPackageUtil.TaskStatus> res = new HashMap<>();
        for(Integer targetReference : targetReferences ) {
            TaskPackageEntity taskPackageEntity = entityManager.find(TaskPackageEntity.class, targetReference);
            if(taskPackageEntity == null) {
                LOGGER.info("TaskPackageEntity with id: {} not found", targetReference);
            } else {
                entityManager.refresh(taskPackageEntity);
                res.put(targetReference, new ESTaskPackageUtil.TaskStatus(taskPackageEntity.getTaskStatus(), targetReference));
            }
        }
        return res;
    }

    public void deleteESTaskpackages(List<Integer> targetReferences) throws SinkException {
        try {
            ESTaskPackageUtil.deleteTaskpackages(entityManager, targetReferences);
        } catch (Exception e) {
            LOGGER.warn("Exception caught while deleting ES-taskpackages.", e);
            throw new SinkException("Failed to delete task packages", e);
        }
    }

    public Chunk getChunkForTaskPackage(int targetReference, Chunk placeholderChunk) throws SinkException {

        TaskSpecificUpdateEntity tpu = entityManager.find(TaskSpecificUpdateEntity.class, targetReference);
        entityManager.refresh( tpu ); // Force Reload of Task package from DB after changes from TPWorkers
        tpu.loadDiagsIfExists( entityManager);
        try {
            return ESTaskPackageUtil.getChunkForTaskPackage(tpu, placeholderChunk);
        } catch (Exception e) {
            LOGGER.warn("Exception caught while creating chunk for ES task package", e);
            throw new SinkException("Failed to create chunk for task package", e);
        }
    }
}
