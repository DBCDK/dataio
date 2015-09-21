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

import dk.dbc.dataio.sink.es.entity.EsInFlight;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Stateless
public class EsInFlightBean {
    @EJB
    EsSinkConfigurationBean configuration;

    @PersistenceContext(unitName = "esInFlightPU")
    EntityManager entityManager;

    public void addEsInFlight(EsInFlight esInFlight) {
        entityManager.persist(esInFlight);
    }

    public void removeEsInFlight(EsInFlight esInFlight) {
        entityManager.remove(esInFlight);
    }

    public List<EsInFlight> listEsInFlight() {
        final TypedQuery<EsInFlight> query = entityManager.createNamedQuery(EsInFlight.FIND_ALL, EsInFlight.class);
        query.setParameter(EsInFlight.QUERY_PARAMETER_RESOURCENAME, configuration.getEsResourceName());
        return query.getResultList();
    }
}
