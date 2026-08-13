package org.jahia.modules.external.users.admin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * Guards the shape of the user/group providers webflow: the JSP the create and edit forms include must be resolved
 * from the provider registry, never carried in the request.
 * <p>
 * The sink these tests protect lives in the flow definition and the JSPs rather than in Java, so it is asserted on the
 * resources themselves. Both skins carry the same forms and both are checked.
 */
public class UserGroupProviderFlowDefinitionTest {

    private static final String BASE = "/jnt_serverSettingsUserGroupProviders/html/";

    private static final String[] FLOWS = {
            BASE + "serverSettingsUserGroupProviders.flow/",
            BASE + "serverSettingsUserGroupProviders.settingsBootstrap3GoogleMaterialStyle.flow/"
    };

    @Test
    public void flowShouldNotBindTheIncludedJSPFromTheRequest() throws IOException {
        for (String flow : FLOWS) {
            String definition = read(flow + "flow.xml");
            assertFalse(flow + "flow.xml must not read the included JSP from the request",
                    definition.contains("requestParameters.createJSP"));
            assertFalse(flow + "flow.xml must not read the included JSP from the request",
                    definition.contains("requestParameters.editJSP"));
        }
    }

    @Test
    public void flowShouldResolveTheIncludedJSPFromTheRegistry() throws IOException {
        for (String flow : FLOWS) {
            String definition = read(flow + "flow.xml");
            assertTrue(flow + "flow.xml must resolve the create JSP from the provider class",
                    definition.contains("userGroupProviderHandler.resolveCreateJSP(requestParameters.providerClass)"));
            assertTrue(flow + "flow.xml must resolve the edit JSP from the provider class",
                    definition.contains("userGroupProviderHandler.resolveEditJSP(requestParameters.providerClass)"));
        }
    }

    @Test
    public void formsShouldNotRoundTripTheIncludedJSPThroughTheBrowser() throws IOException {
        for (String flow : FLOWS) {
            for (String page : new String[] { "view.jsp", "createProviderForm.jsp", "editProviderForm.jsp" }) {
                String markup = read(flow + page);
                assertFalse(flow + page + " must not submit the included JSP as a request parameter",
                        markup.contains("name=\"createJSP\""));
                assertFalse(flow + page + " must not submit the included JSP as a request parameter",
                        markup.contains("name=\"editJSP\""));
            }
        }
    }

    @Test
    public void formsShouldIncludeTheResolvedJSPOnlyWhenOneWasResolved() throws IOException {
        for (String flow : FLOWS) {
            String create = read(flow + "createProviderForm.jsp");
            assertTrue(flow + "createProviderForm.jsp must include the resolved JSP",
                    create.contains("<jsp:include page=\"${createJSP}\"/>"));
            assertTrue(flow + "createProviderForm.jsp must not include anything when no JSP was resolved",
                    create.contains("${not empty createJSP}"));

            String edit = read(flow + "editProviderForm.jsp");
            assertTrue(flow + "editProviderForm.jsp must include the resolved JSP",
                    edit.contains("<jsp:include page=\"${editJSP}\"/>"));
            assertTrue(flow + "editProviderForm.jsp must not include anything when no JSP was resolved",
                    edit.contains("${not empty editJSP}"));
        }
    }

    @Test
    public void formsShouldEscapeTheErrorTheyReportBack() throws IOException {
        // flow.xml puts the raw rootCauseException in flash scope, so whatever the caller managed to get into the
        // message must not reach the page as markup.
        for (String flow : FLOWS) {
            for (String page : new String[] { "createProviderForm.jsp", "editProviderForm.jsp", "deleteProviderForm.jsp" }) {
                String markup = read(flow + page);
                assertFalse(flow + page + " must not write the error out unescaped",
                        markup.contains(">${error}<"));
                assertTrue(flow + page + " must escape the error it reports back",
                        markup.contains("${fn:escapeXml(error)}"));
            }
        }
    }

    private static String read(String resource) throws IOException {
        try (InputStream in = UserGroupProviderFlowDefinitionTest.class.getResourceAsStream(resource)) {
            assertNotNull("Missing resource " + resource, in);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
