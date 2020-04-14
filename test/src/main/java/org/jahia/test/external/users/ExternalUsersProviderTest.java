/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.test.external.users;

import com.google.common.collect.Sets;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.*;
import org.jahia.services.content.decorator.JCRGroupNode;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.test.JahiaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Integration tests for the external users provider implementation.
 * 
 * @author Quentin Lamerand
 */
public class ExternalUsersProviderTest extends JahiaTestCase {

    private JahiaUserManagerService jahiaUserManagerService;
    private JahiaGroupManagerService jahiaGroupManagerService;

    @Before
    public void setUp() throws RepositoryException {
        jahiaUserManagerService = JahiaUserManagerService.getInstance();
        jahiaGroupManagerService = JahiaGroupManagerService.getInstance();
    }

    @Test
    public void testLookupAndMembership() throws RepositoryException {
        JCRUserNode tata = jahiaUserManagerService.lookupUser("tata");
        assertNotNull("User should exist", tata);
        JCRUserNode tete = jahiaUserManagerService.lookupUser("tete");
        assertNotNull("User should exist", tete);
        JCRUserNode titi = jahiaUserManagerService.lookupUser("titi");
        assertNotNull("User should exist", titi);

        JCRGroupNode toto = jahiaGroupManagerService.lookupGroup(null, "toto");
        assertNotNull(toto);
        JCRGroupNode tutu = jahiaGroupManagerService.lookupGroup(null, "tutu");
        assertNotNull(tutu);
        JCRGroupNode tyty = jahiaGroupManagerService.lookupGroup(null, "tyty");
        assertNotNull(tyty);

        assertEquals("toto should have tata, tete and tutu as members", Sets.newHashSet(tata, tete, tutu), new HashSet<JCRNodeWrapper>(toto.getMembers()));
        assertEquals("tutu should have titi as member", Sets.newHashSet(titi), new HashSet<JCRNodeWrapper>(tutu.getMembers()));
        assertEquals("tyty should have tete and titi as members", Sets.newHashSet(tete, titi), new HashSet<JCRNodeWrapper>(tyty.getMembers()));

        Set<String> membership = new HashSet<String>(jahiaGroupManagerService.getMembershipByPath(titi.getPath()));
        Set<String> expectedMembership = Sets.newHashSet(tutu.getPath(), toto.getPath(), tyty.getPath(), "/groups/users", "/groups/guest");
        boolean expectedMembershipReturned = membership.containsAll(expectedMembership);

        // not need anymore since acls implemented test, users are also in some privileged groups.
        /*membership.removeAll(expectedMembership);
        for (String remainingMembership : membership) {
            if (!remainingMembership.endsWith("site-users")) {
                expectedMembershipReturned = false;
                break;
            }
        }*/
        
        assertTrue("titi should be a member of tutu, toto, tyty, users and guests", expectedMembershipReturned);
    }

    @Test
    public void testPassword() throws RepositoryException {
        JCRUserNode tata = jahiaUserManagerService.lookupUser("tata");
        assertNotNull("User should exist", tata);
        assertTrue("User should be external", tata.getClass().getName().equals("org.jahia.modules.external.users.JCRExternalUserNode"));
        assertTrue(tata.verifyPassword("password"));
    }

    @Test
    public void testSearch() throws RepositoryException {
        JCRUserNode tata = jahiaUserManagerService.lookupUser("tata");
        JCRUserNode tete = jahiaUserManagerService.lookupUser("tete");
        JCRUserNode titi = jahiaUserManagerService.lookupUser("titi");

        Properties properties = new Properties();
        properties.put("username", "t*");
        Set<JCRUserNode> users = jahiaUserManagerService.searchUsers(properties);
        Set<JCRUserNode> expectedUsers = Sets.<JCRUserNode>newHashSet(tata, tete, titi);
        boolean expectedUsersReturned = users.containsAll(expectedUsers);
        users.removeAll(expectedUsers);
        Set<JCRUserNode> wrongUsers = new HashSet<JCRUserNode>();
        for (JCRUserNode remainingUser : users) {
            if (!remainingUser.getName().toLowerCase().startsWith("t")) {
                expectedUsersReturned = false;
                wrongUsers.add(remainingUser);
            }
        }
        assertTrue("'username=t*' search should return tata, tete and titi and no other users not having a usernmae with t: " + wrongUsers.toString(), expectedUsersReturned);

        JCRGroupNode toto = jahiaGroupManagerService.lookupGroup(null, "toto");
        JCRGroupNode tutu = jahiaGroupManagerService.lookupGroup(null, "tutu");
        JCRGroupNode tyty = jahiaGroupManagerService.lookupGroup(null, "tyty");

        properties = new Properties();
        properties.put("groupname", "t*");
        Set<JCRGroupNode> groups = jahiaGroupManagerService.searchGroups(null, properties);
        assertEquals("'groupname=t*' search should return toto, tutu and tyty", Sets.newHashSet(toto, tutu, tyty), groups);
    }

