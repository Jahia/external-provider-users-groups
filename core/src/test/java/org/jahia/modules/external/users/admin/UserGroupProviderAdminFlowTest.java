package org.jahia.modules.external.users.admin;

import static org.jahia.modules.external.users.admin.UserGroupProviderAdminFlow.declaredBy;
import static org.jahia.modules.external.users.admin.UserGroupProviderAdminFlow.grantsAdministration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jahia.modules.external.users.ExternalUserGroupService;
import org.jahia.modules.external.users.UserGroupProviderConfiguration;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.usermanager.JahiaUser;
import org.junit.Before;
import org.junit.Test;
import org.springframework.binding.message.MessageContext;
import org.springframework.binding.message.MessageResolver;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.core.collection.LocalParameterMap;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.core.collection.ParameterMap;

import java.util.Collections;
import java.util.Map;

/**
 * The screen's operations are served to a caller who administers the server, and to no one else.
 * <p>
 * The transition cases below pass no render context, which is the fail-closed input, and they wire no
 * {@code ExternalUserGroupService} into the handler on purpose: a refusal has to be decided before anything
 * reaches that service, so each of them completes only while that ordering holds. Were the check to move after
 * the service call, they would fail on the unset field rather than pass.
 * <p>
 * The decision is exercised two ways. {@link UserGroupProviderAdminFlow#grantsAdministration} is called
 * directly for the truth table on the node. The public methods are driven through a mocked
 * {@code RenderContext} to cover the mapping from that context to the node and caller — the step a direct
 * call skips. Mocking {@code Resource} pulls in Guava (its class initialiser needs it), which is why the
 * test scope carries that dependency.
 * <p>
 * The view-resolution cases are the other half, and they are the ones that DO wire a provider service: each
 * form's view has to come from the registered configuration of the provider class the request names, so the
 * cases assert both views against it and assert what an unregistered class, a null class and an absent
 * registry resolve to.
 */
public class UserGroupProviderAdminFlowTest {

    private static final String PROVIDER_KEY = "ldap.corporate";
    private static final String PROVIDER_CLASS = "org.jahia.services.usermanager.ldap.LDAPUserGroupProvider";
    // Two paths that differ, and neither is a real module's: what matters is that the wrong one
    // would be visible. A real provider module may well declare the SAME view for both forms — the
    // ldap module does — which is exactly the case that would hide an exchange of the two getters.
    private static final String PROVIDER_NAME = "Example directory";
    private static final String CREATE_VIEW = "/modules/example/createProvider.jsp";
    private static final String EDIT_VIEW = "/modules/example/editProvider.jsp";
    private static final String NODE_PATH = "/sites/example/home/main/providers";

    private UserGroupProviderAdminFlow handler;
    private MessageContext messages;

    /** A node that answers {@code granted} to the permission this screen requires. */
    private static JCRNodeWrapper node(boolean granted) {
        JCRNodeWrapper node = mock(JCRNodeWrapper.class);
        when(node.hasPermission("adminUsers")).thenReturn(granted);
        when(node.getPath()).thenReturn(NODE_PATH);
        return node;
    }

    /** A render of the given mode, whose main resource is the given node. */
    private static RenderContext renderContext(String editModeConfigName, JCRNodeWrapper mainNode) {
        Resource mainResource = mock(Resource.class);
        when(mainResource.getNode()).thenReturn(mainNode);
        JahiaUser user = mock(JahiaUser.class);
        when(user.getName()).thenReturn("a template developer");
        RenderContext renderContext = mock(RenderContext.class);
        when(renderContext.getEditModeConfigName()).thenReturn(editModeConfigName);
        when(renderContext.getMainResource()).thenReturn(mainResource);
        when(renderContext.getUser()).thenReturn(user);
        return renderContext;
    }

    private static ParameterMap parameters() {
        return new LocalParameterMap(Collections.singletonMap("providerClass", PROVIDER_CLASS));
    }

    private static MutableAttributeMap<Object> flashScope() {
        return new LocalAttributeMap<>();
    }

    @Before
    public void setUp() {
        handler = new UserGroupProviderAdminFlow();
        messages = mock(MessageContext.class);
    }

    /** Registers one provider configuration that declares a view for each form, and supports both. */
    private Map<String, UserGroupProviderConfiguration> registered() {
        return registered(true, true, true);
    }

