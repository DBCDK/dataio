
package junit.testsuite;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>{@code
 * <complexType>
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <choice maxOccurs="unbounded" minOccurs="0">
 *           <element ref="{}skipped"/>
 *           <element ref="{}error"/>
 *           <element ref="{}failure"/>
 *           <element ref="{}rerunFailure" maxOccurs="unbounded" minOccurs="0"/>
 *           <element ref="{}rerunError" maxOccurs="unbounded" minOccurs="0"/>
 *           <element ref="{}flakyFailure" maxOccurs="unbounded" minOccurs="0"/>
 *           <element ref="{}flakyError" maxOccurs="unbounded" minOccurs="0"/>
 *           <element ref="{}system-out"/>
 *           <element ref="{}system-err"/>
 *         </choice>
 *       </sequence>
 *       <attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="time" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="classname" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       <attribute name="group" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "skippedOrErrorOrFailure"
})
@XmlRootElement(name = "testcase")
public class Testcase {
    private static final Map<ChunkItem.Status, Supplier<TestInfo>> STATUS_MAPPING = new EnumMap<>(Map.of(ChunkItem.Status.FAILURE, Failure::new, ChunkItem.Status.IGNORE, Skipped::new));

    @XmlElementRefs({
        @XmlElementRef(name = "skipped", type = Skipped.class, required = false),
        @XmlElementRef(name = "error", type = Error.class, required = false),
        @XmlElementRef(name = "failure", type = Failure.class, required = false),
        @XmlElementRef(name = "rerunFailure", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "rerunError", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "flakyFailure", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "flakyError", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "system-out", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "system-err", type = JAXBElement.class, required = false)
    })
    protected List<Object> skippedOrErrorOrFailure;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "time")
    protected String time;
    @XmlAttribute(name = "classname")
    protected String classname;
    @XmlAttribute(name = "group")
    protected String group;

    /**
     * Gets the value of the skippedOrErrorOrFailure property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a {@code set} method for the skippedOrErrorOrFailure property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSkippedOrErrorOrFailure().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link RerunType }{@code >}
     * {@link JAXBElement }{@code <}{@link RerunType }{@code >}
     * {@link JAXBElement }{@code <}{@link RerunType }{@code >}
     * {@link JAXBElement }{@code <}{@link RerunType }{@code >}
     * {@link Error }
     * {@link Failure }
     * {@link Skipped }
     * 
     * 
     * @return
     *     The value of the skippedOrErrorOrFailure property.
     */
    public List<Object> getSkippedOrErrorOrFailure() {
        if (skippedOrErrorOrFailure == null) {
            skippedOrErrorOrFailure = new ArrayList<>();
        }
        return this.skippedOrErrorOrFailure;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the time property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTime() {
        return time;
    }

    /**
     * Sets the value of the time property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTime(String value) {
        this.time = value;
    }

    /**
     * Gets the value of the classname property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClassname() {
        return classname;
    }

    /**
     * Sets the value of the classname property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setClassname(String value) {
        this.classname = value;
    }

    /**
     * Gets the value of the group property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getGroup() {
        return group;
    }

    /**
     * Sets the value of the group property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setGroup(String value) {
        this.group = value;
    }

    public Testcase withSkippedOrErrorOrFailure(List<Object> skippedOrErrorOrFailure) {
        this.skippedOrErrorOrFailure = skippedOrErrorOrFailure;
        return this;
    }

    public Testcase withName(String name) {
        this.name = name;
        return this;
    }

    public Testcase withTime(String time) {
        this.time = time;
        return this;
    }

    public Testcase withClassname(String classname) {
        this.classname = classname;
        return this;
    }

    public Testcase withGroup(String group) {
        this.group = group;
        return this;
    }

    public static Testcase from(ChunkItem ci) {
        Testcase testcase = new Testcase().withName("Post " + ci.getId());
        Optional<Diagnostic> diag = Optional.ofNullable(ci.getDiagnostics()).stream().flatMap(Collection::stream).findFirst();
        Supplier<TestInfo> supplier = STATUS_MAPPING.get(ci.getStatus());
        if(supplier != null) {
            TestInfo info = supplier.get().withContent(new String(ci.getData(), ci.getEncoding()));
            diag.ifPresent(d -> info.withMessage(d.getMessage()));
            testcase.skippedOrErrorOrFailure = List.of(info);
        }
        return testcase;
    }
}
