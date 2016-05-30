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

package dk.dbc.dataio.commons.utils.ush.bindings;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import dk.dbc.dataio.harvester.types.UshHarvesterProperties;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

import static java.util.Locale.ENGLISH;

public class UshHarvesterJob {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", ENGLISH);

    @JacksonXmlProperty(isAttribute = true)
    private int id;

    private String name;
    private String scheduleString;
    private String lastUpdated;
    private String lastHarvested;
    private String reportedStatus;
    private String latestStatus;
    private String error;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScheduleString() {
        return scheduleString;
    }

    public void setScheduleString(String scheduleString) {
        this.scheduleString = scheduleString;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getLastHarvested() {
        return lastHarvested;
    }

    public void setLastHarvested(String lastHarvested) {
        this.lastHarvested = lastHarvested;
    }

    public String getReportedStatus() {
        return reportedStatus;
    }

    public void setReportedStatus(String reportedStatus) {
        this.reportedStatus = reportedStatus;
    }

    public String getLatestStatus() {
        return latestStatus;
    }

    public void setLatestStatus(String latestStatus) {
        this.latestStatus = latestStatus;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "UshHarvesterJob{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", scheduleString='" + scheduleString + '\'' +
                ", lastUpdated='" + lastUpdated + '\'' +
                ", lastHarvested='" + lastHarvested + '\'' +
                ", reportedStatus='" + reportedStatus + '\'' +
                ", latestStatus='" + latestStatus + '\'' +
                ", error='" + error + '\'' +
                '}';
    }

    /**
     * Maps this UshHarvesterJob to new instance of UshHarvesterProperties
     * @return ushHarvesterProperties
     */
    public UshHarvesterProperties toUshHarvesterProperties() {
        return new UshHarvesterProperties()
                .withJobId(id)
                .withName(name)
                .withLastUpdatedDate(toDate(lastUpdated))
                .withLastHarvestedDate(toDate(lastHarvested))
                .withScheduleString(scheduleString)
                .withLatestStatus(latestStatus)
                .withReportedStatus(reportedStatus)
                .withError(error);
    }

    /**
     * Converts the UshHarvesterJob's String representation to java.util.Date
     * @param dateString, String representation of the date
     * @return UshHarvesterJob.new Date representation of the String date, null if the String date is null
     */
    public static Date toDate(String dateString) throws DateTimeParseException {
        Date date = null;
        if(dateString != null) {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateString, formatter);
            date = Date.from(zonedDateTime.toInstant());
        }
        return date;
    }
}
