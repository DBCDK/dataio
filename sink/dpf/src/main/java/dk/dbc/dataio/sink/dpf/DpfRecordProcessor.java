/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.dpf;

import dk.dbc.dataio.sink.dpf.model.DpfRecord;
import dk.dbc.dataio.sink.dpf.model.RawrepoRecord;
import dk.dbc.jsonb.JSONBException;
import dk.dbc.lobby.LobbyConnectorException;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.opennumberroll.OpennumberRollConnectorException;
import dk.dbc.rawrepo.RecordServiceConnectorException;
import dk.dbc.updateservice.UpdateServiceDoubleRecordCheckConnectorException;
import dk.dbc.weekresolver.WeekresolverConnectorException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

class DpfRecordProcessor {
    private final ServiceBroker serviceBroker;
    private List<DpfRecord> dpfRecords;
    private List<Event> eventLog;

    DpfRecordProcessor(ServiceBroker serviceBroker) {
        this.serviceBroker = serviceBroker;
    }

    List<Event> process(List<DpfRecord> dpfRecords) throws DpfRecordProcessorException {
        reset(dpfRecords);

        for (DpfRecord dpfRecord : dpfRecords) {
            if (dpfRecord.hasErrors()) {
                sendToLobby();
                return eventLog;
            }
        }

        // All DpfRecords have same recordState so we just look at the first one
        final DpfRecord.State recordState = dpfRecords.get(0).getProcessingInstructions().getRecordState();
        if (recordState == DpfRecord.State.NEW) {
            processAsNew();
        } else if (recordState == DpfRecord.State.MODIFIED) {
            processAsModified();
        }

        for (DpfRecord dpfRecord : dpfRecords) {
            if (dpfRecord.hasErrors()) {
                sendToLobby();
                return eventLog;
            }
        }

        return eventLog;
    }

    private void reset(List<DpfRecord> dpfRecords) {
        this.dpfRecords = dpfRecords;
        this.eventLog = new ArrayList<>();
    }

    private void sendToLobby() throws DpfRecordProcessorException {
        for (DpfRecord dpfRecord : dpfRecords) {
            sendToLobby(dpfRecord);
        }
    }

    private void sendToLobby(DpfRecord dpfRecord) throws DpfRecordProcessorException {
        try {
            serviceBroker.sendToLobby(dpfRecord);
            eventLog.add(new Event(dpfRecord.getId(), Event.Type.SENT_TO_LOBBY));
        } catch (LobbyConnectorException | JSONBException e) {
            throw new DpfRecordProcessorException(
                    "Unable to send DPF record '" + dpfRecord.getId() + "' to lobby", e);
        }
    }

    private void processAsNew() throws DpfRecordProcessorException {
        final DpfRecord dpfRecord = dpfRecords.get(0);
        eventLog.add(new Event(dpfRecord.getId(), Event.Type.PROCESS_AS_NEW));

        executeDoubleRecordCheck(dpfRecord);
        if (dpfRecord.hasErrors()) {
            return;
        }

        final String bibliographicRecordId = getNewBibliographicRecordId(dpfRecord);
        dpfRecord.setBibliographicRecordId(bibliographicRecordId);
        dpfRecord.addSystemControlNumber("(DK-870970)" + bibliographicRecordId);

        final String catalogueCode = getCatalogueCode(dpfRecord);
        handleCatalogueCode(dpfRecord, catalogueCode);

        if (dpfRecords.size() == 2) {
            final DpfRecord dpfHead = dpfRecords.get(1);
            eventLog.add(new Event(dpfHead.getId(), Event.Type.PROCESS_HEAD));

            final String bibliographicRecordIdHead = getNewBibliographicRecordId(dpfHead);
            dpfHead.setBibliographicRecordId(bibliographicRecordIdHead);

            // Head volume should have a reference to the bind volume (not a parent/child relation!)
            dpfHead.setOtherBibliographicRecordId(bibliographicRecordId);

            dpfRecord.addSystemControlNumber("(DPFHOVED)" + bibliographicRecordIdHead);
        }

        // TODO Send to update
    }

    private void processAsModified() throws DpfRecordProcessorException {
        final DpfRecord dpfRecord = dpfRecords.get(0);
        final RawrepoRecord rawrepoRecord;
        final RawrepoRecord rawrepoHeadRecord;
        eventLog.add(new Event(dpfRecord.getId(), Event.Type.PROCESS_AS_MODIFIED));

        rawrepoRecord = getRawrepoRecord(dpfRecord, dpfRecord.getBibliographicRecordId(), 870970);
        if (rawrepoRecord == null) {
            dpfRecord.addError("notfound");
            eventLog.add(new Event(dpfRecord.getId(), Event.Type.NOT_FOUND, dpfRecord.getBibliographicRecordId() + ":870970"));
            return;
        }

        if (!dpfRecord.getPeriodicaType().equals(rawrepoRecord.getPeriodicaType())) {
            dpfRecord.addError("periodicatype");
            eventLog.add(new Event(dpfRecord.getId(), Event.Type.DIFFERENT_PERIODICA_TYPE));
            return;
        }

        final String catalogueCode = rawrepoRecord.getCatalogueCode();
        handleCatalogueCode(dpfRecord, catalogueCode);

        // Handle head DPF record
        if ("z".equals(dpfRecord.getPeriodicaType())) {
            final DpfRecord dpfHead = dpfRecords.get(1);
            eventLog.add(new Event(dpfHead.getId(), Event.Type.PROCESS_HEAD));

            rawrepoHeadRecord = getRawrepoRecord(dpfRecord, dpfRecord.getDPFHeadBibliographicRecordId(), 870970);
            if (rawrepoHeadRecord == null) {
                dpfHead.addError("headnotfound");
                eventLog.add(new Event(dpfHead.getId(), Event.Type.NOT_FOUND, dpfRecord.getDPFHeadBibliographicRecordId() + ":870970"));
                return;
            }

            if (rawrepoHeadRecord.getOtherBibliographicRecordId() == null ||
                    !rawrepoHeadRecord.getOtherBibliographicRecordId().equals(dpfRecord.getBibliographicRecordId())) {
                dpfHead.addError("headreferencemismatch");
                eventLog.add(new Event(dpfHead.getId(), Event.Type.DPF_REFERENCE_MISMATCH));
                return;
            }

        }

        // TODO Send to update
    }

