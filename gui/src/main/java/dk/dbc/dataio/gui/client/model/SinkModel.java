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

package dk.dbc.dataio.gui.client.model;

import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.util.Format;

import java.util.ArrayList;
import java.util.List;

public class SinkModel extends GenericBackendModel {

    private SinkContent.SinkType sinkType;
    private String sinkName;
    private String resource;
    private String description;

    // Open Update Configuration data:
    private String openUpdateUserId;
    private String openUpdatePassword;
    private String openUpdateEndpoint;
    private List<String> openUpdateAvailableQueueProviders;


    /**
     * Empty constructor
     */
    public SinkModel() {
        this(0L, 0L, "", "", "");
    }

    /**
     * Old style Sink (with no SinkType - for backwards compatibility reasons)
     * @param id Sink Id
     * @param version Sink Version
     * @param name Sink Name
     * @param resource Sink Resource
     * @param description Sink Description
     */
    public SinkModel(long id, long version, String name, String resource, String description) {
        this(id, version, SinkContent.SinkType.ES, name, resource, description);
    }

    /**
     * Non Open Update Sink
     * @param id Sink Id
     * @param version Sink Version
     * @param sinkType Sink Type
     * @param name Sink Name
     * @param resource Sink Resource
     * @param description Sink Description
     */
    public SinkModel(long id, long version, SinkContent.SinkType sinkType, String name, String resource, String description) {
        this(id, version, sinkType, name, resource, description, "", "", "", new ArrayList<String>());
    }

    /**
     * Open Update Config Sink
     * @param id Sink Id
     * @param version Sink Version
     * @param sinkType Sink Type
     * @param name Sink Name
     * @param resource Sink Resource
     * @param description Sink Description
     * @param openUpdateUserId Open Update Sink Config User Id
     * @param openUpdatePassword Open Update Sink Config Password
     * @param openUpdateEndpoint Open Update Sink Config Endpoint URL
     * @param openUpdateAvailableQueueProviders Open Update List of Available Queue Providers
     */
    public SinkModel(long id,
                     long version,
                     SinkContent.SinkType sinkType,
                     String name,
                     String resource,
                     String description,
                     String openUpdateUserId,
                     String openUpdatePassword,
                     String openUpdateEndpoint,
                     List<String> openUpdateAvailableQueueProviders) {
        super(id, version);
        this.sinkType = sinkType;
        this.sinkName = name;
        this.resource = resource;
        this.description = description == null? "" : description;
        this.openUpdateUserId = openUpdateUserId;
        this.openUpdatePassword = openUpdatePassword;
        this.openUpdateEndpoint = openUpdateEndpoint;
        this.openUpdateAvailableQueueProviders = openUpdateAvailableQueueProviders;
    }

    /**
     * Gets the Sink Type
     * @return Sink Type
     */
    public SinkContent.SinkType getSinkType() {
        return sinkType;
    }

    /**
     * Sets the Sink Type
     * @param sinkType The Sink Type
     */
    public void setSinkType(SinkContent.SinkType sinkType) {
        this.sinkType = sinkType;
    }

    /**
     * Gets the Resource name
     * @return resourceName
     */
    public String getResourceName() {
        return resource;
    }

    /**
     * Set resource name
     * @param resourceName Resource name
     */
    public void setResourceName(String resourceName) {
        this.resource = resourceName;
    }

    /**
     * Gets the Sink name
     * @return sinkName
     */
    public String getSinkName() {
        return sinkName;
    }

    /**
     * Set sink name
     * @param sinkName Sink name
     */
    public void setSinkName(String sinkName) {
        this.sinkName = sinkName;
    }

    /**
     * Gets the Description
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set sink description
     * @param description Sink description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the Open Update Configuration data: User Id
     * @return Open Update Configuration data: User Id
     */
    public String getOpenUpdateUserId() {
        return openUpdateUserId;
    }

    /**
     * Sets the Open Update Configuration data: User Id
     * @param openUpdateUserId Open Update Configuration data: User Id
     */
    public void setOpenUpdateUserId(String openUpdateUserId) {
        this.openUpdateUserId = openUpdateUserId;
    }

    /**
     * Gets the Open Update Configuration data: Password
     * @return Open Update Configuration data: Password
     */
    public String getOpenUpdatePassword() {
        return openUpdatePassword;
    }

