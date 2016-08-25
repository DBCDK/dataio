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

import dk.dbc.dataio.commons.types.EsSinkConfig;
import dk.dbc.dataio.commons.types.ImsSinkConfig;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.SinkConfig;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.util.Format;

import java.util.List;

public class SinkModel extends GenericBackendModel {

    private SinkContent.SinkType sinkType;
    private String sinkName;
    private String resource;
    private String description;
    private SinkContent.SequenceAnalysisOption sequenceAnalysisOption;
    private SinkConfig sinkConfig;


    /**
     * Empty constructor
     */
    public SinkModel() {
        this(0L, 0L, SinkContent.SinkType.ES, "", "", "", SinkContent.SequenceAnalysisOption.ALL, null);
    }

    /**
     * Open Update Config Sink
     * @param id Sink Id
     * @param version Sink Version
     * @param sinkType Sink Type
     * @param name Sink Name
     * @param resource Sink Resource
     * @param description Sink Description
     * @param sequenceAnalysisOption deciding level of sequence analysis
     * @param sinkConfig Sink Config
     */
    public SinkModel(long id,
                     long version,
                     SinkContent.SinkType sinkType,
                     String name,
                     String resource,
                     String description,
                     SinkContent.SequenceAnalysisOption sequenceAnalysisOption,
                     SinkConfig sinkConfig) {
        super(id, version);
        this.sinkType = sinkType;
        this.sinkName = name;
        this.resource = resource;
        this.description = description == null? "" : description;
        this.sequenceAnalysisOption = sequenceAnalysisOption;
        this.sinkConfig = sinkConfig;
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
        return ((OpenUpdateSinkConfig) sinkConfig).getUserId();
    }

    /**
     * Sets the Open Update Configuration data: User Id
     * @param openUpdateUserId Open Update Configuration data: User Id
     */
    public void setOpenUpdateUserId(String openUpdateUserId) {
        ((OpenUpdateSinkConfig) sinkConfig).withUserId(openUpdateUserId);
    }

    /**
     * Gets the Open Update Configuration data: Password
     * @return Open Update Configuration data: Password
     */
    public String getOpenUpdatePassword() {
        return ((OpenUpdateSinkConfig) sinkConfig).getPassword();
    }

    /**
     * Sets the Open Update Configuration data: Password
     * @param openUpdatePassword Open Update Configuration data: Password
     */
    public void setOpenUpdatePassword(String openUpdatePassword) {
        ((OpenUpdateSinkConfig) sinkConfig).withPassword(openUpdatePassword);
    }

    /**
     * Gets the Open Update Configuration data: Endpoint
     * @return Open Update Configuration data: Endpoint
     */
    public String getOpenUpdateEndpoint() {
        return ((OpenUpdateSinkConfig) sinkConfig).getEndpoint();
    }

    /**
     * Sets the Open Update Configuration data: Endpoint
     * @param openUpdateEndpoint Open Update Configuration data: Endpoint
     */
    public void setOpenUpdateEndpoint(String openUpdateEndpoint) {
        ((OpenUpdateSinkConfig) sinkConfig).withEndpoint(openUpdateEndpoint);
    }

    /**
     * Gets the Open Update Configuration data: List of Available Queue Providers
     * @return Open Update Configuration data: List of Available Queue Providers
     */
    public List<String> getOpenUpdateAvailableQueueProviders() {
        return ((OpenUpdateSinkConfig) sinkConfig).getAvailableQueueProviders();
    }

    /**
     * Sets the Open Update Configuration data: List of Available Queue Providers
     * @param availableQueueProviders Open Update Configuration data: List of Available Queue Providers
     */
    public void setOpenUpdateAvailableQueueProviders(List<String> availableQueueProviders) {
        ((OpenUpdateSinkConfig) sinkConfig).withAvailableQueueProviders(availableQueueProviders);
    }

    /**
     * Gets the ES Configuration data: user id
     * @return ES Configuration data: user id
     */
    public Integer getEsUserId() {
        return ((EsSinkConfig) sinkConfig).getUserId();
    }

    /**
     * Sets the ES Configuration data: user id
     * @param userId ES Configuration data: user id
     */
    public void setEsUserId(Integer userId) {
        ((EsSinkConfig) sinkConfig).withUserId(userId);
    }

    /**
     * Gets the ES Configuration data: database
     * @return ES Configuration data: database
     */
    public String getEsDatabase() {
        return ((EsSinkConfig) sinkConfig).getDatabaseName();
    }

    /**
     * Sets the ES Configuration data: database
     * @param database ES Configuration data: database
     */
    public void setEsDatabase(String database) {
        ((EsSinkConfig) sinkConfig).withDatabaseName(database);
    }

    /**
     * Gets the IMS Configuration data: endpoint
     * @return IMS Configuration data: endpoint
     */
    public String getImsEndpoint() {
        return ((ImsSinkConfig) sinkConfig).getEndpoint();
    }

    /**
     * Sets the IMS Configuration data: endpoint
     * @param endpoint IMS Configuration data: endpoint
     */
    public void setImsEndpoint(String endpoint) {
        ((ImsSinkConfig) sinkConfig).withEndpoint(endpoint);
    }

    /**
     * Gets the Sequence Analysis Option
     * @return Sequence Analysis Option
     */
    public SinkContent.SequenceAnalysisOption getSequenceAnalysisOption() {
        return sequenceAnalysisOption;
    }

    /**
     * Sets the Sequence Analysis Option
     * @param sequenceAnalysisOption the Sequence Analysis Option
     */
    public void setSequenceAnalysisOption(SinkContent.SequenceAnalysisOption sequenceAnalysisOption) {
        this.sequenceAnalysisOption = sequenceAnalysisOption;
    }

    public SinkConfig getSinkConfig() {
        return sinkConfig;
    }

    public void setSinkConfig(SinkConfig sinkConfig) {
        this.sinkConfig = sinkConfig;
    }

    /**
     * Checks for null or empty String values
     * @return true if no null or empty String values were found, otherwise false
     */
    public boolean isInputFieldsEmpty() {
        if(sinkName.isEmpty() || resource.isEmpty() || description.isEmpty()) {
            return true;
        } else {
            switch (sinkType) {
                case OPENUPDATE:
                    final OpenUpdateSinkConfig openUpdateSinkConfig = (OpenUpdateSinkConfig) sinkConfig;
                    return openUpdateSinkConfig.getAvailableQueueProviders() == null
                            || openUpdateSinkConfig.getUserId() == null
                            || openUpdateSinkConfig.getEndpoint() == null
                            || openUpdateSinkConfig.getPassword() == null;
                case ES:
                    final EsSinkConfig esSinkConfig = (EsSinkConfig) sinkConfig;
                    return esSinkConfig.getUserId() == null || esSinkConfig.getDatabaseName() == null;
                default:
                    return false;
            }
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
        if (sequenceAnalysisOption != sinkModel.sequenceAnalysisOption) return false;
        return sinkConfig != null ? sinkConfig.equals(sinkModel.sinkConfig) : sinkModel.sinkConfig == null;

    }

    @Override
    public int hashCode() {
        int result = sinkType != null ? sinkType.hashCode() : 0;
        result = 31 * result + (sinkName != null ? sinkName.hashCode() : 0);
        result = 31 * result + (resource != null ? resource.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (sequenceAnalysisOption != null ? sequenceAnalysisOption.hashCode() : 0);
        result = 31 * result + (sinkConfig != null ? sinkConfig.hashCode() : 0);
        return result;
    }
}
