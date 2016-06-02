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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.Date;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class UshHarvesterProperties {

    @JacksonXmlProperty(isAttribute = true)
    private String uri;

    private int amountHarvested;
    private String currentStatus;
    private boolean enabled;
    private int id;
    private String jobClass;
    private Date lastHarvestFinished;
    private Date lastHarvestStarted;
    private Date lastUpdated;
    private String message;
    private String name;
    private Date nextHarvestSchedule;
    private String storageUrl;

    public String getUri() {
        return uri;
    }

    public UshHarvesterProperties withUri(String uri) {
        this.uri = uri;
        return this;
    }

    public int getAmountHarvested() {
        return amountHarvested;
    }

    public UshHarvesterProperties withAmountHarvested(int amountHarvested) {
        this.amountHarvested = amountHarvested;
        return this;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public UshHarvesterProperties withCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
        return this;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public UshHarvesterProperties withEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public int getId() {
        return id;
    }

    public UshHarvesterProperties withId(int id) {
        this.id = id;
        return this;
    }

    public String getJobClass() {
        return jobClass;
    }

    public UshHarvesterProperties withJobClass(String jobClass) {
        this.jobClass = jobClass;
        return this;
    }

    public Date getLastHarvestFinished() {
        return lastHarvestFinished;
    }

    public UshHarvesterProperties withLastHarvestFinishedDate(Date lastHarvestFinished) {
        this.lastHarvestFinished = lastHarvestFinished;
        return this;
    }

    public Date getLastHarvestStarted() {
        return lastHarvestStarted;
    }

    public UshHarvesterProperties withLastHarvestStartedDate(Date lastHarvestStarted) {
        this.lastHarvestStarted = lastHarvestStarted;
        return this;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public UshHarvesterProperties withLastUpdatedDate(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public UshHarvesterProperties withMessage(String message) {
        this.message = message;
        return this;
    }

    public String getName() {
        return name;
    }

    public UshHarvesterProperties withName(String name) {
        this.name = name;
        return this;
    }

    public Date getNextHarvestSchedule() {
        return nextHarvestSchedule;
    }

    public UshHarvesterProperties withNextHarvestSchedule(Date nextHarvestSchedule) {
        this.nextHarvestSchedule = nextHarvestSchedule;
        return this;
    }

    public String getStorageUrl() {
        return storageUrl;
    }

    public UshHarvesterProperties withStorageUrl(String storageUrl) {
        this.storageUrl = storageUrl;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UshHarvesterProperties)) return false;

        UshHarvesterProperties that = (UshHarvesterProperties) o;

        if (amountHarvested != that.amountHarvested) return false;
        if (enabled != that.enabled) return false;
        if (id != that.id) return false;
        if (uri != null ? !uri.equals(that.uri) : that.uri != null) return false;
        if (currentStatus != null ? !currentStatus.equals(that.currentStatus) : that.currentStatus != null)
            return false;
        if (jobClass != null ? !jobClass.equals(that.jobClass) : that.jobClass != null) return false;
        if (lastHarvestFinished != null ? !lastHarvestFinished.equals(that.lastHarvestFinished) : that.lastHarvestFinished != null)
            return false;
        if (lastHarvestStarted != null ? !lastHarvestStarted.equals(that.lastHarvestStarted) : that.lastHarvestStarted != null)
            return false;
        if (lastUpdated != null ? !lastUpdated.equals(that.lastUpdated) : that.lastUpdated != null) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (nextHarvestSchedule != null ? !nextHarvestSchedule.equals(that.nextHarvestSchedule) : that.nextHarvestSchedule != null)
            return false;
        return storageUrl != null ? storageUrl.equals(that.storageUrl) : that.storageUrl == null;

    }

    @Override
    public int hashCode() {
        int result = uri != null ? uri.hashCode() : 0;
        result = 31 * result + amountHarvested;
        result = 31 * result + (currentStatus != null ? currentStatus.hashCode() : 0);
        result = 31 * result + (enabled ? 1 : 0);
        result = 31 * result + id;
        result = 31 * result + (jobClass != null ? jobClass.hashCode() : 0);
        result = 31 * result + (lastHarvestFinished != null ? lastHarvestFinished.hashCode() : 0);
        result = 31 * result + (lastHarvestStarted != null ? lastHarvestStarted.hashCode() : 0);
        result = 31 * result + (lastUpdated != null ? lastUpdated.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (nextHarvestSchedule != null ? nextHarvestSchedule.hashCode() : 0);
        result = 31 * result + (storageUrl != null ? storageUrl.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UshHarvesterProperties{" +
                "uri='" + uri + '\'' +
                ", amountHarvested=" + amountHarvested +
                ", currentStatus='" + currentStatus + '\'' +
                ", enabled=" + enabled +
                ", id=" + id +
                ", jobClass='" + jobClass + '\'' +
                ", lastHarvestFinished=" + lastHarvestFinished +
                ", lastHarvestStarted=" + lastHarvestStarted +
                ", lastUpdated=" + lastUpdated +
                ", message='" + message + '\'' +
                ", name='" + name + '\'' +
                ", nextHarvestSchedule=" + nextHarvestSchedule +
                ", storageUrl='" + storageUrl + '\'' +
                '}';
    }
}
