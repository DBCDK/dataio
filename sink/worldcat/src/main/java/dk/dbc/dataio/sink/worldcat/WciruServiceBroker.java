package dk.dbc.dataio.sink.worldcat;

import dk.dbc.dataio.commons.utils.lang.JaxpUtil;
import dk.dbc.oclc.wciru.Diagnostic;
import dk.dbc.oclc.wciru.DiagnosticsType;
import dk.dbc.oclc.wciru.OcnMismatchException;
import dk.dbc.oclc.wciru.OperationStatusType;
import dk.dbc.oclc.wciru.UpdateResponseType;
import dk.dbc.oclc.wciru.WciruServiceConnector;
import dk.dbc.oclc.wciru.WciruServiceConnectorException;
import dk.dbc.ocnrepo.dto.WorldCatEntity;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WciruServiceBroker {
    public static final String PRIMARY_HOLDING_SYMBOL = "DKDLA";

    private final WciruServiceConnector wciruServiceConnector;

    public WciruServiceBroker(WciruServiceConnector wciruServiceConnector) {
        this.wciruServiceConnector = wciruServiceConnector;
    }

    /**
     * Pushes record contained in given chunk item to WorldCat via WCIRU service
     * @param chunkItemWithWorldCatAttributes chunk item enriched with WorldCat attributes
     * @param worldCatEntity cached WorldCat metadata
     * @return broker result
     */
    public Result push(ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes, WorldCatEntity worldCatEntity) {
        if (isWciruDelete(chunkItemWithWorldCatAttributes)) {
            return wciruDeleteRecord(chunkItemWithWorldCatAttributes, worldCatEntity);
        }
        return wciruAddOrUpdateRecord(chunkItemWithWorldCatAttributes, worldCatEntity);
    }

    /**
     * Determines if record is to be deleted
     * @param chunkItemWithWorldCatAttributes chunk item enriched with WorldCat attributes
     * @return true if all holdings have action delete, otherwise false
     */
    private boolean isWciruDelete(ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes) {
        final List<Holding> holdings = chunkItemWithWorldCatAttributes.getWorldCatAttributes().getHoldings();
        return holdings.size() == holdings.stream()
                .filter(holding -> holding.getAction() == Holding.Action.DELETE)
                .count();
    }

    /**
     * Deletes record and associated holdings
     * @param chunkItemWithWorldCatAttributes chunk item enriched with WorldCat attributes
     * @param worldCatEntity cached WorldCat metadata
     * @return {@link Result}
     */
    private Result wciruDeleteRecord(ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes, WorldCatEntity worldCatEntity) {
        final Result result = new Result()
                .withOcn(worldCatEntity.getOcn());

        if (result.getOcn() == null || result.getOcn().isEmpty()) {
            // It makes no sense to try and delete a record if we don't have its OCN number
            final Diagnostic diagnostic = new Diagnostic();
            diagnostic.setMessage("missing OCN number");
            result.withEvents(new Event()
                    .withAction(Event.Action.DELETE)
                    .withDiagnostics(diagnostic));
            return result;
        }

        try {
            final Element recordContent = toRequestElement(chunkItemWithWorldCatAttributes);

            // First delete primary holding symbol
            UpdateResponseType replaceResponse = result.replaceRecord(result.getOcn(), recordContent, new Holding()
                    .withSymbol(PRIMARY_HOLDING_SYMBOL)
                    .withAction(Holding.Action.DELETE));

            // ... then delete remaining holding
            result.withOcn(getOcnFromServiceResponse(replaceResponse));
            for (Holding holding : chunkItemWithWorldCatAttributes.getWorldCatAttributes().getHoldings()) {
                if (PRIMARY_HOLDING_SYMBOL.equals(holding.getSymbol())) {
                    continue;
                }
                replaceResponse = result.replaceRecord(result.getOcn(), recordContent, holding);
                verifyOcnFromServiceResponse(replaceResponse, result.getOcn());
            }

            // ... then delete the WorldCat record using WCIRU "Unlink"
            final UpdateResponseType deleteResponse = result.deleteRecord(result.getOcn(), recordContent);
            verifyOcnFromServiceResponse(deleteResponse, result.getOcn());

        } catch (WciruServiceConnectorException | OcnMismatchException | IllegalArgumentException e) {
            result.withException(e);
            if (e instanceof WciruServiceConnectorException) {
                result.getLastEvent().withDiagnostics(((WciruServiceConnectorException) e).getDiagnostic());
            }
        }

        return result;
    }

    /**
     * Adds or updates record and associated holdings
     * @param chunkItemWithWorldCatAttributes chunk item enriched with WorldCat attributes
     * @param worldCatEntity cached WorldCat metadata
     * @return {@link Result}
     */
    private Result wciruAddOrUpdateRecord(ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes, WorldCatEntity worldCatEntity) {
        final Result result = new Result()
                .withOcn(worldCatEntity.getOcn());

        try {
            final Element recordContent = toRequestElement(chunkItemWithWorldCatAttributes);

            // First create or update WorldCat record
            final UpdateResponseType addOrUpdateResponse = result.addOrUpdateRecord(result.getOcn(), recordContent, new Holding()
                    .withSymbol(PRIMARY_HOLDING_SYMBOL)
                    .withAction(Holding.Action.INSERT));

            // ... then create or update remaining holdings
            result.withOcn(getOcnFromServiceResponse(addOrUpdateResponse));
            for (Holding holding : chunkItemWithWorldCatAttributes.getWorldCatAttributes().getHoldings()) {
                if (PRIMARY_HOLDING_SYMBOL.equals(holding.getSymbol())) {
                    continue;
                }
                final UpdateResponseType replaceResponse = result.replaceRecord(result.getOcn(), recordContent, holding);
                verifyOcnFromServiceResponse(replaceResponse, result.getOcn());
            }
        } catch (WciruServiceConnectorException | OcnMismatchException | IllegalArgumentException e) {
            result.withException(e);
            if (e instanceof WciruServiceConnectorException) {
                result.getLastEvent().withDiagnostics(((WciruServiceConnectorException) e).getDiagnostic());
            }
        }

        return result;
    }

    /**
     * Extracts WorldCat identifier (OCN) from WCIRU service response
     * @param response WCIRU service response
     * @return WorldCat identifier
     * @throws IllegalStateException if no OCN was returned by WCIRU service
     */
    private String getOcnFromServiceResponse(UpdateResponseType response) throws IllegalStateException {
        final String ocn = response.getRecordIdentifier();
        if (ocn == null || ocn.isEmpty()) {
            throw new IllegalStateException("no OCN returned by WCIRU service");
        }
        return ocn;
    }

    /**
     * Extracts diagnostics from WCIRU service response
     * @param response WCIRU service response
     * @return list of diagnostics
     */
    private List<Diagnostic> getDiagnosticsFromServiceResponse(UpdateResponseType response) {
        // WCIRU can return diagnostics even for successful requests,
        final DiagnosticsType diagnosticsType = response.getDiagnostics();
        if (diagnosticsType != null) {
            return diagnosticsType.getDiagnostic();
        }
        return Collections.emptyList();
    }

    /**
     * Verifies that given OCN matches the one in the given WCIRU service response
     * @param response WCIRU service response
     * @param expectedOcn expected WorldCat identifier
     * @throws OcnMismatchException on mismatch
     */
    private void verifyOcnFromServiceResponse(UpdateResponseType response, String expectedOcn) throws OcnMismatchException {
        // A FAIL response may be given when the error was
        // suppressed by the connector.
        if (response.getOperationStatus() != OperationStatusType.FAIL) {
            final String ocn = getOcnFromServiceResponse(response);
            if (!expectedOcn.equals(ocn)) {
                throw new OcnMismatchException(
                        String.format("expected OCN '%s' got '%s'", expectedOcn, ocn));
            }
        }
    }

    /**
     * Transforms chunk item data into WCIRU service payload
     * @param chunkItemWithWorldCatAttributes chunk item enriched with WorldCat attributes
     * @return WCIRU service payload
     * @throws IllegalArgumentException on failure to transform data
     */
    private Element toRequestElement(ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes)
            throws IllegalArgumentException {
        try {
            final String normalizedData = UnicodeNormalizationFormDecomposed.of(new String(
                    chunkItemWithWorldCatAttributes.getData(),
                    chunkItemWithWorldCatAttributes.getEncoding()));
            return JaxpUtil.parseDocument(normalizedData).getDocumentElement();
        } catch (IOException | SAXException | RuntimeException e) {
            throw new IllegalArgumentException("Error transforming data", e);
        }
    }

    public static class Event {
        public enum Action {
            ADD_OR_UPDATE,
            DELETE,
            REPLACE
        }

        private Holding holding;
        private Action action;
        private List<Diagnostic> diagnostics;

        public Event() {
            diagnostics = new ArrayList<>();
        }

        public Holding getHolding() {
            return holding;
        }

        public Event withHolding(Holding holding) {
            this.holding = holding;
            return this;
        }

        public Action getAction() {
            return action;
        }

        public Event withAction(Action action) {
            this.action = action;
            return this;
        }

        public List<Diagnostic> getDiagnostics() {
            return diagnostics;
        }

        public Event withDiagnostics(List<Diagnostic> diagnostics) {
            this.diagnostics.addAll(diagnostics);
            return this;
        }

        public Event withDiagnostics(Diagnostic... diagnostics) {
            this.diagnostics.addAll(Arrays.asList(diagnostics));
            return this;
        }
    }

    /**
     * {@link WciruServiceBroker} result with {@link Event} audit trail
     */
    public class Result {
        private String ocn;
        private List<Event> events;
        private Exception exception;

        public Result() {
            events = new ArrayList<>();
        }

        public String getOcn() {
            return ocn;
        }

        public Result withOcn(String ocn) {
            this.ocn = ocn;
            return this;
        }

        public List<Event> getEvents() {
            return events;
        }

        public Result withEvents(List<Event> events) {
            this.events.addAll(events);
            return this;
        }

        public Result withEvents(Event... events) {
            this.events.addAll(Arrays.asList(events));
            return this;
        }

        public Event getLastEvent() {
            if (!events.isEmpty()) {
                return events.get(events.size() - 1);
            }
            return null;
        }

        public Exception getException() {
            return exception;
        }

        public Result withException(Exception exception) {
            this.exception = exception;
            return this;
        }

        public boolean isFailed() {
            return exception != null;
        }

        /**
         * Replaces record in WorldCat
         * @param ocn WorldCat record identifier
         * @param recordContent record content
         * @param holding holding symbol and action
         * @return WCIRU service response
         * @throws WciruServiceConnectorException on WCIRU service failure
         */
        private UpdateResponseType replaceRecord(String ocn, Element recordContent, Holding holding)
                throws WciruServiceConnectorException {

            final Event replaceEvent = new Event()
                    .withAction(Event.Action.REPLACE)
                    .withHolding(holding);
            events.add(replaceEvent);

            final UpdateResponseType response = wciruServiceConnector.replaceRecord(recordContent, ocn,
                    holding.getSymbol(), holding.getAction().getWciruValue());
            replaceEvent.withDiagnostics(getDiagnosticsFromServiceResponse(response));

            return response;
        }

        /**
         * Unlinks record in WorldCat
         * @param ocn WorldCat record identifier
         * @param recordContent record content
         * @return WCIRU service response
         * @throws WciruServiceConnectorException on WCIRU service failure
         */
        private UpdateResponseType deleteRecord(String ocn, Element recordContent)
                throws WciruServiceConnectorException {
            final Event deleteEvent = new Event().withAction(Event.Action.DELETE);

            final UpdateResponseType response = wciruServiceConnector.deleteRecord(recordContent, ocn);
            deleteEvent.withDiagnostics(getDiagnosticsFromServiceResponse(response));

            return response;
        }

        /**
         * Creates new or updates existing record in WorldCat
         * @param ocn WorldCat record identifier
         * @param recordContent record content
         * @param holding holding symbol
         * @return WCIRU service response
         * @throws WciruServiceConnectorException on WCIRU service failure
         */
        private UpdateResponseType addOrUpdateRecord(String ocn, Element recordContent, Holding holding)
                throws WciruServiceConnectorException {
            final Event addOrUpdateEvent = new Event()
                    .withAction(Event.Action.ADD_OR_UPDATE)
                    .withHolding(holding);
            events.add(addOrUpdateEvent);

            final UpdateResponseType response = wciruServiceConnector.addOrUpdateRecord(recordContent,
                    holding.getSymbol(), ocn);
            addOrUpdateEvent.withDiagnostics(getDiagnosticsFromServiceResponse(response));

            return response;
        }
    }
}
