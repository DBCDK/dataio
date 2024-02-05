
package junit.testsuite;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the junit.testsuite package. 
 * <p>An ObjectFactory allows you to programmatically 
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

    private static final QName _SystemErr_QNAME = new QName("", "system-err");
    private static final QName _SystemOut_QNAME = new QName("", "system-out");
    private static final QName _RerunFailure_QNAME = new QName("", "rerunFailure");
    private static final QName _RerunError_QNAME = new QName("", "rerunError");
    private static final QName _FlakyFailure_QNAME = new QName("", "flakyFailure");
    private static final QName _FlakyError_QNAME = new QName("", "flakyError");
    private static final QName _RerunTypeStackTrace_QNAME = new QName("", "stackTrace");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: junit.testsuite
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Failure }
     * 
     * @return
     *     the new instance of {@link Failure }
     */
    public Failure createFailure() {
        return new Failure();
    }

    /**
     * Create an instance of {@link Error }
     * 
     * @return
     *     the new instance of {@link Error }
     */
    public Error createError() {
        return new Error();
    }

    /**
     * Create an instance of {@link Skipped }
     * 
     * @return
     *     the new instance of {@link Skipped }
     */
    public Skipped createSkipped() {
        return new Skipped();
    }

    /**
     * Create an instance of {@link Properties }
     * 
     * @return
     *     the new instance of {@link Properties }
     */
    public Properties createProperties() {
        return new Properties();
    }

    /**
     * Create an instance of {@link Property }
     * 
     * @return
     *     the new instance of {@link Property }
     */
    public Property createProperty() {
        return new Property();
    }

    /**
     * Create an instance of {@link RerunType }
     * 
     * @return
     *     the new instance of {@link RerunType }
     */
    public RerunType createRerunType() {
        return new RerunType();
    }

    /**
     * Create an instance of {@link Testcase }
     * 
     * @return
     *     the new instance of {@link Testcase }
     */
    public Testcase createTestcase() {
        return new Testcase();
    }

    /**
     * Create an instance of {@link Testsuite }
     * 
     * @return
     *     the new instance of {@link Testsuite }
     */
    public Testsuite createTestsuite() {
        return new Testsuite();
    }

    /**
     * Create an instance of {@link Testsuites }
     * 
     * @return
     *     the new instance of {@link Testsuites }
     */
    public Testsuites createTestsuites() {
        return new Testsuites();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "system-err")
    public JAXBElement<String> createSystemErr(String value) {
        return new JAXBElement<>(_SystemErr_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "system-out")
    public JAXBElement<String> createSystemOut(String value) {
        return new JAXBElement<>(_SystemOut_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RerunType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link RerunType }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "rerunFailure")
    public JAXBElement<RerunType> createRerunFailure(RerunType value) {
        return new JAXBElement<>(_RerunFailure_QNAME, RerunType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RerunType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link RerunType }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "rerunError")
    public JAXBElement<RerunType> createRerunError(RerunType value) {
        return new JAXBElement<>(_RerunError_QNAME, RerunType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RerunType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link RerunType }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "flakyFailure")
    public JAXBElement<RerunType> createFlakyFailure(RerunType value) {
        return new JAXBElement<>(_FlakyFailure_QNAME, RerunType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RerunType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link RerunType }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "flakyError")
    public JAXBElement<RerunType> createFlakyError(RerunType value) {
        return new JAXBElement<>(_FlakyError_QNAME, RerunType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "stackTrace", scope = RerunType.class)
    public JAXBElement<String> createRerunTypeStackTrace(String value) {
        return new JAXBElement<>(_RerunTypeStackTrace_QNAME, String.class, RerunType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "system-out", scope = RerunType.class)
    public JAXBElement<String> createRerunTypeSystemOut(String value) {
        return new JAXBElement<>(_SystemOut_QNAME, String.class, RerunType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "", name = "system-err", scope = RerunType.class)
    public JAXBElement<String> createRerunTypeSystemErr(String value) {
        return new JAXBElement<>(_SystemErr_QNAME, String.class, RerunType.class, value);
    }

}
