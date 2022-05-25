package dk.dbc.dataio.gatekeeper.operation;

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.gatekeeper.transfile.TransFile;
import dk.dbc.dataio.jobstore.types.AddNotificationRequest;
import dk.dbc.dataio.jobstore.types.InvalidTransfileNotificationContext;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.invariant.InvariantUtil;

import java.nio.file.Path;

public class CreateInvalidTransfileNotificationOperation implements Operation {
    private static final Opcode OPCODE = Opcode.CREATE_INVALID_TRANSFILE_NOTIFICATION;
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final Path workingDir;
    private final TransFile transfile;
    private final String transfileName;
    private final String causeForInvalidation;

    public CreateInvalidTransfileNotificationOperation(JobStoreServiceConnector jobStoreServiceConnector,
                                                       Path workingDir,
                                                       String transfileName,
                                                       String causeForInvalidation)
            throws NullPointerException, IllegalArgumentException {
        this.jobStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(jobStoreServiceConnector, "jobStoreServiceConnector");
        this.workingDir = InvariantUtil.checkNotNullOrThrow(workingDir, "workingDir");
        this.transfileName = InvariantUtil.checkNotNullNotEmptyOrThrow(transfileName, "transfileName");
        this.causeForInvalidation = InvariantUtil.checkNotNullNotEmptyOrThrow(causeForInvalidation, "causeForInvalidation");
        this.transfile = new TransFile(workingDir.resolve(transfileName));
    }

    @Override
    public Opcode getOpcode() {
        return OPCODE;
    }

    public JobStoreServiceConnector getJobStoreServiceConnector() {
        return jobStoreServiceConnector;
    }

    public Path getWorkingDir() {
        return workingDir;
    }

    public String getTransfileName() {
        return transfileName;
    }

    public String getCauseForInvalidation() {
        return causeForInvalidation;
    }

    @Override
    public void execute() throws OperationExecutionException {
        final InvalidTransfileNotificationContext context = new InvalidTransfileNotificationContext(
                transfileName, transfile.toString(), causeForInvalidation);
        final AddNotificationRequest addNotificationRequest = new AddNotificationRequest(
                getDestinationFromTransfile(), context, Notification.Type.INVALID_TRANSFILE);
        try {
            jobStoreServiceConnector.addNotification(addNotificationRequest);
        } catch (JobStoreServiceConnectorException e) {
            throw new OperationExecutionException(e);
        }
    }

    private String getDestinationFromTransfile() {
        return transfile.getLines().stream()
                .map(this::getDestinationFromTransfileLine)
                .filter(dest -> dest != null)
                .filter(dest -> !dest.isEmpty())
                .findFirst()
                .orElse(Constants.MISSING_FIELD_VALUE);
    }

    private String getDestinationFromTransfileLine(TransFile.Line line) {
        final String destination = line.getField("m");
        return destination != null && !destination.isEmpty() ? destination : line.getField("M");
    }
}
