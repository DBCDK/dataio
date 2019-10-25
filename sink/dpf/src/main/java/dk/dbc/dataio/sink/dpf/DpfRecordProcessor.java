/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.dpf;

import dk.dbc.dataio.sink.dpf.model.DpfRecord;
import dk.dbc.jsonb.JSONBException;
import dk.dbc.lobby.LobbyConnectorException;

import java.util.ArrayList;
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

        if (this.dpfRecords.get(0).hasErrors()) {
            for (DpfRecord dpfRecord : dpfRecords) {
                needsManualProcessing(dpfRecord);
            }
            return eventLog;
        }
        return eventLog;
    }

    private void reset(List<DpfRecord> dpfRecords) {
        this.dpfRecords = dpfRecords;
        this.eventLog = new ArrayList<>();
    }

    private void needsManualProcessing(DpfRecord dpfRecord) throws DpfRecordProcessorException {
        try {
            serviceBroker.sendToLobby(dpfRecord);
            eventLog.add(new Event(dpfRecord.getId(), Event.Type.SENT_TO_LOBBY));
        } catch (LobbyConnectorException | JSONBException e) {
            throw new DpfRecordProcessorException(
                    "Unable to send DPF record '" + dpfRecord.getId() + "' to lobby", e);
        }
    }

    static class Event {
        private final String dpfRecordId;
        private final String suffix;
        private final Type type;

        public enum Type {
            SENT_TO_LOBBY("Sent to lobby");

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
                    + (suffix == null ? "" :  " " + suffix);
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
