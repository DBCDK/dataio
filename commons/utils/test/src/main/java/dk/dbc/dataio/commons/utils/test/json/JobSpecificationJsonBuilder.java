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

package dk.dbc.dataio.commons.utils.test.json;

import dk.dbc.dataio.commons.types.JobSpecification;

public class JobSpecificationJsonBuilder extends JsonBuilder {
    private String packaging = "packaging";
    private String format = "format";
    private String charset = "charset";
    private String destination = "destination";
    private String dataFile = "dataFile";
    private long submitterId = 42L; // Submitter number
    private JobSpecification.Type type = JobSpecification.Type.TEST;

    public JobSpecificationJsonBuilder setCharset(String charset) {
        this.charset = charset;
        return this;
    }

    public JobSpecificationJsonBuilder setDataFile(String dataFile) {
        this.dataFile = dataFile;
        return this;
    }

    public JobSpecificationJsonBuilder setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public JobSpecificationJsonBuilder setFormat(String format) {
        this.format = format;
        return this;
    }

    public JobSpecificationJsonBuilder setPackaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public JobSpecificationJsonBuilder setSubmitterId(long submitterId) {
        this.submitterId = submitterId;
        return this;
    }

    public JobSpecificationJsonBuilder setType(JobSpecification.Type type) {
        this.type = type;
        return this;
    }

    public String build() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(START_OBJECT);
        stringBuilder.append(asTextMember("packaging", packaging)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("format", format)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("charset", charset)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("destination", destination)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("dataFile", dataFile)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("mailForNotificationAboutVerification", "")); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("mailForNotificationAboutProcessing", "")); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("resultmailInitials", ""));stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asLongMember("submitterId", submitterId)); stringBuilder.append(MEMBER_DELIMITER);
        stringBuilder.append(asTextMember("type", type.name()));
        stringBuilder.append(END_OBJECT);
        return stringBuilder.toString();
    }
}
