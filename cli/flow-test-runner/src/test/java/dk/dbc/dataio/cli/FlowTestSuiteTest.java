package dk.dbc.dataio.cli;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

class FlowTestSuiteTest {

    @Test
    void findFlowTestSuites_suite1() throws IOException {
        final List<FlowTestSuite> flowTestSuites = FlowTestSuite.findFlowTestSuites(Path.of("src", "test", "resources", "suite1"));

        assertThat("feature files found", flowTestSuites.size(), is(1));
        final FlowTestSuite flowTestSuite = flowTestSuites.get(0);
        assertThat("feature name", flowTestSuite.getFeatureName(), is("suite1"));
        assertThat("feature description", flowTestSuite.getFeatureDescription(), is("150041.credo\n"));

        assertThat("scenarios found", flowTestSuite.getScenarios(), containsInAnyOrder(
                new FlowTestSuite.Scenario()
                        .withName("preparation_of_xml_record_from_credo")
                        .withInputFile("150041.credo.enpost.xml")
                        .withOutputFile("150041.credo.enpost.tickle.txt")
                        .withAgency("150041")
                        .withFormat("credo"),
                new FlowTestSuite.Scenario()
                        .withName("preparation_of_another_xml_record_from_credo")
                        .withInputFile("150041.credo.topost.xml")
                        .withOutputFile("150041.credo.topost.tickle.txt")
                        .withAgency("150041")
                        .withFormat("credo"),
                new FlowTestSuite.Scenario()
                        .withName("preparation_of_xml_delete_record_from_credo")
                        .withInputFile("150041.credo.delete.xml")
                        .withOutputFile("150041.credo.delete.tickle.txt")
                        .withAgency("150041")
                        .withFormat("credo")
        ));
    }

    @Test
    void findFlowTestSuites_suite2() throws IOException {
        final List<FlowTestSuite> flowTestSuites = FlowTestSuite.findFlowTestSuites(Path.of("src", "test", "resources", "suite2"));

        assertThat("feature files found", flowTestSuites.size(), is(1));
        final FlowTestSuite flowTestSuite = flowTestSuites.get(0);
        assertThat("feature name", flowTestSuite.getFeatureName(), is("suite2"));
        assertThat("feature description", flowTestSuite.getFeatureDescription(),
                is("EBOG5 På baggrund af data fra dmat service skal der dannes en opdateret post til RR\n\nPå baggrund af data fra dmat service og en vedhæftet post skal der dannes en opdateret post i RR\n"));

        assertThat("scenarios found", flowTestSuite.getScenarios(), containsInAnyOrder(
                new FlowTestSuite.Scenario()
                        .withName("json_object_and_attached_record_from_dmat_service_can_be_converted_to_an_updated_marc_record_for_RR_update_service_-_revived_for_ereol,_record_with_845_(MS-4236)")
                        .withInputFile("190015.dmat.9788743053545-135310794.json.addi")
                        .withOutputFile("870970.135310794.dm2.addi")
                        .withAgency("190015")
                        .withFormat("dmat"),
                new FlowTestSuite.Scenario()
                        .withName("json_object_and_attached_record_from_dmat_service_can_be_converted_to_an_updated_marc_record_for_RR_update_service_-_update_with_990b_and_f07_after_BKMV")
                        .withInputFile("190015.dmat.9788794232111-134591773.json.addi")
                        .withOutputFile("870970.134591773.dm2.addi")
                        .withAgency("190015")
                        .withFormat("dmat")
        ));
    }

    @Test
    void findFlowTestSuites_multipleFound() throws IOException {
        final List<FlowTestSuite> flowTestSuites = FlowTestSuite.findFlowTestSuites(Path.of("src", "test", "resources"));

        assertThat("feature files found", flowTestSuites.size(), is(2));
    }
}