    private void executeDoubleRecordCheck(DpfRecord dpfRecord) throws DpfRecordProcessorException {
        try {
            eventLog.add(new Event(dpfRecord.getId(), Event.Type.SENT_TO_DOUBLE_RECORD_CHECK));
            if (serviceBroker.isDoubleRecord(dpfRecord)) {
                addError("dobbeltpost");
                eventLog.add(new Event(dpfRecord.getId(), Event.Type.IS_DOUBLE_RECORD));
            }
        } catch (BibliographicRecordFactoryException | UpdateServiceDoubleRecordCheckConnectorException e) {
            throw new DpfRecordProcessorException(
                    "Unable to execute double record check for DPF record " + dpfRecord.getId(), e);
        }
    }

    private String getCatalogueCode(DpfRecord dpfRecord) throws DpfRecordProcessorException {
        try {
            final String catalogueCode = serviceBroker.getCatalogueCode(dpfRecord.getDPFCode());

            eventLog.add(new Event(dpfRecord.getId(), Event.Type.NEW_CATALOGUE_CODE, catalogueCode));

            return catalogueCode;
        } catch (WeekresolverConnectorException e) {
            throw new DpfRecordProcessorException(
                    "Unable to get catalogue code for DPF record " + dpfRecord.getId(), e);
        }
    }

    private RawrepoRecord getRawrepoRecord(DpfRecord dpfRecord, String bibliographicRecordId, int agencyId) throws DpfRecordProcessorException {
        try {
            if (!serviceBroker.rawrepoRecordExists(bibliographicRecordId, agencyId)) {
                return null;
            }
        } catch (RecordServiceConnectorException e) {
            throw new DpfRecordProcessorException(
                    "Unexpected exception during rawrepoRecordExists", e);
        }

        try {
            return serviceBroker.getRawrepoRecord(dpfRecord.getBibliographicRecordId(), agencyId);
        } catch (RecordServiceConnectorException | MarcReaderException e) {
            throw new DpfRecordProcessorException(
                    "Unexpected exception during getRawrepoRecord", e);
        }
    }

    private void handleCatalogueCode(DpfRecord dpfRecord, String catalogueCode) {
        final String dpfCode = dpfRecord.getDPFCode();

        if (!"".equals(dpfCode) && Arrays.asList("DPF", "GPG", "FPF").contains(dpfCode)) {
            dpfRecord.setCatalogueCode(catalogueCode);
            dpfRecord.removeDPFCode();
        }
    }

    private String getNewBibliographicRecordId(DpfRecord dpfRecord) throws DpfRecordProcessorException {
        try {
            final String newFaust = serviceBroker.getNewFaust();

            eventLog.add(new Event(dpfRecord.getId(), Event.Type.NEW_FAUST, newFaust));

            return newFaust;
        } catch (OpennumberRollConnectorException e) {
            throw new DpfRecordProcessorException(
                    "Unable to get faust for DPF record " + dpfRecord.getId(), e);
        }
    }

    private void addError(String errorMessage) {
        for (DpfRecord dpfRecord : dpfRecords) {
            dpfRecord.addError(errorMessage);
        }
    }

    static class Event {
        private final String dpfRecordId;
        private final String suffix;
        private final Type type;

        public enum Type {
            SENT_TO_DOUBLE_RECORD_CHECK("Sent to double record check"),
            NEW_CATALOGUE_CODE("Got new catalogue code"),
            NEW_FAUST("Got new faust from opennumberroll"),
            PROCESS_AS_NEW("New DPF record"),
            PROCESS_AS_MODIFIED("Modified DPF record"),
            PROCESS_HEAD("Processing DPF head record"),
            IS_DOUBLE_RECORD("Is a double record"),
            SENT_TO_LOBBY("Sent to lobby"),
            DIFFERENT_PERIODICA_TYPE("Periodica type is changed"),
            NOT_FOUND("Record was not found"),
            DPF_REFERENCE_MISMATCH("The DPF record reference (035 *a) in DPF head doesn't match referencing (018 *a) DPF record");

            private final String displayMessage;

            Type(String displayMessage) {
                this.displayMessage = displayMessage;
            }

            public String getDisplayMessage() {
                return displayMessage;
            }
        }

        Event(String dpfRecordId, Event.Type type) {
            this.dpfRecordId = dpfRecordId;
            this.suffix = null;
            this.type = type;
        }

        Event(String dpfRecordId, Event.Type type, String suffix) {
            this.dpfRecordId = dpfRecordId;
            this.suffix = suffix;
            this.type = type;
        }

        @Override
        public String toString() {
            return dpfRecordId + ": " + type.getDisplayMessage()
                    + (suffix == null ? "" : " " + suffix);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Event event = (Event) o;
            if (!Objects.equals(dpfRecordId, event.dpfRecordId)) {
                return false;
            }
            if (!Objects.equals(suffix, event.suffix)) {
                return false;
            }
            return type == event.type;
        }

        @Override
        public int hashCode() {
            int result = dpfRecordId != null ? dpfRecordId.hashCode() : 0;
            result = 31 * result + (suffix != null ? suffix.hashCode() : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
            return result;
        }
    }
}
