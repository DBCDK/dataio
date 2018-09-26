/*
 * DataIO - Data IO
 *
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.flowstore.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@IdClass(FlowBinderWithSubmitter.class)
@Table(name = FlowBinderWithSubmitter.TABLE_NAME)
@NamedQueries({
    @NamedQuery(name = FlowBinderWithSubmitter.FIND_BY_SUBMITTER,
            query = "SELECT flowBinderWithSubmitter " +
                    "FROM FlowBinderWithSubmitter flowBinderWithSubmitter " +
                    "WHERE flowBinderWithSubmitter.submitterId = :submitterId " +
                    "ORDER BY flowBinderWithSubmitter.flowBinderName")
})
public class FlowBinderWithSubmitter {
    public static final String TABLE_NAME = "flow_binders_with_submitter";
    public static final String FIND_BY_SUBMITTER = "FlowBinderWithSubmitter.findBySubmitter";

    @Id
    @Column(name = "submitter_id")
    private Long submitterId;

    @Id
    @Column(name = "flow_binder_id")
    private Long flowBinderId;

    @Id
    @Column(name = "name_idx")
    private String flowBinderName;

    public Long getSubmitterId() {
        return submitterId;
    }

    public Long getFlowBinderId() {
        return flowBinderId;
    }

    public String getFlowBinderName() {
        return flowBinderName;
    }

    @Override
    public String toString() {
        return "FlowBinderWithSubmitter{" +
                "submitterId=" + submitterId +
                ", flowBinderId=" + flowBinderId +
                ", flowBinderName='" + flowBinderName + '\'' +
                '}';
    }
}