    /** The same, with each operation's support flag chosen — the flags the list reads before it offers a button. */
    private Map<String, UserGroupProviderConfiguration> registered(boolean createSupported, boolean editSupported,
            boolean deleteSupported) {
        UserGroupProviderConfiguration configuration = mock(UserGroupProviderConfiguration.class);
        when(configuration.isCreateSupported()).thenReturn(createSupported);
        when(configuration.isEditSupported()).thenReturn(editSupported);
        when(configuration.isDeleteSupported()).thenReturn(deleteSupported);
        when(configuration.getName()).thenReturn(PROVIDER_NAME);
        when(configuration.getCreateJSP()).thenReturn(CREATE_VIEW);
        when(configuration.getEditJSP()).thenReturn(EDIT_VIEW);
        Map<String, UserGroupProviderConfiguration> configurations =
                Collections.singletonMap(PROVIDER_CLASS, configuration);
        ExternalUserGroupService service = mock(ExternalUserGroupService.class);
        when(service.getProviderConfigurations()).thenReturn(configurations);
        handler.setExternalUserGroupService(service);
        return configurations;
    }

    /**
     * Everything the forms render about a provider kind — each form's view, and the name the create
     * heading shows — is what that provider class's own registered configuration declares. All three are
     * asserted, so exchanging any two of the getters turns the suite red: the values differ by one word
     * and the wrong one still renders a form.
     */
    @Test
    public void whatTheFormsRenderComesFromTheRegisteredConfiguration() {
        registered();

        assertEquals(CREATE_VIEW, handler.resolveCreateJSP(PROVIDER_CLASS));
        assertEquals(EDIT_VIEW, handler.resolveEditJSP(PROVIDER_CLASS));
        assertEquals(PROVIDER_NAME, handler.resolveProviderName(PROVIDER_CLASS));
    }

    /**
     * A provider class nothing registers resolves to no view at all. That is the case a request naming
     * its own path used to reach, and the forms render no fields rather than including what it named.
     */
    @Test
    public void aProviderClassNothingRegistersResolvesToNoView() {
        registered();

        assertNull(handler.resolveCreateJSP("org.example.NotRegistered"));
        assertNull(handler.resolveEditJSP("org.example.NotRegistered"));
        assertNull(handler.resolveProviderName("org.example.NotRegistered"));
        assertNull(handler.resolveCreateJSP(null));
        assertNull(handler.resolveEditJSP(null));
        assertNull(handler.resolveProviderName(null));
    }

    /**
     * A kind that declares a view it does not support resolves to none, so a resolver answers for exactly
     * the kinds the list that leads to it offers a button for. The name still resolves: it is the heading's
     * label, not a form, and the heading has its own fallback.
     */
    @Test
    public void aViewTheKindDoesNotSupportResolvesToNone() {
        registered(false, false, false);

        assertNull(handler.resolveCreateJSP(PROVIDER_CLASS));
        assertNull(handler.resolveEditJSP(PROVIDER_CLASS));
        assertEquals(PROVIDER_NAME, handler.resolveProviderName(PROVIDER_CLASS));
    }

    /**
     * The three transitions that WRITE refuse a kind nothing registers, rather than dereferencing the
     * configuration they did not find. Driven with no provider service reachable for that class, so each
     * case passes only while the refusal precedes the call.
     */
    @Test
    public void aWriteNamingAnUnregisteredKindIsRefused() throws Exception {
        registered();
        ParameterMap unregistered = new LocalParameterMap(
                Collections.singletonMap("providerClass", "org.example.NotRegistered"));

        handler.createProvider(unregistered, flashScope(), messages, renderContext("editmode", node(true)));
        handler.editProvider(unregistered, flashScope(), messages, renderContext("editmode", node(true)));
        handler.deleteProvider(PROVIDER_KEY, "org.example.NotRegistered", flashScope(), messages,
                renderContext("editmode", node(true)));

        verify(messages, times(3)).addMessage(any(MessageResolver.class));
    }

    /**
     * A write is refused when the kind is registered and says it does not offer that operation — the same
     * flags the resolvers read, so the two halves answer alike. The configuration is asserted untouched,
     * which is the part that matters: the refusal has to precede the call, not follow it.
     */
    @Test
    public void aWriteTheKindDoesNotOfferIsRefused() throws Exception {
        UserGroupProviderConfiguration configuration = registered(false, false, false).get(PROVIDER_CLASS);

        handler.createProvider(parameters(), flashScope(), messages, renderContext("editmode", node(true)));
        handler.editProvider(parameters(), flashScope(), messages, renderContext("editmode", node(true)));
        handler.deleteProvider(PROVIDER_KEY, PROVIDER_CLASS, flashScope(), messages,
                renderContext("editmode", node(true)));

        verify(messages, times(3)).addMessage(any(MessageResolver.class));
        verify(configuration, never()).create(any(), any());
        verify(configuration, never()).edit(any(), any(), any());
        verify(configuration, never()).delete(any(), any());
    }

