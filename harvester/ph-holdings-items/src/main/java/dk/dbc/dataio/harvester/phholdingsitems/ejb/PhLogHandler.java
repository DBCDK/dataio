/*
 * DataIO - Data IO
 * Copyright (C) 2017 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.harvester.phholdingsitems.ejb;

import dk.dbc.phlog.PhLog;
import dk.dbc.phlog.dto.PhLogEntry;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import java.util.Map;

@Stateless
public class PhLogHandler {
    @EJB
    PhLog phLog;

    private static final String DECOMMISSIONED_KEY = "Decommissioned";
    /**
     * Updates the phlog based on info about a record.
     *
     * @param agencyId agencyId of the record
     * @param bibliographicRecordId bibliographic id of the record
     * @param statusMap map of holdings data for the record
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updatePhLogEntry(int agencyId, String bibliographicRecordId,
            Map<String, Integer> statusMap) {
        EntityManager entityManager = phLog.getEntityManager();
        boolean isDeleted = recordIsDeleted(statusMap);
        PhLogEntry.Key entryKey = new PhLogEntry.Key()
            .withAgencyId(agencyId)
            .withBibliographicRecordId(bibliographicRecordId);
        PhLogEntry phLogEntry = entityManager.find(PhLogEntry.class, entryKey);
        if(statusMap.isEmpty()) {
            statusMap.put(DECOMMISSIONED_KEY,1);
        }
        if(phLogEntry == null) {
            phLogEntry = new PhLogEntry().withKey(entryKey).withDeleted(isDeleted)
                .withHoldingsStatusMap(statusMap);
            entityManager.persist(phLogEntry);
        } else {
            phLogEntry.withDeleted(isDeleted).withHoldingsStatusMap(statusMap);
        }
    }

    protected static boolean recordIsDeleted(Map<String, Integer> statusMap) {
        // awaits further deliberation on the business logic
        if(statusMap.containsKey(DECOMMISSIONED_KEY)) {
            int decommissioned = statusMap.get(DECOMMISSIONED_KEY);
            return decommissioned > 0 && statusMap.entrySet().stream()
                    .filter(e -> !e.getKey().equals(DECOMMISSIONED_KEY))
                    .noneMatch(e -> e.getValue() > 0);
        }
        return false;
    }
}
