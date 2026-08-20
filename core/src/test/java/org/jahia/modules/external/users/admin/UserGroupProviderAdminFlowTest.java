package org.jahia.modules.external.users.admin;

import static org.jahia.modules.external.users.admin.UserGroupProviderAdminFlow.grantsAdministration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.render.RenderContext;
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
 * The permission decision is exercised through {@link UserGroupProviderAdminFlow#grantsAdministration}, on the
 * node it is evaluated against. Going through a {@code RenderContext} instead would mean constructing a
 * {@code Resource}, whose static initialisation needs a running Jahia.
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

    /**
     * The requirement is the screen's own permission, not the aggregate that implies it: a role may hold
     * {@code adminUsers} without holding {@code admin}, and the template admits such a role.
     */
    @Test
    public void aCallerHoldingOnlyTheAggregateNameIsNotWhatIsAskedFor() {
        JCRNodeWrapper aggregateOnly = mock(JCRNodeWrapper.class);
        when(aggregateOnly.hasPermission("admin")).thenReturn(true);
        when(aggregateOnly.hasPermission("adminUsers")).thenReturn(false);
        when(aggregateOnly.getPath()).thenReturn(NODE_PATH);

        assertFalse(grantsAdministration(aggregateOnly, "a caller the template would refuse"));
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

        assertEquals(PROVIDER_KEY, handler.resolveProviderKey(PROVIDER_KEY, studio));
    }

    /** The exemption is that one mode and no other: any other mode falls through to the requirement. */
    @Test
    public void anyOtherModeIsNotExempt() {
        RenderContext editMode = mock(RenderContext.class);
        when(editMode.getEditModeConfigName()).thenReturn("editmode");

        assertNull(handler.resolveProviderKey(PROVIDER_KEY, editMode));
    }

    @Test
    public void noRenderContextLeavesTheFormsWithNoProviderToLookUp() {
        assertNull(handler.resolveProviderKey(PROVIDER_KEY, null));
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