    @Test
    public void testExtension() throws RepositoryException {
        JCRUserNode tata = jahiaUserManagerService.lookupUser("tata");
        tata.setProperty("j:about", "I shot the sheriff");
        assertEquals("Property not updated", "I shot the sheriff", tata.getProperty("j:about").getString());
        boolean threwException = false;
        try {
            tata.setProperty("j:firstName", "Bob");
        } catch (UnsupportedRepositoryOperationException e) {
            threwException = true;
        }
        assertTrue("Setting a read-only property shouldn't be possible", threwException);
    }

    @Test
    public void testAcls() throws RepositoryException {
        JCRTemplate.getInstance().doExecuteWithSystemSession(
                new JCRCallback<String>() {
                    @Override
                    public String doInJCR(JCRSessionWrapper jcrSessionWrapper) throws RepositoryException {
                        // users need to be priviliged
                        jcrSessionWrapper.getNode("/sites/systemsite").grantRoles("u:tata", Collections.singleton("editor-in-chief"));
                        jcrSessionWrapper.save();
                        return null;
                    }
                });
        final String folderPath = JCRTemplate.getInstance().doExecute("tata", null, "default", null, new JCRCallback<String>() {
            @Override
            public String doInJCR(JCRSessionWrapper jcrSessionWrapper) throws RepositoryException {

                jcrSessionWrapper.getNode("/sites/systemsite").grantRoles("u:titi", Collections.singleton("editor"));
                jcrSessionWrapper.getNode("/sites/systemsite").grantRoles("u:tete", Collections.singleton("editor"));
                jcrSessionWrapper.getNode("/sites/systemsite").grantRoles("u:yaya", Collections.singleton("editor"));
                jcrSessionWrapper.save();

                JCRUserNode tata = jahiaUserManagerService.lookupUser("tata", jcrSessionWrapper);
                JCRNodeWrapper folder = tata.addNode("tata_folder", "jnt:folder");
                folder.grantRoles("u:titi", Collections.singleton("reader"));
                folder.grantRoles("u:tete", Collections.singleton("editor"));

                jcrSessionWrapper.save();
                return folder.getPath();
            }
        });

        JCRTemplate.getInstance().doExecuteWithSystemSession(
                new JCRCallback<String>() {
                    @Override
                    public String doInJCR(JCRSessionWrapper jcrSessionWrapper) throws RepositoryException {
                        // users need to be priviliged
                        JCRNodeWrapper folder2 = jcrSessionWrapper.getNode(folderPath).addNode("tata_folder2", "jnt:folder");
                        folder2.setAclInheritanceBreak(true);
                        folder2.grantRoles("u:yaya", Collections.singleton("owner"));
                        jcrSessionWrapper.save();
                        return null;
                    }
                });

        // triggered events put users in site privileged group
        assertTrue(jahiaGroupManagerService.isMember("tata", JahiaGroupManagerService.SITE_PRIVILEGED_GROUPNAME, "systemsite"));

        // tata is owner, should be able to jcr:write on first folder, but not second one because inheritance is broken
        JCRTemplate.getInstance().doExecute("tata", null, "default", null, new JCRCallback<String>() {
            @Override
            public String doInJCR(JCRSessionWrapper jcrSessionWrapper) throws RepositoryException {
                JCRUserNode tata = jahiaUserManagerService.lookupUser("tata", jcrSessionWrapper);
                assertTrue(checkPermission(jcrSessionWrapper, tata.getPath(), "jcr:write"));

                // check that is not possible to modify user acls (because of AccessControllable impl)
                boolean threwException = false;
                try {
                    tata.grantRoles("u:tete", Collections.singleton("owner"));
                } catch (UnsupportedRepositoryOperationException e) {
                    threwException = true;
                }
                assertTrue("Changing acl on external node that implement AccessControlable shouldn't be possible", threwException);
                assertFalse(checkPermission(jcrSessionWrapper, folderPath + "/tata_folder2" , "jcr:write"));
                assertFalse(checkPermission(jcrSessionWrapper, folderPath + "/tata_folder2" , "jcr:read"));
                return null;
            }
        });

        JCRTemplate.getInstance().doExecute("titi", null, "default", null, new JCRCallback<Object>() {
            @Override
            public Object doInJCR(JCRSessionWrapper jcrSessionWrapper) throws RepositoryException {
                assertTrue(checkPermission(jcrSessionWrapper, folderPath, "jcr:read_live"));
                assertFalse(checkPermission(jcrSessionWrapper, folderPath, "jcr:write"));
                assertFalse(checkPermission(jcrSessionWrapper, folderPath + "/tata_folder2", "jcr:write"));
                return null;
            }
        });

        JCRTemplate.getInstance().doExecute("yaya", null, "default", null, new JCRCallback<Object>() {
            @Override
            public Object doInJCR(JCRSessionWrapper jcrSessionWrapper) throws RepositoryException {
                assertTrue(checkPermission(jcrSessionWrapper, folderPath, "jcr:read"));
                assertFalse(checkPermission(jcrSessionWrapper, folderPath, "jcr:write"));
                assertTrue(checkPermission(jcrSessionWrapper, folderPath + "/tata_folder2", "jcr:write"));
                return null;
            }
        });

        JCRTemplate.getInstance().doExecute("tete", null, "default", null, new JCRCallback<Object>() {
            @Override
            public Object doInJCR(JCRSessionWrapper jcrSessionWrapper) throws RepositoryException {
                assertTrue(checkPermission(jcrSessionWrapper, folderPath, "jcr:write"));
                assertFalse(checkPermission(jcrSessionWrapper, folderPath + "/tata_folder2", "jcr:write"));
                return null;
            }
        });

        JCRTemplate.getInstance().doExecute("tata", null, "default", null, new JCRCallback<String>() {
            @Override
            public String doInJCR(JCRSessionWrapper jcrSessionWrapper) throws RepositoryException {
                JCRNodeWrapper folder = jcrSessionWrapper.getNode(folderPath);
                folder.denyRoles("u:tete", Collections.singleton("editor"));
                jcrSessionWrapper.save();
                return null;
            }
        });

        // Triggered event remove user from privileged group
        // todo: BACKLOG-5678 to fix the following failing test
        //assertFalse(jahiaGroupManagerService.isMember("tata", JahiaGroupManagerService.SITE_PRIVILEGED_GROUPNAME, "systemsite"));

        JCRTemplate.getInstance().doExecute("tete", null, "default", null, new JCRCallback<Object>() {
            @Override
            public Object doInJCR(JCRSessionWrapper jcrSessionWrapper) throws RepositoryException {
                assertFalse(checkPermission(jcrSessionWrapper, folderPath, "jcr:write"));
                return null;
            }
        });
    }

