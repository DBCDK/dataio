package dk.dbc.dataio.sink.worldcat;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.JaxpUtil;
import dk.dbc.oclc.wciru.Diagnostic;
import dk.dbc.oclc.wciru.DiagnosticsType;
import dk.dbc.oclc.wciru.OcnMismatchException;
import dk.dbc.oclc.wciru.UpdateResponseType;
import dk.dbc.oclc.wciru.WciruServiceConnector;
import dk.dbc.oclc.wciru.WciruServiceConnectorException;
import dk.dbc.ocnrepo.dto.WorldCatEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class WciruServiceBroker {
    public final static String PRIMARY_HOLDING_SYMBOL = "DKDLA";
    private static final Logger log = LoggerFactory.getLogger(WciruServiceBroker.class);
    private final WciruServiceConnector wciruServiceConnector;
    private final StringBuilder stringBuilder;

    public WciruServiceBroker(WciruServiceConnector wciruServiceConnector) {
        this.wciruServiceConnector = wciruServiceConnector;
        this.stringBuilder = new StringBuilder();
    }

    public ChunkItem push(ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes, WorldCatEntity worldCatEntity) {
        try {
            if (chunkItemWithWorldCatAttributes.getChecksum() == worldCatEntity.getChecksum()) {
                return chunkItemWithWorldCatAttributes.withStatus(ChunkItem.Status.IGNORE);
            } else {
                return pushTransformedItem(chunkItemWithWorldCatAttributes, worldCatEntity);
            }
        } catch (WciruServiceConnectorException | WorldCatException | OcnMismatchException  e) {
            return getFailedChunkItem(chunkItemWithWorldCatAttributes, e);
        }
    }

    /**
     * Pushes transformed record data contained in given data container to
     * WorldCat via WCIRU service
     *
     * @param chunkItemWithWorldCatAttributes holding the data to transfer
     * @param worldCatEntity containing oclc identifier (pid)
     *
     * @return the updated chunk item
     *
     * @throws WorldCatException on general exceptions
     * @throws WciruServiceConnectorException on failure communicating with wciru
     * @throws OcnMismatchException if OCN returned by subsequent WCIRU
     * replaceRecord call differs from the one returned by initial addOrUpdate
     */
    public ChunkItem pushTransformedItem(ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes, WorldCatEntity worldCatEntity)
            throws WorldCatException, WciruServiceConnectorException, OcnMismatchException {

        stringBuilder.append(toUniCodeNormalizationFormDecomposed(chunkItemWithWorldCatAttributes));
        if (isWciruDelete(chunkItemWithWorldCatAttributes)) {
            wciruDeleteRecord(chunkItemWithWorldCatAttributes, worldCatEntity);
        } else {
            wciruAddOrUpdateRecord(chunkItemWithWorldCatAttributes, worldCatEntity);
        }
        chunkItemWithWorldCatAttributes.withData(stringBuilder.toString().getBytes());
        return chunkItemWithWorldCatAttributes;
    }


    /* Profiled execution of WciruClient deleteRecord method
    */
    protected void wciruDeleteRecord(ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes, WorldCatEntity worldCatEntity) throws
            WciruServiceConnectorException, OcnMismatchException, IllegalArgumentException {
        if (worldCatEntity.getOcn().isEmpty()) {
            log.warn("WCIRU delete empty OCN number");
        } else {
            final Element element = toElement(toUniCodeNormalizationFormDecomposed(chunkItemWithWorldCatAttributes));
            // First delete primary holding symbol
            final UpdateResponseType replaceResponse = wciruServiceConnector.replaceRecord(element, worldCatEntity.getOcn(), PRIMARY_HOLDING_SYMBOL, Holding.Action.DELETE.getWciruValue());

            // ... then append replace response diagnostics to the existing item data
            appendWciruResponseDiagnostics(replaceResponse);

            // ... then delete remaining holding symbols
            final String ocn = getOcnFromServiceResponse(replaceResponse);
            wciruReplace(chunkItemWithWorldCatAttributes, worldCatEntity.getPid(), ocn);

            // ... then delete the WCIRU record using WCIRU "Unlink"
            UpdateResponseType deleteResponse = wciruServiceConnector.deleteRecord(element, worldCatEntity.getOcn());

            // ... then append delete response diagnostics to the existing item data
            appendWciruResponseDiagnostics(deleteResponse);

            // ... then verify, that the OCN number from deleteResponse is the same as from all the "Replace" calls
            String ocnFromDelete = getOcnFromServiceResponse(deleteResponse);
            if (!ocn.equals(ocnFromDelete)) {
                throw new OcnMismatchException(String.format("PID: '%s', expected OCN: '%s', actual OCN: '%s'", worldCatEntity.getPid(), ocn, ocnFromDelete));
            }

            // TODO: 03/03/2017 should we delete anything?
//                deleteIdStoreMappings(worldCatEntity.getPid());
        }

    }

    /**
     * Retrieves the record ocn from response
     * @param response holdong thre record identifier
     * @return the ocn found
     * @throws IllegalStateException if ocn is not in response
     */
    private String getOcnFromServiceResponse(UpdateResponseType response) throws IllegalStateException {
        final String ocn = response.getRecordIdentifier();
        if (ocn == null || ocn.isEmpty()) {
            throw new IllegalStateException("OCN was not returned returned by WCIRU service");
        }
        return ocn;
    }

    /**
     * Extracts diagnostic and appends to the existing item data
     * @param response holding the diagnostics for both success and failure
     */
    private void appendWciruResponseDiagnostics(UpdateResponseType response) {
        // WCIRU can return diagnostics even for successful requests,
        DiagnosticsType diagnosticsType = response.getDiagnostics();

        if(diagnosticsType != null) {
            for (Diagnostic diagnostic : diagnosticsType.getDiagnostic()) {
                log.warn("WCIRU diagnostic: {}", diagnostic.toString());
                String diagnosticData = WciruServiceConnectorException.toString(diagnostic);
                stringBuilder.append(diagnosticData);
            }
        }
    }

    /**
     * Replaces a record i worldcat and checks that the ocn returned matches the ocn stored in dataio
     * @param chunkItemWithWorldCatAttributes holding the data to transfer
     * @param pid oclc identifier
     * @param expectedOcn ocn known in dbc
     * @throws WciruServiceConnectorException on failure communicating with wciru
     * @throws OcnMismatchException if OCN returned by subsequent WCIRU
     * replaceRecord call differs from the one returned by initial addOrUpdate
     */
    private void wciruReplace(ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes, String pid, String expectedOcn) throws WciruServiceConnectorException, OcnMismatchException {
        for (Holding holding : chunkItemWithWorldCatAttributes.getWorldCatAttributes().getHoldings()) {
            String symbol = holding.getSymbol();
            if (!PRIMARY_HOLDING_SYMBOL.equals(symbol)) {

                final String chunkItemData = toUniCodeNormalizationFormDecomposed(chunkItemWithWorldCatAttributes);
                UpdateResponseType response = wciruServiceConnector.replaceRecord(toElement(chunkItemData), expectedOcn, symbol, holding.getAction().getWciruValue());
                appendWciruResponseDiagnostics(response);

                // throw if replaceRecord call returns with OCN that
                // differs from the one returned by the initial addOrUpdate call
                final String ocn = getOcnFromServiceResponse(response);
                if (!expectedOcn.equals(ocn)) {
                    throw new OcnMismatchException(
                            String.format("PID: '%s', expected OCN: '%s', actual OCN: '%s'", pid, expectedOcn, ocn));
                }
            }
        }
    }

    /**
     * Deciphers if the holdings are to be deleted
     * @param chunkItemWithWorldCatAttributes contaning the holdings
     * @return true if all holdings have actions delete - otherwise false
     */
    private boolean isWciruDelete(ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes) {
        final List<Holding> holdings = chunkItemWithWorldCatAttributes.getWorldCatAttributes().getHoldings();
        int deleteCount = 0;
        for(Holding holding : holdings) {
            if(holding.getAction() == Holding.Action.DELETE){
                deleteCount++;
            }
        }
        return deleteCount == chunkItemWithWorldCatAttributes.getWorldCatAttributes().getHoldings().size();
    }

    /*
     * private methods
     */

    /* Profiled execution of WciruClient addOrUpdateRecord method
     */
    private void wciruAddOrUpdateRecord(ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes, WorldCatEntity worldCatEntity) throws WorldCatException, OcnMismatchException {
        try {
            final Element element = toElement(toUniCodeNormalizationFormDecomposed(chunkItemWithWorldCatAttributes));
            final UpdateResponseType response;

            response = wciruServiceConnector.addOrUpdateRecord(element, PRIMARY_HOLDING_SYMBOL, worldCatEntity.getOcn());

            appendWciruResponseDiagnostics(response);

            final String ocn = getOcnFromServiceResponse(response);
            wciruReplace(chunkItemWithWorldCatAttributes, worldCatEntity.getPid(), ocn);

            // The worldCat entity is updated when;
            // Either: The mapping between pid and OCN did not exist
            // Or:     The OCN returned from OCLC differs from the existing value
            if (worldCatEntity.getOcn().isEmpty() || !worldCatEntity.getOcn().equals(ocn)) {
                // No local ID to OCN mapping was known to us beforehand so we update the worldCat entity
                worldCatEntity.withOcn(ocn);
            }
        } catch (WciruServiceConnectorException e) {
            throw new WorldCatException("Unable to add record data", e);
        }
    }

    private String toUniCodeNormalizationFormDecomposed(ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes) {
        return UnicodeNormalizationFormDecomposed.of(new String(chunkItemWithWorldCatAttributes.getData(), chunkItemWithWorldCatAttributes.getEncoding()));
    }

    private Element toElement(String data) throws IllegalArgumentException {
        try {
            return JaxpUtil.parseDocument(data).getDocumentElement();
        } catch (IOException | SAXException e) {
            throw new IllegalArgumentException("Error parsing data", e);
        }
    }

    private ChunkItem getFailedChunkItem(ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes, Exception e) {
        return new ChunkItem().withStatus(ChunkItem.Status.FAILURE)
                .withType(ChunkItem.Type.STRING)
                .withId(chunkItemWithWorldCatAttributes.getId())
                .withTrackingId(chunkItemWithWorldCatAttributes.getTrackingId())
                .withEncoding(StandardCharsets.UTF_8)
                .withData(e.getMessage())
                .withDiagnostics(new dk.dbc.dataio.commons.types.Diagnostic(dk.dbc.dataio.commons.types.Diagnostic.Level.FATAL, e.getMessage(), e));
    }
}
