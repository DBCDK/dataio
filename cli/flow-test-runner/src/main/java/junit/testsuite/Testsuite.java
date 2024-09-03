
package junit.testsuite;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;


/**
 * <p>Java class for anonymous complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>{@code
 * <complexType>
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <choice maxOccurs="unbounded" minOccurs="0">
 *         <element ref="{}testsuite"/>
 *         <element ref="{}properties"/>
 *         <element ref="{}testcase"/>
 *         <element ref="{}system-out"/>
 *         <element ref="{}system-err"/>
 *       </choice>
 *       <attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="tests" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="failures" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="errors" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="group" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="time" type="{}SUREFIRE_TIME" />
 *       <attribute name="skipped" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="timestamp" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="hostname" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="package" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="file" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="log" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="url" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="version" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "testsuiteOrPropertiesOrTestcase"
})
@XmlRootElement(name = "testsuite")
public class Testsuite {

    @XmlElementRefs({
            @XmlElementRef(name = "testsuite", type = Testsuite.class, required = false),
            @XmlElementRef(name = "properties", type = Properties.class, required = false),
            @XmlElementRef(name = "testcase", type = Testcase.class, required = false),
            @XmlElementRef(name = "system-out", type = JAXBElement.class, required = false),
            @XmlElementRef(name = "system-err", type = JAXBElement.class, required = false)
    })
    protected List<Object> testsuiteOrPropertiesOrTestcase;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "tests", required = true)
    protected String tests;
    @XmlAttribute(name = "group")
    protected String group;
    @XmlAttribute(name = "time")
    protected String time;
    @XmlAttribute(name = "timestamp")
    protected String timestamp;
    @XmlAttribute(name = "hostname")
    protected String hostname;
    @XmlAttribute(name = "id")
    protected String id;
    @XmlAttribute(name = "package")
    protected String _package;
    @XmlAttribute(name = "file")
    protected String file;
    @XmlAttribute(name = "log")
    protected String log;
    @XmlAttribute(name = "url")
    protected String url;
    @XmlAttribute(name = "version")
    protected String version;

    /**
     * Gets the value of the testsuiteOrPropertiesOrTestcase property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a {@code set} method for the testsuiteOrPropertiesOrTestcase property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTestsuiteOrPropertiesOrTestcase().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link Properties }
     * {@link Testcase }
     * {@link Testsuite }
     *
     * @return The value of the testsuiteOrPropertiesOrTestcase property.
     */
    public List<Object> getTestsuiteOrPropertiesOrTestcase() {
        if (testsuiteOrPropertiesOrTestcase == null) {
            testsuiteOrPropertiesOrTestcase = new ArrayList<>();
        }
        return this.testsuiteOrPropertiesOrTestcase;
    }

    /**
     * Gets the value of the name property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the tests property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getTests() {
        return tests;
    }

    /**
     * Sets the value of the tests property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setTests(String value) {
        this.tests = value;
    }

    /**
     * Gets the value of the failures property.
     *
     * @return possible object is
     * {@link String }
     */
    @XmlAttribute(name = "failures", required = true)
    public String getFailures() {
        return Long.toString(getTestResults().filter(o -> o instanceof Failure).count());
    }

    private Stream<TestInfo> getTestResults() {
        return getTestsuiteOrPropertiesOrTestcase().stream()
                .filter(o -> o instanceof Testcase)
                .map(Testcase.class::cast)
                .map(Testcase::getSkippedOrErrorOrFailure)
                .flatMap(Collection::stream)
                .filter(ti -> ti instanceof TestInfo)
                .map(TestInfo.class::cast);
    }

    /**
     * Gets the value of the errors property.
     *
     * @return possible object is
     * {@link String }
     */
    @XmlAttribute(name = "errors", required = true)
    public String getErrors() {
        return Long.toString(getTestResults().filter(o -> o instanceof Error).count());
    }

    /**
     * Gets the value of the group property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getGroup() {
        return group;
    }

    /**
     * Sets the value of the group property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setGroup(String value) {
        this.group = value;
    }

    /**
     * Gets the value of the time property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getTime() {
        return time;
    }

    /**
     * Sets the value of the time property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setTime(String value) {
        this.time = value;
    }

    /**
     * Gets the value of the skipped property.
     *
     * @return possible object is
     * {@link String }
     */
    @XmlAttribute(name = "skipped")
    public String getSkipped() {
        return Long.toString(getTestResults().filter(o -> o instanceof Skipped).count());
    }

    /**
     * Gets the value of the timestamp property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the value of the timestamp property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setTimestamp(String value) {
        this.timestamp = value;
    }

    /**
     * Gets the value of the hostname property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Sets the value of the hostname property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setHostname(String value) {
        this.hostname = value;
    }

    /**
     * Gets the value of the id property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the package property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getPackage() {
        return _package;
    }

    /**
     * Sets the value of the package property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setPackage(String value) {
        this._package = value;
    }

    /**
     * Gets the value of the file property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getFile() {
        return file;
    }

    /**
     * Sets the value of the file property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setFile(String value) {
        this.file = value;
    }

    /**
     * Gets the value of the log property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getLog() {
        return log;
    }

    /**
     * Sets the value of the log property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setLog(String value) {
        this.log = value;
    }

    /**
     * Gets the value of the url property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the value of the url property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setUrl(String value) {
        this.url = value;
    }

    /**
     * Gets the value of the version property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the value of the version property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setVersion(String value) {
        this.version = value;
    }

    public Testsuite withTestsuiteOrPropertiesOrTestcase(List<Object> testsuiteOrPropertiesOrTestcase) {
        this.testsuiteOrPropertiesOrTestcase = testsuiteOrPropertiesOrTestcase;
        return this;
    }

    public Testsuite withName(String name) {
        this.name = name;
        return this;
    }

    public Testsuite withTests(String tests) {
        this.tests = tests;
        return this;
    }

    public Testsuite withGroup(String group) {
        this.group = group;
        return this;
    }

    public Testsuite withTime(String time) {
        this.time = time;
        return this;
    }

    public Testsuite withTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Testsuite withHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public Testsuite withId(String id) {
        this.id = id;
        return this;
    }

    public Testsuite withFile(String file) {
        this.file = file;
        return this;
    }

    public Testsuite withLog(String log) {
        this.log = log;
        return this;
    }

    public Testsuite withUrl(String url) {
        this.url = url;
        return this;
    }

    public Testsuite withVersion(String version) {
        this.version = version;
        return this;
    }
}
