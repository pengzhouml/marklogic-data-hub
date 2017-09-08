package com.marklogic.hub;

import com.marklogic.hub.error.ScaffoldingValidationException;
import com.marklogic.hub.flow.CodeFormat;
import com.marklogic.hub.flow.DataFormat;
import com.marklogic.hub.flow.Flow;
import com.marklogic.hub.flow.FlowType;
import com.marklogic.hub.scaffold.Scaffolding;
import com.marklogic.hub.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.*;

public class ScaffoldingTest extends HubTestBase {

    static Path projectPath = Paths.get(PROJECT_PATH).toAbsolutePath();
    private static File projectDir = projectPath.toFile();
    private static File pluginDir = projectPath.resolve("plugins").toFile();

    @BeforeClass
    public static void setup() throws IOException {
        XMLUnit.setIgnoreWhitespace(true);

        if (projectDir.exists()) {
            FileUtils.deleteDirectory(projectDir);
        }
        installHub();
    }

    @After
    public void teardown() throws IOException {
        FileUtils.deleteDirectory(projectDir);
    }

    @Test
    public void createEntity() throws FileNotFoundException {
        assertFalse(projectDir.exists());
        Scaffolding scaffolding = new Scaffolding(projectDir.toString(), stagingClient);
        scaffolding.createEntity("my-fun-test");
        assertTrue(projectDir.exists());

        Path entityDir = scaffolding.getEntityDir("my-fun-test");
        assertTrue(entityDir.toFile().exists());
        assertEquals(
                Paths.get(pluginDir.toString(), "entities", "my-fun-test"),
                entityDir);

        Path flowDir = scaffolding.getFlowDir("my-fun-test", "blah", FlowType.INPUT);
        assertEquals(Paths.get(pluginDir.toString(), "entities", "my-fun-test", "input", "blah"),
                flowDir);
        assertFalse(flowDir.toFile().exists());
    }

    @Test
    public void createXqyInputFlow() throws IOException, SAXException {
        createInputFlow(CodeFormat.XQUERY, DataFormat.XML, false);
    }

    @Test
    public void createXqyHarmonizeFlow() throws IOException, SAXException {
        createHarmonizeFlow(CodeFormat.XQUERY, DataFormat.XML, false);
    }

    @Test
    public void createSjsInputFlow() throws IOException, SAXException {
        createInputFlow(CodeFormat.JAVASCRIPT, DataFormat.JSON, false);
    }

    @Test
    public void createSjsHarmonizeFlow() throws IOException, SAXException {
        createHarmonizeFlow(CodeFormat.JAVASCRIPT, DataFormat.JSON, false);
    }

    @Test
    public void createESXqyInputFlow() throws IOException, SAXException {
        createInputFlow(CodeFormat.XQUERY, DataFormat.XML, true);
    }

    @Test
    public void createESXqyHarmonizeFlow() throws IOException, SAXException {
        createHarmonizeFlow(CodeFormat.XQUERY, DataFormat.XML, true);
    }

    @Test
    public void createESSjsInputFlow() throws IOException, SAXException {
        createInputFlow(CodeFormat.JAVASCRIPT, DataFormat.JSON, true);
    }

    @Test
    public void createESSjsHarmonizeFlow() throws IOException, SAXException {
        createHarmonizeFlow(CodeFormat.JAVASCRIPT, DataFormat.JSON, true);
    }

