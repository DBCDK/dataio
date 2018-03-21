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

package dk.dbc.dataio.gatekeeper.operation;

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.gatekeeper.transfile.TransFile;
import dk.dbc.dataio.jobstore.types.AddNotificationRequest;
import dk.dbc.dataio.jobstore.types.InvalidTransfileNotificationContext;
import dk.dbc.dataio.jobstore.types.Notification;

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
