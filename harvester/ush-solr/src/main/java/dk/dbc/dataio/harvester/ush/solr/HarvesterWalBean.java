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

package dk.dbc.dataio.harvester.ush.solr;

import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.ush.solr.entity.ProgressWal;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Optional;

/**
 * Simple write-ahead-log for harvester redo functionality.
 * <pre>
 * {@code
 *
 *  Optional<ProgressWal> entry = harvesterWal.read(configId);
 *  if (entry.isPresent()) {
 *      // redo whatever
 *      harvesterWal.commit(entry);
 *  }
 *  // Update entry...
 *
 *  harvesterWal.write(entry);
 *  ...
 *  harvesterWal.commit(entry);
 * }
 * </pre>
 */
@Stateless
public class HarvesterWalBean {
    @PersistenceContext
    EntityManager entityManager;

    /**
     * Reads non-committed progress from this WAL if it exists
     * @return ProgressWal, otherwise empty
     * @throws HarvesterException on error while reading WAL entry
     */
    public Optional<ProgressWal> read(long configId) throws HarvesterException {
        try {
            final ProgressWal progressWal = entityManager.find(ProgressWal.class, configId);
            if (progressWal != null) {
                return Optional.of(progressWal);
            }
            return Optional.empty();
        } catch (RuntimeException e) {
            throw new HarvesterException("Unexpected exception caught while reading wal entry for config ID: " + configId, e);
        }
    }

    /**
     * Writes progress WAL entry
     * @param progressWal Progress WAL entry to be written
     * @throws HarvesterException on attempting to write to WAL already containing non-committed entry, or on general error writing WAL
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void write(ProgressWal progressWal) throws HarvesterException {
        try {
            entityManager.persist(progressWal);
        } catch (RuntimeException e) {
            throw new HarvesterException("Unexpected exception caught while writing wal entry: " + progressWal, e);
        }
    }

    /**
     * Commits (removes) this WAL
     * @param progressWal Progress WAL entry to be committed
     * @throws HarvesterException on failure to commit
     */
    public void commit(ProgressWal progressWal) throws HarvesterException {
        try {
            entityManager.remove(progressWal);
        } catch (RuntimeException e) {
            throw new HarvesterException("Commit failed for wal entry: " + progressWal, e);
        }
    }
}
