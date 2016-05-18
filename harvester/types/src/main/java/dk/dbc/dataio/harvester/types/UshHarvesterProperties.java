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

package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Date;

/**
 * UshHarvesterProperties DTO class.
 *
 * This class is NOT thread safe.
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class UshHarvesterProperties {

    private int jobId;
    private String name;
    private String scheduleString;
    private Date lastUpdated;
    private Date lastHarvested;
    private String reportedStatus;
    private String latestStatus;
    private String error;

    public int getJobId() {
        return jobId;
    }

    public UshHarvesterProperties withJobId(int jobId) {
        this.jobId = jobId;
        return this;
    }

    public String getName() {
        return name;
    }

    public UshHarvesterProperties withName(String name) {
        this.name = name;
        return this;
    }

    public String getScheduleString() {
        return scheduleString;
    }

    public UshHarvesterProperties withScheduleString(String scheduleString) {
        this.scheduleString = scheduleString;
        return this;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public UshHarvesterProperties withLastUpdatedDate(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }

    public Date getLastHarvested() {
        return lastHarvested;
    }

    public UshHarvesterProperties withLastHarvestedDate(Date lastHarvested) {
        this.lastHarvested = lastHarvested;
        return this;
    }

    public String getReportedStatus() {
        return reportedStatus;
    }

    public UshHarvesterProperties withReportedStatus(String reportedStatus) {
        this.reportedStatus = reportedStatus;
        return this;
    }

    public String getLatestStatus() {
        return latestStatus;
    }

    public UshHarvesterProperties withLatestStatus(String latestStatus) {
        this.latestStatus = latestStatus;
        return this;
    }

    public String getError() {
        return error;
    }

    public UshHarvesterProperties withError(String error) {
        this.error = error;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UshHarvesterProperties)) return false;

        UshHarvesterProperties that = (UshHarvesterProperties) o;

        if (jobId != that.jobId) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (scheduleString != null ? !scheduleString.equals(that.scheduleString) : that.scheduleString != null)
            return false;
        if (lastUpdated != null ? !lastUpdated.equals(that.lastUpdated) : that.lastUpdated != null) return false;
        if (lastHarvested != null ? !lastHarvested.equals(that.lastHarvested) : that.lastHarvested != null)
            return false;
        if (reportedStatus != null ? !reportedStatus.equals(that.reportedStatus) : that.reportedStatus != null)
            return false;
        if (latestStatus != null ? !latestStatus.equals(that.latestStatus) : that.latestStatus != null) return false;
        return error != null ? error.equals(that.error) : that.error == null;

    }

    @Override
    public int hashCode() {
        int result = jobId;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (scheduleString != null ? scheduleString.hashCode() : 0);
        result = 31 * result + (lastUpdated != null ? lastUpdated.hashCode() : 0);
        result = 31 * result + (lastHarvested != null ? lastHarvested.hashCode() : 0);
        result = 31 * result + (reportedStatus != null ? reportedStatus.hashCode() : 0);
        result = 31 * result + (latestStatus != null ? latestStatus.hashCode() : 0);
        result = 31 * result + (error != null ? error.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UshHarvesterProperties{" +
                "jobId=" + jobId +
                ", name=" + name +
                ", scheduleString=" + scheduleString +
                ", lastUpdated=" + lastUpdated +
                ", lastHarvested=" + lastHarvested +
                ", reportedStatus=" + reportedStatus +
                ", latestStatus=" + latestStatus +
                ", error=" + error +
                '}';
    }
}
