package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.es.entity.EsInFlight;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.marc.DanMarc2Charset;
import dk.dbc.marc.Iso2709Packer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@MessageDriven
public class EsMessageProcessorBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsMessageProcessorBean.class);
    private static final String PROCESSING_TAG = "dataio:sink-processing";
    private DocumentBuilder documentBuilder;
    private Transformer transformer;

    @EJB
    EsThrottlerBean esThrottler;

    @EJB
    EsConnectorBean esConnector;

    @EJB
    EsInFlightBean esInFlightAdmin;

    @EJB
    EsSinkConfigurationBean configuration;

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @PostConstruct
    public void setup() {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            transformer = transformerFactory.newTransformer();
        } catch (ParserConfigurationException | TransformerConfigurationException e) {
            throw new EJBException(e);
        }
    }

    /**
     * Generates ES workload from given processor result, ensures that ES processing
     * capacity is not exceeded by acquiring the necessary number of record slots from
     * the ES throttler, creates ES task package, and marks it as being in-flight.
     * @param consumedMessage message containing chunk payload
     * @throws SinkException on any failure during workload processing
     * @throws InvalidMessageException if unable to unmarshall message payload to chunk
     */
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws SinkException, InvalidMessageException {
        final ExternalChunk processedChunk = unmarshallPayload(consumedMessage);
        final EsWorkload workload = getEsWorkloadFromChunkResult(processedChunk);
        final ExternalChunk deliveredChunk = workload.getDeliveredChunk();

        if (!esThrottler.acquireRecordSlots(workload.getAddiRecords().size())) {
            LOGGER.warn("Unable to acquire needed record slots - forcing rollback");
            messageDrivenContext.setRollbackOnly();
            return;
        }
        try {
            if (workload.getAddiRecords().isEmpty()) {
                try {
                    jobStoreServiceConnectorBean.getConnector().addChunkIgnoreDuplicates(deliveredChunk, deliveredChunk.getJobId(), deliveredChunk.getChunkId());
                } catch (JobStoreServiceConnectorException e) {
                    throw new EJBException(e);
                }

                LOGGER.info("chunk {} of job {} contained no Addi records - sending result",
                        deliveredChunk.getChunkId(), deliveredChunk.getJobId());
            } else {
                final int targetReference = esConnector.insertEsTaskPackage(workload);
                final EsInFlight esInFlight = new EsInFlight();
                esInFlight.setResourceName(configuration.getEsResourceName());
                esInFlight.setJobId(deliveredChunk.getJobId());
                esInFlight.setChunkId(deliveredChunk.getChunkId());
                esInFlight.setRecordSlots(workload.getAddiRecords().size());
                esInFlight.setTargetReference(targetReference);
                esInFlight.setIncompleteDeliveredChunk(JsonUtil.toJson(deliveredChunk));
                esInFlightAdmin.addEsInFlight(esInFlight);

                LOGGER.info("Created ES task package with target reference {} for chunk {} of job {}",
                        targetReference, deliveredChunk.getChunkId(), deliveredChunk.getJobId());
            }
        } catch (Exception e) {
            esThrottler.releaseRecordSlots(workload.getAddiRecords().size());
            throw new SinkException("Exception caught during workload processing", e);
        }
    }

    /**
     * Generates ES workload based on given chunk content.
     * <br/> All input ChunkItems with status SUCCESS containing valid Addi records are converted into ChunkItem placeholders with status SUCCESS in Delivered Chunk
     * <br/> All input ChunkItems with status SUCCESS containing invalid Addi records are converted into ChunkItems with status FAILURE in Delivered Chunk
     * <br/> All input ChunkItems with status IGNORE are converted into ChunkItems with status IGNORE in Delivered Chunk
     * <br/> All input ChunkItems with status FAILURE are converted into ChunkItems with status IGNORE in Delivered Chunk
     * @param processedChunk processor result
     * @return ES workload
     * @throws SinkException on unhandled ChunkItem status
     */
    EsWorkload getEsWorkloadFromChunkResult(ExternalChunk processedChunk) throws SinkException {
        final int numberOfItems = processedChunk.size();
        final List<AddiRecord> addiRecords = new ArrayList<>(numberOfItems);
        final ExternalChunk incompleteDeliveredChunk = new ExternalChunk(processedChunk.getJobId(), processedChunk.getChunkId(), ExternalChunk.Type.DELIVERED);
        incompleteDeliveredChunk.setEncoding(processedChunk.getEncoding());
        final StopWatch stopWatch = new StopWatch();

        for(ChunkItem chunkItem : processedChunk) {
            switch (chunkItem.getStatus()) {
                case SUCCESS:
                    try {
                        addiRecords.add(buildAddiRecord(chunkItem, processedChunk));
                        incompleteDeliveredChunk.insertItem(new ChunkItem(chunkItem.getId(), "Empty slot", ChunkItem.Status.SUCCESS));
                    } catch (RuntimeException | IOException | SAXException | TransformerException e) {
                        incompleteDeliveredChunk.insertItem(new ChunkItem(chunkItem.getId(), e.getMessage(), ChunkItem.Status.FAILURE));
                    } finally {
                        LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
                    }
                    break;
                case FAILURE:
                    incompleteDeliveredChunk.insertItem(new ChunkItem(chunkItem.getId(), "Failed by processor", ChunkItem.Status.IGNORE));
                    break;
                case IGNORE:
                    incompleteDeliveredChunk.insertItem(new ChunkItem(chunkItem.getId(), "Ignored by processor", ChunkItem.Status.IGNORE));
                    break;
                default:
                    throw new SinkException("Unknown chunk item state: " + chunkItem.getStatus().name());
            }
        }
        return new EsWorkload(incompleteDeliveredChunk, addiRecords);
    }

    /**
     * This method determines builds an addi record following the below rules:
     *
     * If the processing tag(dataio:sink-processing) is not found within the meta data, the addi-record is extracted from given chunk item.
     *
     * If processing tag is found with value FALSE: the tag is removed from the meta data.
     *                                              new addi-record is created  -> containing meta data without processing tag and content data from given chunkItem.
     *
     * If processing tag is found with value TRUE:  the tag is removed from the meta data.
     *                                              the content data from the chunk item converted to iso2709.
     *                                              new addi-record is created -> containing meta data without processing tag and content data as 2709.
     * @param chunkItem chunk item
     * @param processedChunk external chunk
     * @return the processed addi-record
     * @throws IOException if an error occurs during reading of the addi-data
     * @throws SAXException on failure to parse document
     * @throws TransformerException on failure to transform to byte array
     */
     AddiRecord buildAddiRecord(ChunkItem chunkItem, ExternalChunk processedChunk) throws IOException, SAXException, TransformerException {
        AddiRecord addiRecordFromChunkItem = ESTaskPackageUtil.getAddiRecordFromChunkItem(chunkItem, processedChunk.getEncoding());
        Document metaDataDocument = getDocument(addiRecordFromChunkItem.getMetaData());
        NodeList nodeList = metaDataDocument.getElementsByTagName(PROCESSING_TAG);
        final AddiRecord processedAddiRecord;

        if(nodeList.getLength() == 1) { // The specific tag has been located
            if (do2709Encoding(nodeList)) {
                Document contentDataDocument = getDocument(addiRecordFromChunkItem.getContentData());
                byte[] as2709 = Iso2709Packer.create2709FromMarcXChangeRecord(contentDataDocument, new DanMarc2Charset());
                removeNodeFromDom(nodeList.item(0));
                byte[] newMetaData = domToByteArray(metaDataDocument);
                processedAddiRecord = new AddiRecord(newMetaData, as2709);
            } else {
                removeNodeFromDom(nodeList.item(0));
                byte[] newMetaData = domToByteArray(metaDataDocument);
                processedAddiRecord = new AddiRecord(newMetaData, addiRecordFromChunkItem.getContentData());
            }
        } else {
            processedAddiRecord = ESTaskPackageUtil.getAddiRecordFromChunkItem(chunkItem, processedChunk.getEncoding());
        }
         return processedAddiRecord;
    }

    /**
     * This method removes a child node from the dom
     * @param node the node to remove
     */
    void removeNodeFromDom(Node node) {
        node.getParentNode().removeChild(node); // Remove node from dom
    }

    /**
     * This method converts a document to a byte array
     * @param document the document to convert
     * @return a new byte array
     * @throws TransformerException
     */
    byte[] domToByteArray(Document document) throws TransformerException {
        Source source = new DOMSource(document);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Result result = new StreamResult(byteArrayOutputStream);
        transformer.reset();
        transformer.transform(source, result);
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * This method determines if iso2709 encoding should be performed depending on the node value
     * @param nodeList if not null then containing exactly one item
     * @return true if iso2709 encoding should be performed, otherwise false
     */
    boolean do2709Encoding(NodeList nodeList) {
        Node node = nodeList.item(0);
        return Boolean.valueOf(node.getAttributes().getNamedItem("encodeAs2709").getNodeValue());
    }

    Document getDocument(byte[] byteArray) throws IOException, SAXException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        documentBuilder.reset();
        return documentBuilder.parse(byteArrayInputStream);
    }

}
