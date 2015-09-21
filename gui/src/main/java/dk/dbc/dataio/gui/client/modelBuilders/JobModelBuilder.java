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

package dk.dbc.dataio.gui.client.modelBuilders;

import dk.dbc.dataio.gui.client.model.DiagnosticModel;
import dk.dbc.dataio.gui.client.model.JobModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JobModelBuilder {

    private String jobCreationTime = "2015-08-13 14:56:11";
    private String jobCompletionTime = "";
    private String jobId = "32";
    private String submitterNumber = "536278";
    private String submitterName = "Submitter name";
    private String flowBinderName = "Flowbinder Name";
    private long sinkId = 4L;
    private String sinkName = "Sink name";
    private boolean jobDone = true;
    private long itemCounter = 10L;
    private long failedCounter = 0L;
    private long ignoredCounter = 0L;
    private long processingIgnoredCounter = 0L;
    private long partitionedCounter = 14;
    private long processedCounter = 15;
    private long deliveredCounter = 16;
    private List<DiagnosticModel> diagnosticModels = new ArrayList<DiagnosticModel>(Collections.singletonList(
            new DiagnosticModelBuilder().build()));
    private boolean diagnosticFatal = false;
    private String packaging = "-packaging-";
    private String format = "-format-";
    private String charset = "-charset-";
    private String destination = "-destination-";
    private String mailForNotificationAboutVerification = "-mailForNotificationAboutVerification-";
    private String mailForNotificationAboutProcessing = "-mailForNotificationAboutProcessing-";
    private String resultMailInitials = "-resultmailInitials-";
    private JobModel.Type type = JobModel.Type.TRANSIENT;

    public JobModelBuilder setJobCreationTime(String jobCreationTime) {
        this.jobCreationTime = jobCreationTime;
        return this;
    }

    public JobModelBuilder setJobCompletionTime(String jobCompletionTime) {
        this.jobCompletionTime = jobCompletionTime;
        return this;
    }

    public JobModelBuilder setJobId(String jobId) {
        this.jobId = jobId;
        return this;
    }

    public JobModelBuilder setSubmitterNumber(String submitterNumber) {
        this.submitterNumber = submitterNumber;
        return this;
    }

    public JobModelBuilder setSubmitterName(String submitterName) {
        this.submitterName = submitterName;
        return this;
    }

    public JobModelBuilder setFlowBinderName(String flowBinderName) {
        this.flowBinderName = flowBinderName;
        return this;
    }

    public JobModelBuilder setSinkId(long sinkId) {
        this.sinkId = sinkId;
        return this;
    }

    public JobModelBuilder setSinkName(String sinkName) {
        this.sinkName = sinkName;
        return this;
    }

    public JobModelBuilder setIsJobDone(boolean jobDone) {
        this.jobDone = jobDone;
        return this;
    }

    public JobModelBuilder setItemCounter(long itemCounter) {
        this.itemCounter = itemCounter;
        return this;
    }

    public JobModelBuilder setFailedCounter(long failedCounter) {
        this.failedCounter = failedCounter;
        return this;
    }

    public JobModelBuilder setIgnoredCounter(long ignoredCounter) {
        this.ignoredCounter = ignoredCounter;
        return this;
    }

    public JobModelBuilder setProcessingIgnoredCounter(long processingIgnoredCounter) {
        this.processingIgnoredCounter = processingIgnoredCounter;
        return this;
    }

    public JobModelBuilder setPartitionedCounter(long partitionedCounter) {
        this.partitionedCounter = partitionedCounter;
        return this;
    }

    public JobModelBuilder setProcessedCounter(long processedCounter) {
        this.processedCounter = processedCounter;
        return this;
    }

    public JobModelBuilder setDeliveredCounter(long deliveredCounter) {
        this.deliveredCounter = deliveredCounter;
        return this;
    }

    public JobModelBuilder setDiagnosticModels(List<DiagnosticModel> diagnosticModels) {
        this.diagnosticModels = new ArrayList<DiagnosticModel>(diagnosticModels);
        return this;
    }

    public JobModelBuilder setHasDiagnosticFatal(boolean diagnosticFatal) {
        this.diagnosticFatal = diagnosticFatal;
        return this;
    }

    public JobModelBuilder setPackaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public JobModelBuilder setFormat(String format) {
        this.format = format;
        return this;
    }

    public JobModelBuilder setCharset(String charset) {
        this.charset = charset;
        return this;
    }

    public JobModelBuilder setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public JobModelBuilder setMailForNotificationAboutVerification(String mailForNotificationAboutVerification) {
        this.mailForNotificationAboutVerification = mailForNotificationAboutVerification;
        return this;
    }

    public JobModelBuilder setMailForNotificationAboutProcessing(String mailForNotificationAboutProcessing) {
        this.mailForNotificationAboutProcessing = mailForNotificationAboutProcessing;
        return this;
    }

    public JobModelBuilder setResultMailInitials(String resultMailInitials) {
        this.resultMailInitials = resultMailInitials;
        return this;
    }

    public JobModelBuilder setType(JobModel.Type type) {
        this.type = type;
        return this;
    }

    public JobModel build() {
        return new JobModel(
                jobCreationTime,
                jobCompletionTime,
                jobId,
                submitterNumber,
                submitterName,
                flowBinderName,
                sinkId,
                sinkName,
                jobDone,
                itemCounter,
                failedCounter,
                ignoredCounter,
                processingIgnoredCounter,
                partitionedCounter,
                processedCounter,
                deliveredCounter,
                diagnosticModels,
                diagnosticFatal,
                packaging,
                format,
                charset,
                destination,
                mailForNotificationAboutVerification,
                mailForNotificationAboutProcessing,
                resultMailInitials,
                type);
    }
}
