/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.promat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class PromatCaseXmlTransformer {
    private final XmlMapper xmlMapper;

    public PromatCaseXmlTransformer() {
        final JacksonXmlModule module = new JacksonXmlModule();
        module.setDefaultUseWrapper(true);
        xmlMapper = new XmlMapper(module);
        xmlMapper.registerModule(new JavaTimeModule());

        xmlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(SerializationFeature.INDENT_OUTPUT, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .addMixIn(PromatCase.class, PromatCaseXmlTransformer.PromatCaseMixin.class);
    }

    /**
     * Transforms given {@link PromatCase} into XML
     * @param promatCase Promat case
     * @return XML representation of Promat case
     * @throws HarvesterException if unable to transform
     */
    public byte[] toXml(PromatCase promatCase) throws HarvesterException {
        final StringWriter stringWriter = new StringWriter();
        try {
            xmlMapper.writeValue(stringWriter, promatCase);
        } catch (IOException e) {
            final String err = "Unable to transform promat case " +
                    (promatCase == null ? "<null>" : String.valueOf(promatCase.getId())) +
                    " to XML";
            throw new HarvesterException(err, e);
        }
        return stringWriter.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Without this mixin class case tasks would be output as
    * <pre>
    * {@code
    *   <tasks>
    *     <tasks>...</tasks>
    *     <tasks>...</tasks>
    *     ...
    *   </tasks>
    * }
    * </pre>
    * instead of
    * <pre>
    * {@code
    *   <tasks>
    *     <task>...</task>
    *     <task>...</task>
    *     ...
    *   </tasks>
    * }
    * </pre>
    *
    */
    @SuppressWarnings("PMD")
    public static abstract class PromatCaseMixin {
        @JsonProperty("task")
        @JacksonXmlElementWrapper(localName = "tasks")
        private List<PromatTask> tasks;

        @JsonProperty("code")
        @JacksonXmlElementWrapper(localName = "codes")
        private List<String> codes;
    }
}
