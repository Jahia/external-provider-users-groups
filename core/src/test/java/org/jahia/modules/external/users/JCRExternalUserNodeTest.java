package org.jahia.modules.external.users;

import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.users.impl.UserDataSource;
import org.jahia.services.content.JCRNodeWrapper;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JCRExternalUserNodeTest {

    private static final String USER_NAME = "alice";

    private UserGroupProvider userGroupProvider;
    private JCRExternalUserNode userNode;

    @Before
    public void setUp() {
        userGroupProvider = mock(UserGroupProvider.class);
        UserDataSource dataSource = mock(UserDataSource.class);
        when(dataSource.getUserGroupProvider()).thenReturn(userGroupProvider);
        ExternalContentStoreProvider provider = mock(ExternalContentStoreProvider.class);
        when(provider.getDataSource()).thenReturn(dataSource);
        JCRNodeWrapper node = mock(JCRNodeWrapper.class);
        when(node.getName()).thenReturn(USER_NAME);
        when(node.getProvider()).thenReturn(provider);
        userNode = new JCRExternalUserNode(node);
    }

    @Test
    public void aPasswordTheProviderAcceptsIsVerified() {
        doReturn(true).when(userGroupProvider).verifyPassword(USER_NAME, "secret");
        assertTrue(userNode.verifyPassword("secret"));
    }

    @Test
    public void aPasswordTheProviderRefusesIsNotVerified() {
        doReturn(false).when(userGroupProvider).verifyPassword(USER_NAME, "secret");
        assertFalse(userNode.verifyPassword("secret"));
    }

    @Test
    public void anEmptyPasswordIsNotVerified() {
        doReturn(true).when(userGroupProvider).verifyPassword(any(), any());
        assertFalse(userNode.verifyPassword(""));
        verify(userGroupProvider, never()).verifyPassword(any(), any());
    }

    @Test
    public void aNullPasswordIsNotVerified() {
        doReturn(true).when(userGroupProvider).verifyPassword(any(), any());
        assertFalse(userNode.verifyPassword(null));
        verify(userGroupProvider, never()).verifyPassword(any(), any());
    }
}
