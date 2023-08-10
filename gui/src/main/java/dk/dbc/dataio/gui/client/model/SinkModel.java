package dk.dbc.dataio.gui.client.model;

import dk.dbc.dataio.commons.types.DpfSinkConfig;
import dk.dbc.dataio.commons.types.EsSinkConfig;
import dk.dbc.dataio.commons.types.ImsSinkConfig;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.SinkConfig;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.VipSinkConfig;
import dk.dbc.dataio.commons.types.WorldCatSinkConfig;
import dk.dbc.dataio.gui.client.util.Format;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SinkModel extends GenericBackendModel {

    private SinkContent.SinkType sinkType;
    private String sinkName;
    private String queue;
    private String description;
    private SinkContent.SequenceAnalysisOption sequenceAnalysisOption;
    private SinkConfig sinkConfig;
    private int timeout;

    public SinkModel() {
        this(0L, 0L, SinkContent.SinkType.ES, "", "", "", SinkContent.SequenceAnalysisOption.ALL, null, 1);
    }

    public SinkModel(long id,
                     long version,
                     SinkContent.SinkType sinkType,
                     String name,
                     String queue,
                     String description,
                     SinkContent.SequenceAnalysisOption sequenceAnalysisOption,
                     SinkConfig sinkConfig,
                     int timeout) {
        super(id, version);
        this.sinkType = sinkType;
        sinkName = name;
        this.queue = queue;
        this.description = description == null ? "" : description;
        this.sequenceAnalysisOption = sequenceAnalysisOption;
        this.sinkConfig = sinkConfig;
        this.timeout = timeout;
    }

    /**
     * Gets the Sink Type
     *
     * @return Sink Type
     */
    public SinkContent.SinkType getSinkType() {
        return sinkType;
    }

    /**
     * Sets the Sink Type
     *
     * @param sinkType The Sink Type
     */
    public void setSinkType(SinkContent.SinkType sinkType) {
        this.sinkType = sinkType;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    /**
     * Gets the Sink name
     *
     * @return sinkName
     */
    public String getSinkName() {
        return sinkName;
    }

    /**
     * Set sink name
     *
     * @param sinkName Sink name
     */
    public void setSinkName(String sinkName) {
        this.sinkName = sinkName;
    }

    /**
     * Gets the Description
     *
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set sink description
     *
     * @param description Sink description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the Open Update Configuration data: User Id
     *
     * @return Open Update Configuration data: User Id
     */
    public String getOpenUpdateUserId() {
        return ((OpenUpdateSinkConfig) sinkConfig).getUserId();
    }

    /**
     * Sets the Open Update Configuration data: User Id
     *
     * @param openUpdateUserId Open Update Configuration data: User Id
     */
    public void setOpenUpdateUserId(String openUpdateUserId) {
        ((OpenUpdateSinkConfig) sinkConfig).withUserId(openUpdateUserId);
    }

    /**
     * Gets the Open Update Configuration data: Password
     *
     * @return Open Update Configuration data: Password
     */
    public String getOpenUpdatePassword() {
        return ((OpenUpdateSinkConfig) sinkConfig).getPassword();
    }

    /**
     * Sets the Open Update Configuration data: Password
     *
     * @param openUpdatePassword Open Update Configuration data: Password
     */
    public void setOpenUpdatePassword(String openUpdatePassword) {
        ((OpenUpdateSinkConfig) sinkConfig).withPassword(openUpdatePassword);
    }

    /**
     * Gets the Open Update Configuration data: Endpoint
     *
     * @return Open Update Configuration data: Endpoint
     */
    public String getOpenUpdateEndpoint() {
        return ((OpenUpdateSinkConfig) sinkConfig).getEndpoint();
    }

    /**
     * Sets the Open Update Configuration data: Endpoint
     *
     * @param openUpdateEndpoint Open Update Configuration data: Endpoint
     */
    public void setOpenUpdateEndpoint(String openUpdateEndpoint) {
        ((OpenUpdateSinkConfig) sinkConfig).withEndpoint(openUpdateEndpoint);
    }

    /**
     * Gets the Open Update Configuration data: List of Available Queue Providers
     *
     * @return Open Update Configuration data: List of Available Queue Providers
     */
    public List<String> getOpenUpdateAvailableQueueProviders() {
        return ((OpenUpdateSinkConfig) sinkConfig).getAvailableQueueProviders();
    }

    /**
     * Sets the Open Update Configuration data: List of Available Queue Providers
     *
     * @param availableQueueProviders Open Update Configuration data: List of Available Queue Providers
     */
    public void setOpenUpdateAvailableQueueProviders(List<String> availableQueueProviders) {
        ((OpenUpdateSinkConfig) sinkConfig).withAvailableQueueProviders(availableQueueProviders);
    }

    public Set<String> getUpdateServiceIgnoredValidationErrors() {
        return ((OpenUpdateSinkConfig) sinkConfig).getIgnoredValidationErrors();
    }

    public void setUpdateServiceIgnoredValidationErrors(Set<String> errors) {
        ((OpenUpdateSinkConfig) sinkConfig).withIgnoredValidationErrors(errors);
    }

    public String getDpfUpdateServiceUserId() {
        return ((DpfSinkConfig) sinkConfig).getUpdateServiceUserId();
    }

    public void setDpfUpdateServiceUserId(String updateServiceUserId) {
        ((DpfSinkConfig) sinkConfig).withUpdateServiceUserId(updateServiceUserId);
    }

    public String getDpfUpdateServicePassword() {
        return ((DpfSinkConfig) sinkConfig).getUpdateServicePassword();
    }

    public void setDpfUpdateServicePassword(String updateServicePassword) {
        ((DpfSinkConfig) sinkConfig).withUpdateServicePassword(updateServicePassword);
    }

    public List<String> getDpfUpdateServiceAvailableQueueProviders() {
        return ((DpfSinkConfig) sinkConfig).getUpdateServiceAvailableQueueProviders();
    }

    public void setDpfUpdateServiceAvailableQueueProviders(List<String> updateServiceAvailableQueueProviders) {
        ((DpfSinkConfig) sinkConfig).withUpdateServiceAvailableQueueProviders(updateServiceAvailableQueueProviders);
    }

    public int getTimeout() {
        return timeout;
    }

    public SinkModel setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Gets the ES Configuration data: user id
     *
     * @return ES Configuration data: user id
     */
    public Integer getEsUserId() {
        return ((EsSinkConfig) sinkConfig).getUserId();
    }

    /**
     * Sets the ES Configuration data: user id
     *
     * @param userId ES Configuration data: user id
     */
    public void setEsUserId(Integer userId) {
        ((EsSinkConfig) sinkConfig).withUserId(userId);
    }

    /**
     * Gets the ES Configuration data: database
     *
     * @return ES Configuration data: database
     */
    public String getEsDatabase() {
        return ((EsSinkConfig) sinkConfig).getDatabaseName();
    }

    /**
     * Sets the ES Configuration data: database
     *
     * @param database ES Configuration data: database
     */
    public void setEsDatabase(String database) {
        ((EsSinkConfig) sinkConfig).withDatabaseName(database);
    }

    /**
     * Gets the IMS Configuration data: endpoint
     *
     * @return IMS Configuration data: endpoint
     */
    public String getImsEndpoint() {
        return ((ImsSinkConfig) sinkConfig).getEndpoint();
    }

    /**
     * Sets the WorldCat Configuration data: userId
     *
     * @param userId WorldCat Configuration data: userId
     */
    public void setWordCatUserId(String userId) {
        ((WorldCatSinkConfig) sinkConfig).withUserId(userId);
    }

    /**
     * Gets the WorldCat Configuration data: userId
     *
     * @return WorldCat Configuration data: userId
     */
    public String getWorldCatUserId() {
        return ((WorldCatSinkConfig) sinkConfig).getUserId();
    }

    /**
     * Sets the WorldCat Configuration data: password
     *
     * @param password WorldCat Configuration data: password
     */
    public void setWordCatPassword(String password) {
        ((WorldCatSinkConfig) sinkConfig).withPassword(password);
    }

    /**
     * Gets the WorldCat Configuration data: password
     *
     * @return WorldCat Configuration data: password
     */
    public String getWorldCatPassword() {
        return ((WorldCatSinkConfig) sinkConfig).getPassword();
    }


    /**
     * Sets the WorldCat Configuration data: projectId
     *
     * @param projectId WorldCat Configuration data: projectId
     */
    public void setWordCatProjectId(String projectId) {
        ((WorldCatSinkConfig) sinkConfig).withProjectId(projectId);
    }

    /**
     * Gets the WorldCat Configuration data: projectId
     *
     * @return WorldCat Configuration data: projectId
     */
    public String getWorldCatProjectId() {
        return ((WorldCatSinkConfig) sinkConfig).getProjectId();
    }

    /**
     * Sets the WorldCat Configuration data: endpoint
     *
     * @param endpoint WorldCat Configuration data: endpoint
     */
    public void setWorldCatEndpoint(String endpoint) {
        ((WorldCatSinkConfig) sinkConfig).withEndpoint(endpoint);
    }

    /**
     * Gets the WorldCat Configuration data: endpoint
     *
     * @return WorldCat Configuration data: endpoint
     */
    public String getWorldCatEndpoint() {
        return ((WorldCatSinkConfig) sinkConfig).getEndpoint();
    }


    /**
     * Gets the WorldCat Configuration data: List of Retry diagnostics
     *
     * @return WorldCat Configuration data: List of Retry diagnostics
     */
    public List<String> getWorldCatRetryDiagnostics() {
        return ((WorldCatSinkConfig) sinkConfig).getRetryDiagnostics();
    }

    /**
     * Sets the WorldCat Configuration data: List of Retry diagnostics
     *
     * @param retryDiagnostics WorldCat Configuration data: List of Retry diagnostics
     */
    public void setWorldCatRetryDiagnostics(List<String> retryDiagnostics) {
        ((WorldCatSinkConfig) sinkConfig).withRetryDiagnostics(retryDiagnostics);
    }

    /**
     * Sets the IMS Configuration data: endpoint
     *
     * @param endpoint IMS Configuration data: endpoint
     */
    public void setImsEndpoint(String endpoint) {
        ((ImsSinkConfig) sinkConfig).withEndpoint(endpoint);
    }

    public String getVipEndpoint() {
        return ((VipSinkConfig) sinkConfig).getEndpoint();
    }

    public void setVipEndpoint(String endpoint) {
        ((VipSinkConfig) sinkConfig).withEndpoint(endpoint);
    }

    /**
     * Gets the Sequence Analysis Option
     *
     * @return Sequence Analysis Option
     */
    public SinkContent.SequenceAnalysisOption getSequenceAnalysisOption() {
        return sequenceAnalysisOption;
    }

    /**
     * Sets the Sequence Analysis Option
     *
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
     *
     * @return true if no null or empty String values were found, otherwise false
     */
    public boolean isInputFieldsEmpty() {
        if (sinkName.isEmpty() || description.isEmpty()) {
            return true;
        } else {
            switch (sinkType) {
                case DPF:
                    final DpfSinkConfig dpfSinkConfig = (DpfSinkConfig) sinkConfig;
                    return dpfSinkConfig.getUpdateServiceAvailableQueueProviders() == null
                            || dpfSinkConfig.getUpdateServiceUserId() == null
                            || dpfSinkConfig.getUpdateServicePassword() == null;
                case OPENUPDATE:
                    final OpenUpdateSinkConfig openUpdateSinkConfig = (OpenUpdateSinkConfig) sinkConfig;
                    return openUpdateSinkConfig.getAvailableQueueProviders() == null
                            || openUpdateSinkConfig.getUserId() == null
                            || openUpdateSinkConfig.getEndpoint() == null
                            || openUpdateSinkConfig.getPassword() == null;
                case ES:
                    final EsSinkConfig esSinkConfig = (EsSinkConfig) sinkConfig;
                    return esSinkConfig.getUserId() == null || esSinkConfig.getDatabaseName() == null;
                case IMS:
                    final ImsSinkConfig imsSinkConfig = (ImsSinkConfig) sinkConfig;
                    return imsSinkConfig.getEndpoint() == null;
                case WORLDCAT:
                    final WorldCatSinkConfig worldCatSinkConfig = (WorldCatSinkConfig) sinkConfig;
                    return worldCatSinkConfig.getUserId() == null
                            || worldCatSinkConfig.getPassword() == null
                            || worldCatSinkConfig.getProjectId() == null
                            || worldCatSinkConfig.getEndpoint() == null
                            || worldCatSinkConfig.getRetryDiagnostics() == null;
                case VIP:
                    return ((VipSinkConfig) sinkConfig).getEndpoint() == null;
                default:
                    return false;
            }
        }
    }

    /**
     * Checks if the sink name contains illegal characters.
     * A-Å, 0-9, - (minus), + (plus), _ (underscore) and space is valid
     *
     * @return a list containing illegal characters found. Empty list if none found.
     */
    public List<String> getDataioPatternMatches() {
        return Format.getDataioPatternMatches(sinkName);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        SinkModel sinkModel = (SinkModel) object;
        return sinkType == sinkModel.sinkType && Objects.equals(sinkName, sinkModel.sinkName) && Objects.equals(queue, sinkModel.queue) && Objects.equals(description, sinkModel.description) && sequenceAnalysisOption == sinkModel.sequenceAnalysisOption && Objects.equals(sinkConfig, sinkModel.sinkConfig) && Objects.equals(timeout, sinkModel.timeout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sinkType, sinkName, queue, description, sequenceAnalysisOption, sinkConfig, timeout);
    }
}