    public Boolean checkPermission(JCRSessionWrapper session, String path, String permission) throws RepositoryException {
        try {
            return session.getNode(path).hasPermission(permission);
        } catch (PathNotFoundException e) {
            return false;
        }
    }

    @After
    public void tearDown() throws Exception {
        JCRTemplate.getInstance().doExecuteWithSystemSession(
                new JCRCallback<String>() {
                    @Override
                    public String doInJCR(JCRSessionWrapper jcrSessionWrapper) throws RepositoryException {
                        if (jahiaUserManagerService.lookupUser("tata", jcrSessionWrapper).hasNode("tata_folder")) {
                            jahiaUserManagerService.lookupUser("tata", jcrSessionWrapper).getNode("tata_folder").remove();
                            jcrSessionWrapper.save();
                        }
                        return null;
                    }
                });
        JCRSessionFactory.getInstance().closeAllSessions();
    }


    @Test
    public void accesToUserNodeViaRestApi() throws Exception {
        login("tata", "password");
        try {
            JCRUserNode tata = jahiaUserManagerService.lookupUser("tata");
            checkAccess("/modules/api/jcr/v1/live/en/paths" + tata.getPath());
            checkAccess("/modules/api/jcr/v1/default/en/paths" + tata.getPath());

            checkAccess("/modules/api/jcr/v1/live/en/nodes/" + tata.getIdentifier());
            checkAccess("/modules/api/jcr/v1/default/en/nodes/" + tata.getIdentifier());

            checkAccess("/modules/api/jcr/v1/live/en/nodes/" + tata.getIdentifier() + "/properties");
            checkAccess("/modules/api/jcr/v1/default/en/nodes/" + tata.getIdentifier() + "/properties");
        } finally {
            logout();
        }
    }

    private void checkAccess(String url) throws IOException {
        String out = getAsText(url);
        assertFalse("Should have access to the URL: " + url,
                StringUtils.contains(out, "\"exception\":\"javax.jcr.PathNotFoundException\""));
    }

}
