package org.jahia.modules.external.users.admin;

import static org.jahia.modules.external.users.admin.UserGroupProviderAdminFlow.grantsAdministration;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
 */
public class UserGroupProviderAdminFlowTest {

    private static final String PROVIDER_KEY = "ldap.corporate";
    private static final String PROVIDER_CLASS = "org.jahia.services.usermanager.ldap.LDAPUserGroupProvider";
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
     * Studio renders a module's own definitions, and core's render conditions exempt it. A render context is
     * enough to reach the decision for this case: the exemption is answered before the main resource is read,
     * so no {@code Resource} is needed.
     */
    @Test
    public void aStudioRenderIsExempt() {
        RenderContext studio = mock(RenderContext.class);
        when(studio.getEditModeConfigName()).thenReturn("studiomode");

        assertTrue(handler.isAdministrationGranted(studio));
    }

    /** The exemption is that one mode and no other: any other mode falls through to the requirement. */
    @Test
    public void anyOtherModeIsNotExempt() {
        RenderContext editMode = mock(RenderContext.class);
        when(editMode.getEditModeConfigName()).thenReturn("editmode");

        assertFalse(handler.isAdministrationGranted(editMode));
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
