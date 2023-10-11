
package info.lc.xmlns.marcxchange_v1;

import javax.xml.namespace.QName;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the info.lc.xmlns.marcxchange_v1 package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName _Collection_QNAME = new QName("info:lc/xmlns/marcxchange-v1", "collection");
    private static final QName _Record_QNAME = new QName("info:lc/xmlns/marcxchange-v1", "record");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: info.lc.xmlns.marcxchange_v1
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link CollectionType }
     * 
     * @return
     *     the new instance of {@link CollectionType }
     */
    public CollectionType createCollectionType() {
        return new CollectionType();
    }

    /**
     * Create an instance of {@link RecordType }
     * 
     * @return
     *     the new instance of {@link RecordType }
     */
    public RecordType createRecordType() {
        return new RecordType();
    }

    /**
     * Create an instance of {@link LeaderFieldType }
     * 
     * @return
     *     the new instance of {@link LeaderFieldType }
     */
    public LeaderFieldType createLeaderFieldType() {
        return new LeaderFieldType();
    }

    /**
     * Create an instance of {@link ControlFieldType }
     * 
     * @return
     *     the new instance of {@link ControlFieldType }
     */
    public ControlFieldType createControlFieldType() {
        return new ControlFieldType();
    }

    /**
     * Create an instance of {@link DataFieldType }
     * 
     * @return
     *     the new instance of {@link DataFieldType }
     */
    public DataFieldType createDataFieldType() {
        return new DataFieldType();
    }

    /**
     * Create an instance of {@link SubfieldatafieldType }
     * 
     * @return
     *     the new instance of {@link SubfieldatafieldType }
     */
    public SubfieldatafieldType createSubfieldatafieldType() {
        return new SubfieldatafieldType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CollectionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link CollectionType }{@code >}
     */
    @XmlElementDecl(namespace = "info:lc/xmlns/marcxchange-v1", name = "collection")
    public JAXBElement<CollectionType> createCollection(CollectionType value) {
        return new JAXBElement<>(_Collection_QNAME, CollectionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RecordType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link RecordType }{@code >}
     */
    @XmlElementDecl(namespace = "info:lc/xmlns/marcxchange-v1", name = "record")
    public JAXBElement<RecordType> createRecord(RecordType value) {
        return new JAXBElement<>(_Record_QNAME, RecordType.class, null, value);
    }

}
