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
import dk.dbc.rawrepo.RecordServiceConnectorException;
import dk.dbc.updateservice.UpdateServiceDoubleRecordCheckConnectorException;

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

        final DpfRecord dpfRecord = dpfRecords.get(0);
        if (dpfRecord.hasErrors()) {
            sendToLobby();
            return eventLog;
        }

        final DpfRecord.State recordState = dpfRecord.getProcessingInstructions().getRecordState();
        if (recordState == DpfRecord.State.NEW) {
            processAsNew();
        } else if (recordState == DpfRecord.State.MODIFIED) {
            processAsModified();
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

        executeDoubleRecordCheck(dpfRecord);
        if (dpfRecord.hasErrors()) {
            sendToLobby();
            return;
        }

        // TODO Get new faust

        // TODO get new week code

        // TODO Send to update

        if (dpfRecords.size() == 2) {
            handleDBCHeadRecord();
        }
    }

    private void processAsModified() throws DpfRecordProcessorException {
        final DpfRecord dpfRecord = dpfRecords.get(0);
        final RawrepoRecord rawrepoRecord;

        try {
            rawrepoRecord = serviceBroker.getMarcRecord(dpfRecord.getBibliographicRecordId(), 870970);
        } catch (RecordServiceConnectorException | MarcReaderException ex) {
            throw new DpfRecordProcessorException("Exception when loading marc record", ex);
        }

        if (rawrepoRecord == null) {
            dpfRecord.addError("notfound");
            eventLog.add(new Event(dpfRecord.getId(), Event.Type.NOT_FOUND));
            return;
        }

        if (!dpfRecord.getPeriodicaType().equals(rawrepoRecord.getPeriodicaType())) {
            dpfRecord.addError("periodicatype");
            eventLog.add(new Event(dpfRecord.getId(), Event.Type.DIFFERENT_PERIODICA_TYPE));
            return;
        }

        String dpfCode = dpfRecord.getDPFCode();

        if (!"".equals(dpfCode)) {
            if (Arrays.asList("DPF", "GPG", "FPF").contains(dpfCode)) {
                dpfRecord.setNatBibCode(rawrepoRecord.getNatBibCode());
            }
        }

        // TODO Send to update

        if (dpfRecords.size() == 2) {
            handleDBCHeadRecord();
        }
    }

    private void handleDBCHeadRecord() {
        DpfRecord headRecord = dpfRecords.get(1);

        // TODO Get new faust if missing

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
            IS_DOUBLE_RECORD("Is a double record"),
            SENT_TO_LOBBY("Sent to lobby"),
            DIFFERENT_PERIODICA_TYPE("Periodica type is changed"),
            NOT_FOUND("Existing DBC record could not be loaded (it doesn't exist?)");

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
