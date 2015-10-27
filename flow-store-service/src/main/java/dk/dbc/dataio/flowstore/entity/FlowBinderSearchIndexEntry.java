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

package dk.dbc.dataio.flowstore.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Persistence domain class for flow binder search index entries.
 *
 * Note that this entity contains a foreign key relation to its corresponding
 * flow binder.
 */
@Entity
@Table(name = FlowBinderSearchIndexEntry.TABLE_NAME)
@IdClass(FlowBinderSearchKey.class)
public class FlowBinderSearchIndexEntry {

    public static final String TABLE_NAME = "flow_binders_search_index";

    static final String SUBMITTER_NUMBER_COLUMN = "submitter_number";
    static final String FLOW_BINDER_ID_COLUMN = "flow_binder_id";

    // packaging is accessed through java EE, so we will suppress pmd-warnings
    @Id
    @Lob
    @Column(nullable = false)
    private String packaging; // NOPMD

    // format is accessed through java EE, so we will suppress pmd-warnings
    @Id
    @Lob
    @Column(nullable = false)
    private String format; // NOPMD

    // charset is accessed through java EE, so we will suppress pmd-warnings
    @Id
    @Lob
    @Column(nullable = false)
    private String charset; // NOPMD

    // destination is accessed through java EE, so we will suppress pmd-warnings
    @Id
    @Lob
    @Column(nullable = false)
    private String destination; // NOPMD

    // submitter is accessed through java EE, so we will suppress pmd-warnings
    @Id
    @Column(name = SUBMITTER_NUMBER_COLUMN, nullable = false)
    private Long submitter; // NOPMD

    // flowbinder is accessed through java EE, so we will suppress pmd-warnings
    @JoinColumn(name = FLOW_BINDER_ID_COLUMN)
    @OneToOne(fetch = FetchType.LAZY)
    private FlowBinder flowBinder; // NOPMD

    void setFlowBinder(FlowBinder flowBinder) {
        this.flowBinder = flowBinder;
    }

    void setCharset(String charset) {
        this.charset = charset;
    }

    void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDestination() {
        return destination;
    }

    void setFormat(String format) {
        this.format = format;
    }

    void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    void setSubmitter(Long submitter) {
        this.submitter = submitter;
    }
}
