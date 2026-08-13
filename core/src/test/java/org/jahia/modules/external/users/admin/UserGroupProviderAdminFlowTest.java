package org.jahia.modules.external.users.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.jahia.modules.external.users.ExternalUserGroupService;
import org.jahia.modules.external.users.UserGroupProviderConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests that the JSP included by the create and edit forms is resolved from the provider registry and can never be
 * chosen by the caller.
 */
@RunWith(MockitoJUnitRunner.class)
public class UserGroupProviderAdminFlowTest {

    private static final String LDAP_CLASS = "org.jahia.services.usermanager.ldap.LDAPUserGroupProvider";

    private static final String LDAP_CREATE_JSP = "/modules/ldap/userGroupProviderCreate.jsp";

    private static final String LDAP_EDIT_JSP = "/modules/ldap/userGroupProviderEdit.jsp";

    /** The path SEC-289 proved could be included when the flow trusted the request parameter. */
    private static final String ATTACKER_JSP = "/modules/tools/jcrConsole.jsp";

    private final Map<String, UserGroupProviderConfiguration> configurations = new HashMap<>();

    @Mock
    private ExternalUserGroupService externalUserGroupService;

    @Mock
    private UserGroupProviderConfiguration ldapConfiguration;

    @InjectMocks
    private UserGroupProviderAdminFlow handler;

    @Before
    public void setUp() {
        configurations.put(LDAP_CLASS, ldapConfiguration);
        lenient().when(externalUserGroupService.getProviderConfigurations()).thenReturn(configurations);
    }

    @Test
    public void shouldResolveCreateJSPFromTheRegistry() {
        when(ldapConfiguration.isCreateSupported()).thenReturn(true);
        when(ldapConfiguration.getCreateJSP()).thenReturn(LDAP_CREATE_JSP);

        assertEquals(LDAP_CREATE_JSP, handler.resolveCreateJSP(LDAP_CLASS));
    }

    @Test
    public void shouldResolveEditJSPFromTheRegistry() {
        when(ldapConfiguration.isEditSupported()).thenReturn(true);
        when(ldapConfiguration.getEditJSP()).thenReturn(LDAP_EDIT_JSP);

        assertEquals(LDAP_EDIT_JSP, handler.resolveEditJSP(LDAP_CLASS));
    }

    @Test
    public void shouldNotResolveAJSPPathSuppliedByTheCaller() {
        // SEC-289: the caller used to pass the JSP path itself. It is now only ever a registry key, and a path is not
        // a registered provider class, so nothing is included.
        assertNull(handler.resolveCreateJSP(ATTACKER_JSP));
        assertNull(handler.resolveEditJSP(ATTACKER_JSP));
        assertNull(handler.resolveCreateJSP("/WEB-INF/web.xml"));
        assertNull(handler.resolveEditJSP("/WEB-INF/web.xml"));
    }

    @Test
    public void shouldNotResolveAnUnregisteredProviderClass() {
        assertNull(handler.resolveCreateJSP("com.example.NotRegistered"));
        assertNull(handler.resolveEditJSP("com.example.NotRegistered"));
    }

    @Test
    public void shouldNotResolveANullProviderClass() {
        assertNull(handler.resolveCreateJSP(null));
        assertNull(handler.resolveEditJSP(null));
    }

    @Test
    public void shouldNotResolveCreateJSPWhenCreationIsNotSupported() {
        when(ldapConfiguration.isCreateSupported()).thenReturn(false);

        assertNull(handler.resolveCreateJSP(LDAP_CLASS));
    }

    @Test
    public void shouldNotResolveEditJSPWhenEditionIsNotSupported() {
        when(ldapConfiguration.isEditSupported()).thenReturn(false);

        assertNull(handler.resolveEditJSP(LDAP_CLASS));
    }
}
