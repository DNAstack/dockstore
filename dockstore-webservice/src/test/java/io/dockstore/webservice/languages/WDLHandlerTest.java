package io.dockstore.webservice.languages;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

import io.dockstore.webservice.CustomWebApplicationException;
import io.dockstore.webservice.core.Workflow;
import io.dropwizard.testing.ResourceHelpers;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

public class WDLHandlerTest {

    @Test
    public void getWorkflowContent() throws IOException {
        final WDLHandler wdlHandler = new WDLHandler();
        final Workflow workflow = new Workflow();
        workflow.setAuthor("Jane Doe");
        workflow.setDescription("A good description");
        workflow.setEmail("janedoe@example.org");

        final String goodWdl = FileUtils
                .readFileToString(new File(ResourceHelpers.resourceFilePath("valid_description_example.wdl")),
                        StandardCharsets.UTF_8);
        wdlHandler.parseWorkflowContent(workflow, "/foo.wdl", goodWdl, Collections.emptySet());
        Assert.assertEquals(workflow.getAuthor(), "Mr. Foo");
        Assert.assertEquals(workflow.getEmail(), "foo@foo.com");
        Assert.assertEquals(workflow.getDescription(),
                "This is a cool workflow trying another line \n## This is a header\n* First Bullet\n* Second bullet");

        final String invalidDescriptionWdl = FileUtils
                .readFileToString(new File(ResourceHelpers.resourceFilePath("invalid_description_example.wdl")),
                        StandardCharsets.UTF_8);
        wdlHandler.parseWorkflowContent(workflow, "/foo.wdl", invalidDescriptionWdl, Collections.emptySet());
        Assert.assertNull(workflow.getAuthor());
        Assert.assertNull(workflow.getEmail());
        Assert.assertEquals(WDLHandler.WDL_SYNTAX_ERROR, workflow.getDescription());

    }

    @Test
    public void testRecursiveImports() throws IOException {
        final File recursiveWdl = new File(ResourceHelpers.resourceFilePath("recursive.wdl"));

        final WDLHandler wdlHandler = new WDLHandler();
        String s = FileUtils.readFileToString(recursiveWdl, StandardCharsets.UTF_8);
        try {
            wdlHandler.checkForRecursiveHTTPImports(s, new HashSet<>());
            Assert.fail("Should've detected recursive import");
        } catch (CustomWebApplicationException e) {
            Assert.assertEquals("Error parsing workflow. You may have a recursive import.", e.getErrorMessage());
        }

        final File notRecursiveWdl = new File(ResourceHelpers.resourceFilePath("valid_description_example.wdl"));
        s = FileUtils.readFileToString(notRecursiveWdl, StandardCharsets.UTF_8);
        wdlHandler.checkForRecursiveHTTPImports(s, new HashSet<>());
    }
}