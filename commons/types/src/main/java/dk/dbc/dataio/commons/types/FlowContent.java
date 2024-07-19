package dk.dbc.dataio.commons.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * FlowContent DTO class.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlowContent implements Serializable {
    private static final long serialVersionUID = 5520247158829273054L;

    private static final String MANIFEST_FILE = "META-INF/MANIFEST.MF";
    private static final String ATTRIBUTE_NAME = "Flow-Name";
    private static final String ATTRIBUTE_DESCRIPTION = "Flow-Description";
    private static final String ATTRIBUTE_ENTRYPOINT_SCRIPT = "Flow-Entrypoint-Script";
    private static final String ATTRIBUTE_ENTRYPOINT_FUNCTION = "Flow-Entrypoint-Function";

    private final String name;
    private final String description;
    private String entrypointScript;
    private String entrypointFunction;
    @JsonIgnore
    private byte[] jsar;
    private Date timeOfLastModification;

    /**
     * Constructs new instance from given JavaScript archive (jsar)
     * @param jsar JavaScript archive (i.e. zip archive) that must contain a {@value MANIFEST_FILE} file entry
     *             with the following mandatory keys: Manifest-Version, {@value ATTRIBUTE_NAME},
     *             {@value ATTRIBUTE_DESCRIPTION}, {@value ATTRIBUTE_ENTRYPOINT_SCRIPT}, and
     *             {@value ATTRIBUTE_ENTRYPOINT_FUNCTION}
     * @param timeOfLastModification time of last modification
     * @throws IllegalArgumentException on invalid JavaScript archive
     */
    public FlowContent(byte[] jsar, Date timeOfLastModification) {
        Manifest manifest = readManifestFromJsar(jsar);
        Attributes mainAttributes = manifest.getMainAttributes();
        this.name = getAttributeValue(mainAttributes, ATTRIBUTE_NAME).orElseThrow(() ->
                new IllegalArgumentException(String.format(
                        "Invalid jsar - %s missing value for %s", MANIFEST_FILE, ATTRIBUTE_NAME)));
        this.description = getAttributeValue(mainAttributes, ATTRIBUTE_DESCRIPTION).orElseThrow(() ->
                new IllegalArgumentException(String.format(
                        "Invalid jsar - %s missing value for %s", MANIFEST_FILE, ATTRIBUTE_DESCRIPTION)));
        this.entrypointScript = getAttributeValue(mainAttributes, ATTRIBUTE_ENTRYPOINT_SCRIPT).orElseThrow(() ->
                new IllegalArgumentException(String.format(
                        "Invalid jsar - %s missing value for %s", MANIFEST_FILE, ATTRIBUTE_ENTRYPOINT_SCRIPT)));
        this.entrypointFunction = getAttributeValue(mainAttributes, ATTRIBUTE_ENTRYPOINT_FUNCTION).orElseThrow(() ->
                new IllegalArgumentException(String.format(
                        "Invalid jsar - %s missing value for %s", MANIFEST_FILE, ATTRIBUTE_ENTRYPOINT_FUNCTION)));
        this.jsar = jsar;
        this.timeOfLastModification = timeOfLastModification;
    }

    @JsonCreator
    public FlowContent(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("entrypointScript") String entrypointScript,
            @JsonProperty("entrypointFunction") String entrypointFunction,
            @JsonProperty("jsar") byte[] jsar,
            @JsonProperty("timeOfLastModification") Date timeOfLastModification) {
        this.name = name;
        this.description = description;
        this.entrypointScript = entrypointScript;
        this.entrypointFunction = entrypointFunction;
        this.jsar = jsar;
        this.timeOfLastModification = timeOfLastModification;
    }

    public FlowContent(String name, String description) {
        this(name, description, null, null, null, null);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getEntrypointScript() {
        return entrypointScript;
    }

    public String getEntrypointFunction() {
        return entrypointFunction;
    }

    public byte[] getJsar() {
        return jsar;
    }

    public Date getTimeOfLastModification() {
        if (timeOfLastModification != null) {
            return new Date(timeOfLastModification.getTime());
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FlowContent that = (FlowContent) o;
        return Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(entrypointScript, that.entrypointScript) && Objects.equals(entrypointFunction, that.entrypointFunction) && Arrays.equals(jsar, that.jsar) && Objects.equals(timeOfLastModification, that.timeOfLastModification);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name, description, entrypointScript, entrypointFunction, timeOfLastModification);
        result = 31 * result + Arrays.hashCode(jsar);
        return result;
    }

    private static Manifest readManifestFromJsar(byte[] jsar) {
        try (BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(jsar));
             ZipInputStream zipInputStream = new ZipInputStream(bis)) {

            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (MANIFEST_FILE.equals(zipEntry.getName())) {
                    return new Manifest(new ByteArrayInputStream(zipInputStream.readAllBytes()));
                }
            }
            throw new IllegalArgumentException("Invalid jsar: " + MANIFEST_FILE + " not found");

        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid jsar", e);
        }
    }

    private static Optional<String> getAttributeValue(Attributes attributes, String key) {
        return Optional.ofNullable(attributes.getValue(key));
    }
}
