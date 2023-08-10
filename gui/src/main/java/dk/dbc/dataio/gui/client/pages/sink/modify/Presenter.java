package dk.dbc.dataio.gui.client.pages.sink.modify;

import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

import java.util.List;
import java.util.Set;

public interface Presenter extends GenericPresenter {
    void sinkTypeChanged(SinkContent.SinkType sinkType);

    void nameChanged(String name);

    void queueChanged(String name);

    void descriptionChanged(String description);

    void timeoutChanged(String timeout);

    void openUpdateUserIdChanged(String userId);

    void passwordChanged(String password);

    void queueProvidersChanged(List<String> value);

    void updateServiceIgnoredValidationErrorsChanged(Set<String> values);

    void updateServiceIgnoredValidationErrorsAddButtonPressed();

    void updateServiceIgnoredValidationErrorsRemoveButtonPressed(String value);

    void endpointChanged(String endpoint);

    void esUserIdChanged(String userId);

    void esDatabaseChanged(String esDatabase);

    void imsEndpointChanged(String imsEndpoint);

    void worldCatUserIdChanged(String userId);

    void worldCatPasswordChanged(String password);

    void worldCatProjectIdChanged(String projectId);

    void worldCatEndpointChanged(String endpoint);

    void worldCatRetryDiagnosticsChanged(List<String> values);

    void vipEndpointChanged(String imsEndpoint);

    void keyPressed();

    void saveButtonPressed();

    void deleteButtonPressed();

    void queueProvidersAddButtonPressed();

    void worldCatRetryDiagnosticsAddButtonPressed();

    void worldCatRetryDiagnosticRemoveButtonPressed(String retryDiagnostic);

    void sequenceAnalysisSelectionChanged(String value);

    void dpfUpdateServiceUserIdChanged(String userId);

    void dpfUpdateServicePasswordChanged(String password);

    void dpfUpdateServiceQueueProvidersChanged(List<String> queueProviders);

    void dpfUpdateServiceQueueProvidersAddButtonPressed();
}