    /** The lookup itself, including the inputs the flow can hand it before anything is registered. */
    @Test
    public void theLookupFailsClosedOnEveryAbsentInput() {
        Map<String, UserGroupProviderConfiguration> configurations = registered();

        assertEquals(configurations.get(PROVIDER_CLASS), declaredBy(configurations, PROVIDER_CLASS));
        assertNull(declaredBy(configurations, "org.example.NotRegistered"));
        assertNull(declaredBy(configurations, null));
        assertNull(declaredBy(null, PROVIDER_CLASS));
        assertNull(declaredBy(Collections.emptyMap(), PROVIDER_CLASS));
    }

    @Test
    public void aCallerHoldingThePermissionIsGranted() {
        assertTrue(grantsAdministration(node(true), "an administrator"));
    }

    @Test
    public void aCallerNotHoldingThePermissionIsRefused() {
        assertFalse(grantsAdministration(node(false), "an editor"));
    }

    @Test
    public void noNodeToEvaluateAgainstFailsClosed() {
        assertFalse(grantsAdministration(null, "an administrator"));
    }

    @Test
    public void anUnnamedCallerIsStillDecidedOnThePermission() {
        assertTrue(grantsAdministration(node(true), null));
        assertFalse(grantsAdministration(node(false), null));
    }

    /**
     * Studio renders a module's own definitions, and core's render conditions exempt it, so the screen lists
     * what it has where a template developer places it. A render context is enough to reach the decision for
     * this case: the exemption is answered before the main resource is read, so no {@code Resource} is needed.
     */
    @Test
    public void aStudioRenderStillListsWhatTheScreenHas() {
        RenderContext studio = mock(RenderContext.class);
        when(studio.getEditModeConfigName()).thenReturn("studiomode");

        assertTrue(handler.isInventoryReadable(studio));
    }

    /** The exemption is that one mode and no other: any other mode lists on the requirement alone. */
    @Test
    public void anyOtherModeListsOnTheRequirementAlone() {
        RenderContext editMode = mock(RenderContext.class);
        when(editMode.getEditModeConfigName()).thenReturn("editmode");

        assertFalse(handler.isInventoryReadable(editMode));

        assertTrue(handler.isInventoryReadable(renderContext("editmode", node(true))));
    }

    /**
     * The exemption stops at what the screen lists. A Studio render is admitted on a permission that says
     * nothing about administering identity providers, so the operations are decided on the requirement in
     * that mode as in any other. Widening the exemption back over this method turns the second assertion red.
     */
    @Test
    public void aStudioRenderIsStillDecidedOnTheRequirement() {
        assertTrue(handler.isAdministrationGranted(renderContext("studiomode", node(true))));

        assertFalse(handler.isAdministrationGranted(renderContext("studiomode", node(false))));
    }

    /**
     * The five operations in a Studio render, with no {@code ExternalUserGroupService} wired in: each is
     * refused, and each refusal is decided before anything reaches that service. Exempting Studio here again
     * turns this red on the unset field rather than on the assertion.
     */
    @Test
    public void aStudioRenderCarriesNoneOfTheOperations() throws Exception {
        RenderContext studio = renderContext("studiomode", node(false));

        handler.createProvider(parameters(), flashScope(), messages, studio);
        handler.editProvider(parameters(), flashScope(), messages, studio);
        handler.deleteProvider(PROVIDER_KEY, PROVIDER_CLASS, flashScope(), messages, studio);
        handler.suspendProvider(PROVIDER_KEY, messages, studio);
        handler.resumeProvider(PROVIDER_KEY, messages, studio);

        verify(messages, times(5)).addMessage(any(MessageResolver.class));
    }