    private void createFlow(CodeFormat codeFormat, DataFormat dataFormat, FlowType flowType, boolean useEsModel) throws IOException, SAXException {
        String entityName = "my-fun-test";
        String flowName = "test-" + flowType.toString() + "-" + codeFormat.toString() + "-" + dataFormat.toString();

        Scaffolding scaffolding = new Scaffolding(projectDir.toString(), finalClient);

        Path entityDir = scaffolding.getEntityDir(entityName);
        assertFalse(entityDir.toString() + " should exist but doesn't", entityDir.toFile().exists());

        Path employeeDir = scaffolding.getEntityDir("employee");
        assertFalse(employeeDir.toFile().exists());

        scaffolding.createEntity(entityName);
        scaffolding.createEntity("employee");
        assertTrue(projectDir.exists());
        assertTrue(entityDir.toFile().exists());
        assertTrue(employeeDir.toFile().exists());
        assertEquals(Paths.get(pluginDir.toString(), "entities", entityName), entityDir);

        FileUtil.copy(getResourceStream("scaffolding-test/employee.entity.json"), employeeDir.resolve("employee.entity.json").toFile());
        FileUtil.copy(getResourceStream("scaffolding-test/" + entityName + ".json"), entityDir.resolve(entityName + ".entity.json").toFile());

        getDataHub().installUserModules(true);

        scaffolding.createFlow(entityName, flowName, flowType, codeFormat, dataFormat, useEsModel);
        Path flowDir = scaffolding.getFlowDir(entityName, flowName, flowType);
        assertEquals(Paths.get(pluginDir.toString(), "entities", entityName, flowType.toString(), flowName), flowDir);
        assertTrue(flowDir.toFile().exists());

        Path flowDescriptor = flowDir.resolve(flowName + ".properties");
        assertTrue(flowDescriptor.toFile().exists());

        FileInputStream fis = new FileInputStream(flowDescriptor.toFile());
        Properties properties = new Properties();
        properties.load(fis);
        fis.close();

        int expectedPropertiesCount = flowType.equals(FlowType.INPUT) ? 4 : 6;
        assertEquals(expectedPropertiesCount, properties.keySet().size());
        assertEquals(dataFormat.toString(), properties.get("dataFormat"));
        assertEquals(codeFormat.toString(), properties.get("codeFormat"));
        if (flowType.equals(FlowType.HARMONIZE)) {
            assertEquals(codeFormat.toString(), properties.get("collectorCodeFormat"));
            assertEquals("collector." + codeFormat.toString(), properties.get("collectorModule"));
        }
        assertEquals(codeFormat.toString(), properties.get("mainCodeFormat"));
        assertEquals("main." + codeFormat.toString(), properties.get("mainModule"));

        Path defaultCollector = flowDir.resolve("collector." + codeFormat.toString());
        if (flowType.equals(FlowType.INPUT)) {
            assertFalse(defaultCollector.toFile().exists());
        }
        else {
            assertTrue(defaultCollector.toFile().exists());
        }

        Path defaultContent = flowDir.resolve("content." + codeFormat.toString());
        assertTrue(defaultContent.toFile().exists());

        if (useEsModel) {
            assertEquals(getResource("scaffolding-test/es-" + flowType.toString() + "-content." + codeFormat.toString()).replaceAll(" +$", "").replaceAll("\n", "\r\n"), FileUtils.readFileToString(defaultContent.toFile()).replaceAll(" +\r\n", "\r\n"));
        }

        Path defaultHeaders = flowDir.resolve("headers." + codeFormat.toString());
        assertTrue(defaultHeaders.toFile().exists());

        Path triplesContent = flowDir.resolve("triples." + codeFormat.toString());
        assertTrue(triplesContent.toFile().exists());

        Path writer = flowDir.resolve("writer." + codeFormat.toString());
        if (flowType.equals(FlowType.INPUT)) {
            assertFalse(writer.toFile().exists());
        }
        else {
            assertTrue(writer.toFile().exists());
        }

        Path main = flowDir.resolve("main." + codeFormat.toString());
        assertTrue(main.toFile().exists());
    }

    private void createInputFlow(CodeFormat codeFormat, DataFormat dataFormat, boolean useEsModel) throws IOException, SAXException {
        createFlow(codeFormat, dataFormat, FlowType.INPUT, useEsModel);
    }

    private void createHarmonizeFlow(CodeFormat codeFormat, DataFormat dataFormat, boolean useEsModel) throws IOException, SAXException {
        createFlow(codeFormat, dataFormat, FlowType.HARMONIZE, useEsModel);
    }

    @Test
    public void createXqyRestExtension() throws IOException {
        String entityName = "my-fun-test";
        String extensionName = "myExtension";
        FlowType flowType = FlowType.HARMONIZE;
        CodeFormat pluginCodeFormat = CodeFormat.XQUERY;
        Scaffolding scaffolding = new Scaffolding(projectDir.toString(), stagingClient);
        try {
            scaffolding.createRestExtension(entityName, extensionName, flowType, pluginCodeFormat);
        } catch (ScaffoldingValidationException e) {
            Assert.fail(e.getMessage());
        }
        Path restDir = Paths.get(pluginDir.toString(), "entities", entityName, flowType.toString(), "REST").toAbsolutePath().normalize();
        assertTrue(restDir.toFile().exists());
        Path restServicesDir = restDir.resolve("services");
        assertTrue(restServicesDir.toFile().exists());
        Path restExtensionFile = restServicesDir.resolve(extensionName + "." + pluginCodeFormat);
        assertTrue(restExtensionFile.toFile().exists());
        Path restExtensionMetadataDir = restServicesDir.resolve("metadata");
        assertTrue(restExtensionMetadataDir.toFile().exists());
        Path restExtensionMetadataFile = restExtensionMetadataDir.resolve(extensionName + ".xml");
        assertTrue(restExtensionMetadataFile.toFile().exists());
    }

