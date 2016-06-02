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

package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.JobSpecification;


public class JobSpecificationBuilder {
    private String packaging = "-packaging-";
    private String format = "-format-";
    private String charset = "-charset-";
    private String destination = "-destination-";
    private long submitterId = 222;
    private String mailForNotificationAboutVerification = "";
    private String mailForNotificationAboutProcessing = "";
    private String resultmailInitials = "";
    private String dataFile = "-dataFile-";
    private JobSpecification.Type type = JobSpecification.Type.TEST;
    private JobSpecification.Ancestry ancestry = null;

    public JobSpecificationBuilder setPackaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public JobSpecificationBuilder setFormat(String format) {
        this.format = format;
        return this;
    }

    public JobSpecificationBuilder setCharset(String charset) {
        this.charset = charset;
        return this;
    }

    public JobSpecificationBuilder setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public JobSpecificationBuilder setSubmitterId(long submitterId) {
        this.submitterId = submitterId;
        return this;
    }

    public JobSpecificationBuilder setMailForNotificationAboutVerification(String mailForNotificationAboutVerification) {
        this.mailForNotificationAboutVerification = mailForNotificationAboutVerification;
        return this;
    }

    public JobSpecificationBuilder setMailForNotificationAboutProcessing(String mailForNotificationAboutProcessing) {
        this.mailForNotificationAboutProcessing = mailForNotificationAboutProcessing;
        return this;
    }

    public JobSpecificationBuilder setResultmailInitials(String resultmailInitials) {
        this.resultmailInitials = resultmailInitials;
        return this;
    }

    public JobSpecificationBuilder setDataFile(String dataFile) {
        this.dataFile = dataFile;
        return this;
    }

    public JobSpecificationBuilder setType(JobSpecification.Type type) {
        this.type = type;
        return this;
    }

    public JobSpecificationBuilder setAncestry(JobSpecification.Ancestry ancestry) {
        this.ancestry = ancestry;
        return this;
    }

    public JobSpecification build() {
        return new JobSpecification(packaging, format, charset, destination, submitterId,
                mailForNotificationAboutVerification, mailForNotificationAboutProcessing, resultmailInitials,
                dataFile, type, ancestry);
    }
}