    /**
     * Covers the mapping a direct {@code grantsAdministration} call skips: render context to main-resource
     * node to caller. This is the case a deny-all mutation of {@code isAdministrationGranted} — pinning the
     * node to null — makes fail, which the direct-call tests do not catch.
     */
    @Test
    public void aGrantedCallerReachesTheScreen() {
        JCRNodeWrapper node = mock(JCRNodeWrapper.class);
        when(node.hasPermission("adminUsers")).thenReturn(true);
        Resource mainResource = mock(Resource.class);
        when(mainResource.getNode()).thenReturn(node);
        JahiaUser user = mock(JahiaUser.class);
        when(user.getName()).thenReturn("an administrator");
        RenderContext renderContext = mock(RenderContext.class);
        when(renderContext.getEditModeConfigName()).thenReturn("editmode");
        when(renderContext.getMainResource()).thenReturn(mainResource);
        when(renderContext.getUser()).thenReturn(user);

        assertTrue(handler.isAdministrationGranted(renderContext));
    }

    @Test
    public void aRefusedCallerDoesNotReachTheScreen() {
        JCRNodeWrapper node = mock(JCRNodeWrapper.class);
        when(node.hasPermission("adminUsers")).thenReturn(false);
        when(node.getPath()).thenReturn(NODE_PATH);
        Resource mainResource = mock(Resource.class);
        when(mainResource.getNode()).thenReturn(node);
        JahiaUser user = mock(JahiaUser.class);
        when(user.getName()).thenReturn("an editor");
        RenderContext renderContext = mock(RenderContext.class);
        when(renderContext.getEditModeConfigName()).thenReturn("editmode");
        when(renderContext.getMainResource()).thenReturn(mainResource);
        when(renderContext.getUser()).thenReturn(user);

        assertFalse(handler.isAdministrationGranted(renderContext));
    }

    /**
     * The requirement is evaluated on the same resource {@code TemplatePermissionCheckFilter} uses — the AJAX
     * resource when the render has one — so the two enforcement points cannot disagree on an AJAX render.
     * Here the caller holds the permission on the AJAX node but not the main node; using the main node would
     * refuse a caller the template admitted.
     */
    @Test
    public void theAjaxResourceIsPreferredOverTheMainResource() {
        JCRNodeWrapper ajaxNode = mock(JCRNodeWrapper.class);
        when(ajaxNode.hasPermission("adminUsers")).thenReturn(true);
        Resource ajaxResource = mock(Resource.class);
        when(ajaxResource.getNode()).thenReturn(ajaxNode);

        JCRNodeWrapper mainNode = mock(JCRNodeWrapper.class);
        when(mainNode.hasPermission("adminUsers")).thenReturn(false);
        when(mainNode.getPath()).thenReturn(NODE_PATH);
        Resource mainResource = mock(Resource.class);
        when(mainResource.getNode()).thenReturn(mainNode);

        JahiaUser user = mock(JahiaUser.class);
        when(user.getName()).thenReturn("an administrator");
        RenderContext renderContext = mock(RenderContext.class);
        when(renderContext.getEditModeConfigName()).thenReturn("editmode");
        when(renderContext.getAjaxResource()).thenReturn(ajaxResource);
        when(renderContext.getMainResource()).thenReturn(mainResource);
        when(renderContext.getUser()).thenReturn(user);

        assertTrue(handler.isAdministrationGranted(renderContext));
    }

    @Test
    public void noRenderContextFailsClosed() {
        assertFalse(handler.isAdministrationGranted(null));
    }

    @Test
    public void noRenderContextDisclosesNoProviderInventory() {
        assertTrue(handler.getUserGroupProviders(null).isEmpty());
        assertTrue(handler.getCreateConfigurations(null).isEmpty());
    }

    @Test
    public void aRefusedCreateNeverReachesTheProviderConfiguration() throws Exception {
        handler.createProvider(parameters(), flashScope(), messages, null);

        verify(messages).addMessage(any(MessageResolver.class));
    }

    @Test
    public void aRefusedEditNeverReachesTheProviderConfiguration() throws Exception {
        handler.editProvider(parameters(), flashScope(), messages, null);

        verify(messages).addMessage(any(MessageResolver.class));
    }

    @Test
    public void aRefusedDeleteNeverReachesTheProviderConfiguration() throws Exception {
        handler.deleteProvider(PROVIDER_KEY, PROVIDER_CLASS, flashScope(), messages, null);

        verify(messages).addMessage(any(MessageResolver.class));
    }

    @Test
    public void aRefusedSuspendNeverReachesTheProviderRegistry() {
        handler.suspendProvider(PROVIDER_KEY, messages, null);

        verify(messages).addMessage(any(MessageResolver.class));
    }

    @Test
    public void aRefusedResumeNeverReachesTheProviderRegistry() throws Exception {
        handler.resumeProvider(PROVIDER_KEY, messages, null);

        verify(messages).addMessage(any(MessageResolver.class));
    }
}
