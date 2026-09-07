package org.jahia.modules.external.users.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * What the handler answers is asserted by {@link UserGroupProviderAdminFlowTest}; what CALLS it is
 * asserted here, because that decision lives in the flow definitions and the form views, where no
 * other test looks. Without this, exchanging the two resolvers between the two {@code evaluate}
 * lines, or reading a path from the request again, changes what the screen renders and leaves the
 * suite green.
 * <p>
 * Both definitions and all four forms are read, since this module keeps them as hand-maintained
 * copies: an edit that reaches one skin and misses the other is the failure this covers.
 */
public class UserGroupProviderFlowDefinitionTest {

    private static final String BASE = "/jnt_serverSettingsUserGroupProviders/html/";
    private static final String[] DEFINITIONS = {
            "serverSettingsUserGroupProviders.flow",
            "serverSettingsUserGroupProviders.settingsBootstrap3GoogleMaterialStyle.flow",
    };

    private static String resource(String path) {
        // The stream is read before the Scanner is built: constructing one over a null stream throws
        // where a missing resource should say which one.
        InputStream in = UserGroupProviderFlowDefinitionTest.class.getResourceAsStream(path);
        assertNotNull("missing resource " + path, in);
        try (Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name())) {
            return scanner.useDelimiter("\\A").next();
        }
    }

    /** The {@code evaluate} expressions of one view-state's {@code on-entry}, in order. */
    private static List<String> onEntryExpressions(String definition, String viewStateId) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Document document = factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(resource(BASE + definition + "/flow.xml").getBytes(StandardCharsets.UTF_8)));

        List<String> expressions = new ArrayList<>();
        NodeList viewStates = document.getElementsByTagName("view-state");
        for (int i = 0; i < viewStates.getLength(); i++) {
            Element viewState = (Element) viewStates.item(i);
            if (!viewStateId.equals(viewState.getAttribute("id"))) {
                continue;
            }
            NodeList entries = viewState.getElementsByTagName("on-entry");
            for (int j = 0; j < entries.getLength(); j++) {
                NodeList children = entries.item(j).getChildNodes();
                for (int k = 0; k < children.getLength(); k++) {
                    Node child = children.item(k);
                    if (child.getNodeType() == Node.ELEMENT_NODE && "evaluate".equals(child.getNodeName())) {
                        Element evaluate = (Element) child;
                        expressions.add(evaluate.getAttribute("result") + " = " + evaluate.getAttribute("expression"));
                    }
                }
            }
        }
        assertTrue(viewStateId + " has no on-entry in " + definition, !expressions.isEmpty());
        return expressions;
    }

    /**
     * Each form's view comes from the resolver for THAT form. Asserting the pair, per definition, is
     * what makes an exchange of the two resolvers visible: either one alone still renders a form.
     */
    @Test
    public void eachFormEntryCallsItsOwnResolver() throws Exception {
        for (String definition : DEFINITIONS) {
            List<String> create = onEntryExpressions(definition, "createProviderForm");
            assertTrue(definition + " must resolve the create view from the provider class",
                    create.contains("flashScope.createJSP = userGroupProviderHandler.resolveCreateJSP(requestParameters.providerClass)"));
            assertTrue(definition + " must resolve the create heading's name from the provider class",
                    create.contains("flashScope.providerName = userGroupProviderHandler.resolveProviderName(requestParameters.providerClass)"));
            assertTrue(definition + " must resolve the edit view from the provider class",
                    onEntryExpressions(definition, "editProviderForm").contains(
                            "flashScope.editJSP = userGroupProviderHandler.resolveEditJSP(requestParameters.providerClass)"));
        }
    }

    /** Nothing a form renders may be read back off the request — the point of resolving them. */
    @Test
    public void noFormEntryTakesWhatItRendersFromTheRequest() throws Exception {
        for (String definition : DEFINITIONS) {
            for (String viewState : new String[] {"createProviderForm", "editProviderForm", "deleteProviderForm"}) {
                for (String expression : onEntryExpressions(definition, viewState)) {
                    for (String rendered : new String[] {"createJSP", "editJSP", "providerName"}) {
                        assertFalse(definition + "/" + viewState + " reads " + rendered + " from the request: " + expression,
                                expression.contains("requestParameters." + rendered));
                    }
                }
            }
        }
    }

    /** The copies have to stay copies: an edit that reaches one skin and misses the other is a bug. */
    @Test
    public void theTwoFlowDefinitionsStayIdentical() {
        assertEquals("the two flow definitions have drifted apart",
                resource(BASE + DEFINITIONS[0] + "/flow.xml"), resource(BASE + DEFINITIONS[1] + "/flow.xml"));
    }

    /**
     * The forms include what was resolved, and only when something was: an unresolved view must not
     * reach {@code jsp:include} as an empty page attribute, and no form may carry a path back out.
     */
    @Test
    public void theFormsGuardTheIncludeAndCarryNoPath() {
        for (String definition : DEFINITIONS) {
            for (String form : new String[] {"createProviderForm", "editProviderForm"}) {
                String jsp = resource(BASE + definition + "/" + form + ".jsp");
                String rendered = form.startsWith("create") ? "createJSP" : "editJSP";
                // The include has to sit INSIDE the guard, and reformatting the view must not fail this:
                // what is asserted is the nesting, not the whitespace between the two tags.
                Pattern guarded = Pattern.compile("<c:if\\s+test=\"\\$\\{\\s*not empty " + rendered
                        + "\\s*\\}\">\\s*<jsp:include\\s+page=\"\\$\\{\\s*" + rendered
                        + "\\s*\\}\"\\s*/>\\s*</c:if>");
                assertTrue(definition + "/" + form + " must include its view only when one was resolved",
                        guarded.matcher(jsp).find());
                assertFalse(definition + "/" + form + " must not carry " + rendered + " back to the request",
                        jsp.contains("name=\"" + rendered + "\""));
            }
            String view = resource(BASE + definition + "/view.jsp");
            for (String rendered : new String[] {"createJSP", "editJSP"}) {
                assertFalse(definition + "/view.jsp must not hand out " + rendered,
                        view.contains("name=\"" + rendered + "\""));
            }
        }
    }
}
