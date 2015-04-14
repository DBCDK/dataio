package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.service.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.es.entity.EsInFlight;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.dataio.sink.utils.messageproducer.JobProcessorMessageProducerBean;
import dk.dbc.marc.DanMarc2Charset;
import dk.dbc.marc.Iso2709Packer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
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

    @EJB
    EsThrottlerBean esThrottler;

    @EJB
    EsConnectorBean esConnector;

    @EJB
    EsInFlightBean esInFlightAdmin;

    @EJB
    EsSinkConfigurationBean configuration;

    @EJB
    JobProcessorMessageProducerBean jobProcessorMessageProducer;

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

        if (!esThrottler.acquireRecordSlots(workload.getAddiRecords().size())) {
            LOGGER.warn("Unable to acquire needed record slots - forcing rollback");
            messageDrivenContext.setRollbackOnly();
            return;
        }
        try {
            if (workload.getAddiRecords().isEmpty()) {
                jobProcessorMessageProducer.send(workload.getDeliveredChunk());

                LOGGER.info("chunk {} of job {} contained no Addi records - sending result",
                        workload.getDeliveredChunk().getChunkId(), workload.getDeliveredChunk().getJobId());
            } else {
                final int targetReference = esConnector.insertEsTaskPackage(workload);
                final EsInFlight esInFlight = new EsInFlight();
                esInFlight.setResourceName(configuration.getEsResourceName());
                esInFlight.setJobId(workload.getDeliveredChunk().getJobId());
                esInFlight.setChunkId(workload.getDeliveredChunk().getChunkId());
                esInFlight.setRecordSlots(workload.getAddiRecords().size());
                esInFlight.setTargetReference(targetReference);
                esInFlight.setIncompleteDeliveredChunk(JsonUtil.toJson(workload.getDeliveredChunk()));
                esInFlightAdmin.addEsInFlight(esInFlight);

                LOGGER.info("Created ES task package with target reference {} for chunk {} of job {}",
                        targetReference, workload.getDeliveredChunk().getChunkId(), workload.getDeliveredChunk().getJobId());
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

        for(ChunkItem chunkItem : processedChunk) {
            switch (chunkItem.getStatus()) {
                case SUCCESS:
                    try {
                        AddiRecord addiRecordFromChunkItem = ESTaskPackageUtil.getAddiRecordFromChunkItem(chunkItem, processedChunk.getEncoding());
                        Document metaDataDocument = getDocument(addiRecordFromChunkItem.getMetaData());

                        NodeList nodeList = metaDataDocument.getElementsByTagName(PROCESSING_TAG);
                        final AddiRecord processedAddiRecord;

                        if(nodeList.getLength() == 1) { // The specific tag has been located
                            if (do2709Encoding(nodeList)) {
                                Document contentDataDocument = getDocument(addiRecordFromChunkItem.getContentData());
                                byte[] as2709 = Iso2709Packer.create2709FromMarcXChangeRecord(contentDataDocument, new DanMarc2Charset());
                                byte[] newMetaData = RemoveProcessingTagFromDom(nodeList, contentDataDocument);
                                processedAddiRecord = new AddiRecord(newMetaData, as2709);
                            } else {
                                byte[] newMetaData = RemoveProcessingTagFromDom(nodeList, metaDataDocument);
                                processedAddiRecord = new AddiRecord(newMetaData, addiRecordFromChunkItem.getContentData());
                            }
                            addiRecords.remove(addiRecordFromChunkItem);
                            addiRecords.add(processedAddiRecord);
                        } else {
                            addiRecords.add(ESTaskPackageUtil.getAddiRecordFromChunkItem(chunkItem, processedChunk.getEncoding()));
                        }
                        incompleteDeliveredChunk.insertItem(new ChunkItem(chunkItem.getId(), "Empty slot", ChunkItem.Status.SUCCESS));
                    } catch (RuntimeException | IOException | SAXException | TransformerException e) {
                        incompleteDeliveredChunk.insertItem(new ChunkItem(chunkItem.getId(), e.getMessage(), ChunkItem.Status.FAILURE));
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
     * This method removes the processing tag from the dom if it exists
     * @param nodeList if not null then containing exactly one item
     * @param document the document
     * @return a new byte array representing meta data without the processing tag, null if the processing tag was not set
     * @throws TransformerException
     */
    byte[] RemoveProcessingTagFromDom(NodeList nodeList, Document document) throws TransformerException {
        byte[] processedMetaData = null;
        if(nodeList.getLength() == 1) {
            Node node = nodeList.item(0);
            node.getParentNode().removeChild(node); // Remove node from dom
            Source source = new DOMSource(document);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Result result = new StreamResult(byteArrayOutputStream);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(source, result);
            processedMetaData = byteArrayOutputStream.toByteArray();
        }
        return processedMetaData;
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
        final DocumentBuilder builder = getDocumentBuilder();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        return builder.parse(byteArrayInputStream);
    }

    private DocumentBuilder getDocumentBuilder() {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        try {
            return documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }
}
