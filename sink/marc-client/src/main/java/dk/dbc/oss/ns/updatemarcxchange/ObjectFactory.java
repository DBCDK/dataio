
package dk.dbc.oss.ns.updatemarcxchange;

import javax.xml.namespace.QName;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the dk.dbc.oss.ns.updatemarcxchange package. 
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

    private static final QName _UpdateMarcXchangeRequest_QNAME = new QName("http://oss.dbc.dk/ns/updateMarcXchange", "UpdateMarcXchangeRequest");
    private static final QName _UpdateMarcXchangeResult_QNAME = new QName("http://oss.dbc.dk/ns/updateMarcXchange", "UpdateMarcXchangeResult");
    private static final QName _MarcXchangeRecord_QNAME = new QName("http://oss.dbc.dk/ns/updateMarcXchange", "MarcXchangeRecord");
    private static final QName _MarcXchangeRecordId_QNAME = new QName("http://oss.dbc.dk/ns/updateMarcXchange", "MarcXchangeRecordId");
    private static final QName _TrackingId_QNAME = new QName("http://oss.dbc.dk/ns/updateMarcXchange", "trackingId");
    private static final QName _UpdateMarcXchangeMessage_QNAME = new QName("http://oss.dbc.dk/ns/updateMarcXchange", "updateMarcXchangeMessage");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: dk.dbc.oss.ns.updatemarcxchange
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link UpdateMarcXchange }
     * 
     * @return
     *     the new instance of {@link UpdateMarcXchange }
     */
    public UpdateMarcXchange createUpdateMarcXchange() {
        return new UpdateMarcXchange();
    }

    /**
     * Create an instance of {@link UpdateMarcXchangeRequest }
     * 
     * @return
     *     the new instance of {@link UpdateMarcXchangeRequest }
     */
    public UpdateMarcXchangeRequest createUpdateMarcXchangeRequest() {
        return new UpdateMarcXchangeRequest();
    }

    /**
     * Create an instance of {@link UpdateMarcXchangeResponse }
     * 
     * @return
     *     the new instance of {@link UpdateMarcXchangeResponse }
     */
    public UpdateMarcXchangeResponse createUpdateMarcXchangeResponse() {
        return new UpdateMarcXchangeResponse();
    }

    /**
     * Create an instance of {@link UpdateMarcXchangeResult }
     * 
     * @return
     *     the new instance of {@link UpdateMarcXchangeResult }
     */
    public UpdateMarcXchangeResult createUpdateMarcXchangeResult() {
        return new UpdateMarcXchangeResult();
    }

    /**
     * Create an instance of {@link MarcXchangeRecord }
     * 
     * @return
     *     the new instance of {@link MarcXchangeRecord }
     */
    public MarcXchangeRecord createMarcXchangeRecord() {
        return new MarcXchangeRecord();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UpdateMarcXchangeRequest }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link UpdateMarcXchangeRequest }{@code >}
     */
    @XmlElementDecl(namespace = "http://oss.dbc.dk/ns/updateMarcXchange", name = "UpdateMarcXchangeRequest")
    public JAXBElement<UpdateMarcXchangeRequest> createUpdateMarcXchangeRequest(UpdateMarcXchangeRequest value) {
        return new JAXBElement<>(_UpdateMarcXchangeRequest_QNAME, UpdateMarcXchangeRequest.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UpdateMarcXchangeResult }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link UpdateMarcXchangeResult }{@code >}
     */
    @XmlElementDecl(namespace = "http://oss.dbc.dk/ns/updateMarcXchange", name = "UpdateMarcXchangeResult")
    public JAXBElement<UpdateMarcXchangeResult> createUpdateMarcXchangeResult(UpdateMarcXchangeResult value) {
        return new JAXBElement<>(_UpdateMarcXchangeResult_QNAME, UpdateMarcXchangeResult.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link MarcXchangeRecord }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link MarcXchangeRecord }{@code >}
     */
    @XmlElementDecl(namespace = "http://oss.dbc.dk/ns/updateMarcXchange", name = "MarcXchangeRecord")
    public JAXBElement<MarcXchangeRecord> createMarcXchangeRecord(MarcXchangeRecord value) {
        return new JAXBElement<>(_MarcXchangeRecord_QNAME, MarcXchangeRecord.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://oss.dbc.dk/ns/updateMarcXchange", name = "MarcXchangeRecordId")
    public JAXBElement<String> createMarcXchangeRecordId(String value) {
        return new JAXBElement<>(_MarcXchangeRecordId_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://oss.dbc.dk/ns/updateMarcXchange", name = "trackingId")
    public JAXBElement<String> createTrackingId(String value) {
        return new JAXBElement<>(_TrackingId_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://oss.dbc.dk/ns/updateMarcXchange", name = "updateMarcXchangeMessage")
    public JAXBElement<String> createUpdateMarcXchangeMessage(String value) {
        return new JAXBElement<>(_UpdateMarcXchangeMessage_QNAME, String.class, null, value);
    }

}