    @Test
    public void createSjsRestExtension() throws IOException {
        String entityName = "my-fun-test";
        String extensionName = "myExtension";
        FlowType flowType = FlowType.INPUT;
        CodeFormat pluginCodeFormat = CodeFormat.JAVASCRIPT;
        Scaffolding scaffolding = new Scaffolding(projectDir.toString(), stagingClient);
        try {
            scaffolding.createRestExtension(entityName, extensionName, flowType, pluginCodeFormat);
        } catch (ScaffoldingValidationException e) {
            Assert.fail(e.getMessage());
        }
        Path restDir = Paths.get(pluginDir.toString(), "entities", entityName, flowType.toString(), "REST").toAbsolutePath().normalize();
        assertTrue(restDir.toFile().exists());
        Path restServicesDir = restDir.resolve("services");
        assertTrue(restServicesDir.toFile().exists());
        Path restExtensionFile = restServicesDir.resolve(extensionName + "." + pluginCodeFormat);
        assertTrue(restExtensionFile.toFile().exists());
        Path restExtensionMetadataDir = restServicesDir.resolve("metadata");
        assertTrue(restExtensionMetadataDir.toFile().exists());
        Path restExtensionMetadataFile = restExtensionMetadataDir.resolve(extensionName + ".xml");
        assertTrue(restExtensionMetadataFile.toFile().exists());
    }

    @Test
    public void createXqyRestTransform() throws IOException {
        String entityName = "my-fun-test";
        String transformName = "myTransform";
        FlowType flowType = FlowType.HARMONIZE;
        CodeFormat pluginCodeFormat = CodeFormat.XQUERY;
        Scaffolding scaffolding = new Scaffolding(projectDir.toString(), stagingClient);
        try {
            scaffolding.createRestTransform(entityName, transformName, flowType, pluginCodeFormat);
        } catch (ScaffoldingValidationException e) {
            Assert.fail(e.getMessage());
        }
        Path restDir = Paths.get(pluginDir.toString(), "entities", entityName, flowType.toString(), "REST").toAbsolutePath().normalize();
        assertTrue(restDir.toFile().exists());
        Path restTransformDir = restDir.resolve("transforms");
        assertTrue(restTransformDir.toFile().exists());
        Path restTransformFile = restTransformDir.resolve(transformName + "." + pluginCodeFormat);
        assertTrue(restTransformFile.toFile().exists());
    }

    @Test
    public void createSjsRestTransform() throws IOException {
        String entityName = "my-fun-test";
        String transformName = "myTransform";
        FlowType flowType = FlowType.HARMONIZE;
        CodeFormat pluginCodeFormat = CodeFormat.JAVASCRIPT;
        Scaffolding scaffolding = new Scaffolding(projectDir.toString(), stagingClient);
        try {
            scaffolding.createRestTransform(entityName, transformName, flowType, pluginCodeFormat);
        } catch (ScaffoldingValidationException e) {
            Assert.fail(e.getMessage());
        }
        Path restDir = Paths.get(pluginDir.toString(), "entities", entityName, flowType.toString(), "REST").toAbsolutePath().normalize();
        assertTrue(restDir.toFile().exists());
        Path restTransformDir = restDir.resolve("transforms");
        assertTrue(restTransformDir.toFile().exists());
        Path restTransformFile = restTransformDir.resolve(transformName + "." + pluginCodeFormat);
        assertTrue(restTransformFile.toFile().exists());
    }