    /**
     * Sets the Open Update Configuration data: Password
     * @param openUpdatePassword Open Update Configuration data: Password
     */
    public void setOpenUpdatePassword(String openUpdatePassword) {
        this.openUpdatePassword = openUpdatePassword;
    }

    /**
     * Gets the Open Update Configuration data: Endpoint
     * @return Open Update Configuration data: Endpoint
     */
    public String getOpenUpdateEndpoint() {
        return openUpdateEndpoint;
    }

    /**
     * Sets the Open Update Configuration data: Endpoint
     * @param openUpdateEndpoint Open Update Configuration data: Endpoint
     */
    public void setOpenUpdateEndpoint(String openUpdateEndpoint) {
        this.openUpdateEndpoint = openUpdateEndpoint;
    }

    /**
     * Gets the Open Update Configuration data: List of Available Queue Providers
     * @return Open Update Configuration data: List of Available Queue Providers
     */
    public List<String> getOpenUpdateAvailableQueueProviders() {
        return openUpdateAvailableQueueProviders;
    }

    /**
     * Sets the Open Update Configuration data: List of Available Queue Providers
     * @param availableQueueProviders Open Update Configuration data: List of Available Queue Providers
     */
    public void setOpenUpdateAvailableQueueProviders(List<String> availableQueueProviders) {
        this.openUpdateAvailableQueueProviders = availableQueueProviders;
    }

    /**
     * Checks for empty String values
     * NB: The list of Available Queue Providers is optional, and is therefore not considered here
     * @return true if no empty String values were found, otherwise false
     */
    public boolean isInputFieldsEmpty() {
        if (sinkType == SinkContent.SinkType.OPENUPDATE) {
            return sinkName.isEmpty() || resource.isEmpty() || description.isEmpty()
                    || openUpdateUserId.isEmpty() || openUpdatePassword.isEmpty() || openUpdateEndpoint.isEmpty();
        } else {
            return sinkName.isEmpty() || resource.isEmpty() || description.isEmpty();
        }
    }

    /**
     * Checks if the sink name contains illegal characters.
     * A-Å, 0-9, - (minus), + (plus), _ (underscore) and space is valid
     * @return a list containing illegal characters found. Empty list if none found.
     */
    public List<String> getDataioPatternMatches() {
        return Format.getDataioPatternMatches(sinkName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SinkModel)) return false;

        SinkModel sinkModel = (SinkModel) o;

        if (sinkType != sinkModel.sinkType) return false;
        if (sinkName != null ? !sinkName.equals(sinkModel.sinkName) : sinkModel.sinkName != null) return false;
        if (resource != null ? !resource.equals(sinkModel.resource) : sinkModel.resource != null) return false;
        if (description != null ? !description.equals(sinkModel.description) : sinkModel.description != null)
            return false;
        if (openUpdateUserId != null ? !openUpdateUserId.equals(sinkModel.openUpdateUserId) : sinkModel.openUpdateUserId != null)
            return false;
        if (openUpdatePassword != null ? !openUpdatePassword.equals(sinkModel.openUpdatePassword) : sinkModel.openUpdatePassword != null)
            return false;
        if (openUpdateEndpoint != null ? !openUpdateEndpoint.equals(sinkModel.openUpdateEndpoint) : sinkModel.openUpdateEndpoint != null)
            return false;
        return openUpdateAvailableQueueProviders != null ? openUpdateAvailableQueueProviders.equals(sinkModel.openUpdateAvailableQueueProviders) : sinkModel.openUpdateAvailableQueueProviders == null;

    }

    @Override
    public int hashCode() {
        int result = sinkType != null ? sinkType.hashCode() : 0;
        result = 31 * result + (sinkName != null ? sinkName.hashCode() : 0);
        result = 31 * result + (resource != null ? resource.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (openUpdateUserId != null ? openUpdateUserId.hashCode() : 0);
        result = 31 * result + (openUpdatePassword != null ? openUpdatePassword.hashCode() : 0);
        result = 31 * result + (openUpdateEndpoint != null ? openUpdateEndpoint.hashCode() : 0);
        result = 31 * result + (openUpdateAvailableQueueProviders != null ? openUpdateAvailableQueueProviders.hashCode() : 0);
        return result;
    }
}
