package dk.dbc.dataio.sink.ims;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.oss.ns.updatemarcxchange.MarcXchangeRecord;
import info.lc.xmlns.marcxchange_v1.CollectionType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;

public class MarcXchangeRecordUnmarshaller {

    private final Unmarshaller unmarshaller;

    public MarcXchangeRecordUnmarshaller() throws IllegalStateException {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(CollectionType.class);
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            throw new IllegalStateException("Exception caught while instantiating JaxbContext", e);
        }
    }

    /* Transforms given MARC exchange collection into its corresponding CollectionType
      representation and wraps it in a MarcXchangeRecord
    */
    public MarcXchangeRecord toMarcXchangeRecord(ChunkItem chunkItem) throws JAXBException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(chunkItem.getData());
        JAXBElement<CollectionType> jaxbCollection = unmarshaller.unmarshal(new StreamSource(byteArrayInputStream), CollectionType.class);
        MarcXchangeRecord marcXchangeRecord = new MarcXchangeRecord();
        marcXchangeRecord.setCollection(jaxbCollection.getValue());
        marcXchangeRecord.setMarcXchangeRecordId(String.valueOf(chunkItem.getId()));
        return marcXchangeRecord;
    }
}
