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

package dk.dbc.dataio.jobstore.service.param;

import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.partitioner.AddiDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DanMarc2LineFormatDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DanMarc2LineFormatReorderingDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.DefaultXmlDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.Iso2709DataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.Iso2709ReorderingDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.JobItemReorderer;
import dk.dbc.dataio.jobstore.service.partitioner.MarcXchangeAddiDataPartitioner;
import dk.dbc.dataio.jobstore.service.partitioner.RawRepoMarcXmlDataPartitioner;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserDefaultKeyGenerator;
import dk.dbc.dataio.sequenceanalyser.keygenerator.SequenceAnalyserKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.ws.rs.ProcessingException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static dk.dbc.dataio.commons.types.RecordSplitterConstants.RecordSplitter;

/**
 * This class is a parameter abstraction for the PgJobStore.addJob() method.
 * <p>
 * Parameter initialization failures will result in fatal diagnostics being added
 * to the internal diagnostics list, and the corresponding parameter field being
 * given a null value.
 * </p>
 */
public class PartitioningParam {
    private static final Logger LOGGER = LoggerFactory.getLogger(PartitioningParam.class);

    protected List<Diagnostic> diagnostics = new ArrayList<>();
    protected InputStream dataFileInputStream;
    protected DataPartitioner dataPartitioner;

    private final FileStoreServiceConnector fileStoreServiceConnector;
    private EntityManager entityManager;
    private JobEntity jobEntity;
    private String dataFileId;
    private SequenceAnalyserKeyGenerator sequenceAnalyserKeyGenerator;
    private RecordSplitter recordSplitterType;

    public PartitioningParam(
            JobEntity jobEntity,
            FileStoreServiceConnector fileStoreServiceConnector,
            EntityManager entityManager,
            RecordSplitter recordSplitterType) throws NullPointerException {
        this.fileStoreServiceConnector = InvariantUtil.checkNotNullOrThrow(fileStoreServiceConnector, "fileStoreServiceConnector");
        this.jobEntity = InvariantUtil.checkNotNullOrThrow(jobEntity, "jobEntity");
        if (!this.jobEntity.hasFatalError()) {
            this.entityManager = InvariantUtil.checkNotNullOrThrow(entityManager, "entityManager");
            this.recordSplitterType = InvariantUtil.checkNotNullOrThrow(recordSplitterType, "recordSplitterType");
            this.sequenceAnalyserKeyGenerator = new SequenceAnalyserDefaultKeyGenerator(jobEntity.getSpecification().getSubmitterId());
            this.dataFileId = extractDataFileIdFromURN();
            this.dataFileInputStream = newDataFileInputStream();
            this.dataPartitioner = newDataPartitioner();
        }
    }

    public String getDataFileId() {
        return dataFileId;
    }

    public InputStream getDataFileInputStream() {
        return dataFileInputStream;
    }

    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

    public JobEntity getJobEntity() {
        return this.jobEntity;
    }

    public DataPartitioner getDataPartitioner() {
        return dataPartitioner;
    }

    public RecordSplitter getRecordSplitterType() {
        return recordSplitterType;
    }

    public SequenceAnalyserKeyGenerator getSequenceAnalyserKeyGenerator() {
        return sequenceAnalyserKeyGenerator;
    }

    public void closeDataFile() {
        if (dataFileInputStream != null) {
            try {
                dataFileInputStream.close();
            } catch (IOException e) {
                LOGGER.error("Unable to close datafile input stream", e);
            }
        }
    }

    private InputStream newDataFileInputStream() {
        if (dataFileId != null && !dataFileId.isEmpty()) {
            try {
                return fileStoreServiceConnector.getFile(dataFileId);
            } catch (FileStoreServiceConnectorException | ProcessingException e) {
                final String message = String.format("Could not get input stream for data file: %s", jobEntity.getSpecification().getDataFile());
                diagnostics.add(ObjectFactory.buildFatalDiagnostic(message, e));
            }
        }
        return null;
    }

    private DataPartitioner newDataPartitioner() {
        if (dataFileInputStream != null) {
            switch (recordSplitterType) {
                case XML:
                    return DefaultXmlDataPartitioner.newInstance(dataFileInputStream, jobEntity.getSpecification().getCharset());
                case ISO2709:
                    return getIso2709Partitioner();
                case DANMARC2_LINE_FORMAT:
                    return getDanMarc2LineFormatPartitioner();
                case RR_MARC_XML:
                    return RawRepoMarcXmlDataPartitioner.newInstance(dataFileInputStream, jobEntity.getSpecification().getCharset());
                case ADDI_MARC_XML:
                    return MarcXchangeAddiDataPartitioner.newInstance(dataFileInputStream, jobEntity.getSpecification().getCharset());
                case ADDI:
                    return AddiDataPartitioner.newInstance(dataFileInputStream, jobEntity.getSpecification().getCharset());
                default:
                    diagnostics.add(ObjectFactory.buildFatalDiagnostic("unknown data partitioner: " + recordSplitterType));
            }
        }
        return null;
    }

    private String extractDataFileIdFromURN() {
        final String dataFileURN = jobEntity.getSpecification().getDataFile();
        if(!Files.exists(Paths.get(dataFileURN))) {
            try {
                return new FileStoreUrn(dataFileURN).getFileId();
            } catch (URISyntaxException e) {
                final String message = String.format("Invalid file-store service URN: %s", dataFileURN);
                diagnostics.add(ObjectFactory.buildFatalDiagnostic(message, e));
            }
        }
        return null;
    }

    private DataPartitioner getDanMarc2LineFormatPartitioner() {
        final String encoding = jobEntity.getSpecification().getCharset();
        if (shouldBeReordered()) {
            final JobItemReorderer reorderer = new JobItemReorderer(jobEntity.getId(), entityManager);
            return DanMarc2LineFormatReorderingDataPartitioner.newInstance(dataFileInputStream, encoding, reorderer);
        }
        return DanMarc2LineFormatDataPartitioner.newInstance(dataFileInputStream, encoding);
    }

    private DataPartitioner getIso2709Partitioner() {
        final String encoding = jobEntity.getSpecification().getCharset();
        if(shouldBeReordered()) {
            final JobItemReorderer reorderer = new JobItemReorderer(jobEntity.getId(), entityManager);
            return Iso2709ReorderingDataPartitioner.newInstance(dataFileInputStream, encoding, reorderer);
        }
        return Iso2709DataPartitioner.newInstance(dataFileInputStream, encoding);
    }

    private boolean shouldBeReordered() {
        final JobSpecification.Ancestry ancestry = jobEntity.getSpecification().getAncestry();
        // Items originating from FTP server must undergo potential re-ordering
        return ancestry != null && ancestry.getTransfile() != null;
    }
}