    @Test
    public void updateLegacyFlowsFrom1x() throws IOException, SAXException, ParserConfigurationException {
        String fromVersion = "1.1.5";
        Scaffolding scaffolding = new Scaffolding(projectDir.toString(), stagingClient);
        assertEquals(0, scaffolding.updateLegacyFlows(fromVersion, "my-fun-test").size());

        Path inputDir = projectPath.resolve("plugins/entities/my-fun-test/input");
        Path harmonizeDir = projectPath.resolve("plugins/entities/my-fun-test/harmonize");
        FileUtils.copyDirectory(getResourceFile("scaffolding-test/legacy-input-flow"), inputDir.resolve("legacy-input-flow").toFile());
        FileUtils.copyDirectory(getResourceFile("scaffolding-test/legacy-harmonize-flow"), harmonizeDir.resolve("legacy-harmonize-flow").toFile());

        assertEquals(2, scaffolding.updateLegacyFlows(fromVersion, "my-fun-test").size());

        FileInputStream fis = new FileInputStream(inputDir.resolve("legacy-input-flow").resolve("legacy-input-flow.properties").toFile());
        Properties properties = new Properties();
        properties.load(fis);
        fis.close();

        assertEquals(4, properties.keySet().size());
        assertEquals(CodeFormat.JAVASCRIPT.toString(), properties.get("codeFormat"));
        assertEquals(DataFormat.JSON.toString(), properties.get("dataFormat"));
        assertEquals(CodeFormat.JAVASCRIPT.toString(), properties.get("mainCodeFormat"));
        assertEquals("main.sjs", properties.get("mainModule"));
        FileInputStream inputStream = new FileInputStream(inputDir.resolve("legacy-input-flow").resolve("main.sjs").toFile());
        String actual = IOUtils.toString(inputStream);
        inputStream.close();
        assertEquals(getResource("scaffolding/input/sjs/main-legacy-1x.sjs"), actual);

        fis = new FileInputStream(harmonizeDir.resolve("legacy-harmonize-flow").resolve("legacy-harmonize-flow.properties").toFile());
        properties = new Properties();
        properties.load(fis);
        fis.close();

        assertEquals(6, properties.keySet().size());
        assertEquals(CodeFormat.JAVASCRIPT.toString(), properties.get("codeFormat"));
        assertEquals(DataFormat.JSON.toString(), properties.get("dataFormat"));
        assertEquals(CodeFormat.JAVASCRIPT.toString(), properties.get("collectorCodeFormat"));
        assertEquals("collector/collector.sjs", properties.get("collectorModule"));
        assertEquals(CodeFormat.JAVASCRIPT.toString(), properties.get("mainCodeFormat"));
        assertEquals("main.sjs", properties.get("mainModule"));
        inputStream = new FileInputStream(harmonizeDir.resolve("legacy-harmonize-flow").resolve("main.sjs").toFile());
        actual = IOUtils.toString(inputStream);
        inputStream.close();
        assertEquals(getResource("scaffolding/harmonize/sjs/main-legacy-1x.sjs"), actual);


        assertEquals(0, scaffolding.updateLegacyFlows(fromVersion, "my-fun-test").size());
    }

    @Test
    public void updateLegacyFlowsFrom2x() throws IOException, SAXException, ParserConfigurationException {
        String fromVersion = "2.0.0-rc.1";
        Scaffolding scaffolding = new Scaffolding(projectDir.toString(), stagingClient);
        assertEquals(0, scaffolding.updateLegacyFlows(fromVersion, "my-fun-test").size());

        Path inputDir = projectPath.resolve("plugins/entities/my-fun-test/input");
        Path harmonizeDir = projectPath.resolve("plugins/entities/my-fun-test/harmonize");
        FileUtils.copyDirectory(getResourceFile("scaffolding-test/legacy-input-flow"), inputDir.resolve("legacy-input-flow").toFile());
        FileUtils.copyDirectory(getResourceFile("scaffolding-test/legacy-harmonize-flow"), harmonizeDir.resolve("legacy-harmonize-flow").toFile());

        assertEquals(2, scaffolding.updateLegacyFlows(fromVersion, "my-fun-test").size());

        FileInputStream fis = new FileInputStream(inputDir.resolve("legacy-input-flow").resolve("legacy-input-flow.properties").toFile());
        Properties properties = new Properties();
        properties.load(fis);
        fis.close();

        assertEquals(4, properties.keySet().size());
        assertEquals(CodeFormat.JAVASCRIPT.toString(), properties.get("codeFormat"));
        assertEquals(DataFormat.JSON.toString(), properties.get("dataFormat"));
        assertEquals(CodeFormat.JAVASCRIPT.toString(), properties.get("mainCodeFormat"));
        assertEquals("main.sjs", properties.get("mainModule"));
        FileInputStream inputStream = new FileInputStream(inputDir.resolve("legacy-input-flow").resolve("main.sjs").toFile());
        String actual = IOUtils.toString(inputStream);
        inputStream.close();
        assertEquals(getResource("scaffolding/input/sjs/main-legacy.sjs"), actual);

        fis = new FileInputStream(harmonizeDir.resolve("legacy-harmonize-flow").resolve("legacy-harmonize-flow.properties").toFile());
        properties = new Properties();
        properties.load(fis);
        fis.close();

        assertEquals(6, properties.keySet().size());
        assertEquals(CodeFormat.JAVASCRIPT.toString(), properties.get("codeFormat"));
        assertEquals(DataFormat.JSON.toString(), properties.get("dataFormat"));
        assertEquals(CodeFormat.JAVASCRIPT.toString(), properties.get("collectorCodeFormat"));
        assertEquals("collector/collector.sjs", properties.get("collectorModule"));
        assertEquals(CodeFormat.JAVASCRIPT.toString(), properties.get("mainCodeFormat"));
        assertEquals("main.sjs", properties.get("mainModule"));
        inputStream = new FileInputStream(harmonizeDir.resolve("legacy-harmonize-flow").resolve("main.sjs").toFile());
        actual = IOUtils.toString(inputStream);
        inputStream.close();
        assertEquals(getResource("scaffolding/harmonize/sjs/main-legacy.sjs"), actual);

        assertEquals(0, scaffolding.updateLegacyFlows(fromVersion, "my-fun-test").size());
    }
}
