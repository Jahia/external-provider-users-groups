/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.external.users.impl;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.modules.external.ExternalQuery;
import org.jahia.modules.external.users.UserGroupProvider;
import org.jahia.modules.external.users.UserNotFoundException;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.services.usermanager.JahiaUserSplittingRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import java.util.*;

import static javax.jcr.security.Privilege.*;
import static org.jahia.api.Constants.EDIT_WORKSPACE;
import static org.jahia.api.Constants.LIVE_WORKSPACE;

/**
 * Data source implementation for retrieving users. 
 */
public class UserDataSource implements ExternalDataSource, ExternalDataSource.Searchable, ExternalDataSource.AccessControllable, ExternalDataSource.CanCheckAvailability, ExternalDataSource.Initializable {

    private static final String[] READ_PROVILEGES = new String[] {JCR_READ + "_" + EDIT_WORKSPACE, JCR_READ + "_" + LIVE_WORKSPACE};
    
    private static final String[] USER_FOLDER_PRIVILEGES = new String[] {
        JCR_READ + "_" + EDIT_WORKSPACE, JCR_READ + "_" + LIVE_WORKSPACE,
        JCR_WRITE + "_" + EDIT_WORKSPACE, JCR_WRITE + "_" + LIVE_WORKSPACE,
        JCR_ADD_CHILD_NODES + "_" + EDIT_WORKSPACE, JCR_ADD_CHILD_NODES + "_" + LIVE_WORKSPACE,
        JCR_REMOVE_CHILD_NODES + "_" + EDIT_WORKSPACE, JCR_REMOVE_CHILD_NODES + "_" + LIVE_WORKSPACE,
        "actions"
    };
    
    private static final String[] USER_SUBFOLDER_PROVILEGES = new String[] {
            JCR_READ + "_" + EDIT_WORKSPACE, JCR_READ + "_" + LIVE_WORKSPACE,
            JCR_WRITE + "_" + EDIT_WORKSPACE, JCR_WRITE + "_" + LIVE_WORKSPACE,
            JCR_REMOVE_NODE + "_" + EDIT_WORKSPACE, JCR_REMOVE_NODE + "_" + LIVE_WORKSPACE,
            JCR_ADD_CHILD_NODES + "_" + EDIT_WORKSPACE, JCR_ADD_CHILD_NODES + "_" + LIVE_WORKSPACE,
            JCR_REMOVE_CHILD_NODES + "_" + EDIT_WORKSPACE, JCR_REMOVE_CHILD_NODES + "_" + LIVE_WORKSPACE,
            "actions"
    };
    
    private static Logger logger = LoggerFactory.getLogger(UserDataSource.class);
    
    public static final HashSet<String> SUPPORTED_NODE_TYPES = new HashSet<String>(Arrays.asList("jnt:externalUser", "jnt:usersFolder"));
    
    private JahiaUserManagerService jahiaUserManagerService;

    private UserGroupProvider userGroupProvider;

    private ExternalContentStoreProvider contentStoreProvider;

    @Override
    public void start() {
        if (userGroupProvider instanceof Initializable) {
            ((Initializable) userGroupProvider).start();
        }
        jahiaUserManagerService.clearNonExistingUsersCache();
    }

    @Override
    public void stop() {
        if (userGroupProvider instanceof Initializable) {
            ((Initializable) userGroupProvider).stop();
        }
    }

    @Override
    public List<String> getChildren(String path) throws RepositoryException {
        if (path == null || path.indexOf('/') == -1) {
            throw new PathNotFoundException(path);
        }
        return Collections.emptyList();
    }

    @Override
    public ExternalData getItemByIdentifier(String identifier) throws ItemNotFoundException {
        if (identifier.startsWith("/")) {
            try {
                return getItemByPath(identifier);
            } catch (PathNotFoundException e) {
                throw new ItemNotFoundException(identifier, e);
            }
        }
        throw new ItemNotFoundException(identifier);
    }

    @Override
    public ExternalData getItemByPath(String path) throws PathNotFoundException {
        if (path == null || path.indexOf('/') == -1) {
            throw new PathNotFoundException(path);
        }
        if ("/".equals(path)) {
            return new ExternalData(path, path, "jnt:usersFolder", new HashMap<String, String[]>());
        }
        String[] pathSegments = StringUtils.split(path, '/');
        JahiaUserSplittingRule userSplittingRule = jahiaUserManagerService.getUserSplittingRule();
        if (pathSegments.length <= userSplittingRule.getNumberOfSegments()) {
            if (pathSegments[pathSegments.length - 1].length() == 2) { // split folder names are two characters long
                return new ExternalData(path, path, "jnt:usersFolder", new HashMap<String, String[]>());
            }
            throw new PathNotFoundException(path);
        }
        if (pathSegments.length >  userSplittingRule.getNumberOfSegments() + 1) { // number of split folders + user name
            throw new PathNotFoundException(path);
        }
        try {
            ExternalData data = getUserData(userGroupProvider.getUser(pathSegments[pathSegments.length - 1]));
            if (!path.equals(data.getPath())) {
                throw new PathNotFoundException("Cannot find user " + path);
            }
            return data;
        } catch (UserNotFoundException e) {
            throw new PathNotFoundException("Cannot find user " + path, e);
        }
    }

    @Override
    public Set<String> getSupportedNodeTypes() {
        return SUPPORTED_NODE_TYPES;
    }

    @Override
    public boolean isSupportsHierarchicalIdentifiers() {
        return true;
    }

    @Override
    public boolean isSupportsUuid() {
        return false;
    }

    @Override
    public boolean itemExists(String path) {
        try {
            getItemByPath(path);
        } catch (PathNotFoundException e) {
            return false;
        }
        return true;
    }

    @Override
    public List<String> search(ExternalQuery externalQuery) throws RepositoryException {
        Properties searchCriteria = new Properties();
        boolean hasOrConstraints = SearchCriteriaHelper.fillCriteriaFromConstraint(externalQuery.getConstraint(), searchCriteria, "username");
        searchCriteria.remove("jcr:language");
        // abort the search if we are only looking for internal users.
        if(searchCriteria.containsKey(JCRUserNode.J_EXTERNAL) && !Boolean.valueOf(searchCriteria.getProperty(JCRUserNode.J_EXTERNAL))){
            return Collections.emptyList();
        }
        if (searchCriteria.size() > 1 && !hasOrConstraints) {
            searchCriteria.put(JahiaUserManagerService.MULTI_CRITERIA_SEARCH_OPERATION, "and");
        }
        List<String> result = new ArrayList<String>();
        try {
            JahiaUserSplittingRule userSplittingRule = jahiaUserManagerService.getUserSplittingRule();
            for (String userName : userGroupProvider.searchUsers(searchCriteria, externalQuery.getOffset(), externalQuery.getLimit())) {
                result.add(userSplittingRule.getRelativePathForUsername(userName));
            }
        } catch (Exception e) {
            logger.error("Error while executing query {} on provider {}, issue {}",new Object[]{externalQuery,userGroupProvider,e.getMessage()});
            if(logger.isDebugEnabled()){
                logger.debug(e.getMessage(),e);
            }
        }
        return result;
    }

    private ExternalData getUserData(JahiaUser user) {
        String path = jahiaUserManagerService.getUserSplittingRule().getRelativePathForUsername(user.getName());
        Map<String,String[]> properties = new HashMap<String, String[]>();
        Properties userProperties = user.getProperties();
        for (Object key : userProperties.keySet()) {
            Object value = userProperties.get(key);
            if (value instanceof String[]) {
                properties.put((String) key, (String[]) value);
            } else {
                properties.put((String) key, new String[]{(String) value});
            }
        }
        properties.put("j:external", new String[]{"true"});
        properties.put("j:externalSource", new String[]{StringUtils.removeEnd(contentStoreProvider.getKey(), ".users")});
        return new ExternalData(path, path, "jnt:externalUser", properties);
    }

    public void setJahiaUserManagerService(JahiaUserManagerService jahiaUserManagerService) {
        this.jahiaUserManagerService = jahiaUserManagerService;
    }

    public UserGroupProvider getUserGroupProvider() {
        return userGroupProvider;
    }

    public void setUserGroupProvider(UserGroupProvider userGroupProvider) {
        this.userGroupProvider = userGroupProvider;
    }

    public ExternalContentStoreProvider getContentStoreProvider() {
        return contentStoreProvider;
    }

    public void setContentStoreProvider(ExternalContentStoreProvider contentStoreProvider) {
        this.contentStoreProvider = contentStoreProvider;
    }

    @Override
    public String[] getPrivilegesNames(String username, String path) {
        if (path.contains("/")) {
            String[] pathSegments = StringUtils.split(path, '/');
            JahiaUserSplittingRule userSplittingRule = jahiaUserManagerService.getUserSplittingRule();
            if (pathSegments.length == userSplittingRule.getNumberOfSegments() + 1 // number of split folders + user name
                    && username.equals(pathSegments[userSplittingRule.getNumberOfSegments()])) {
                return USER_FOLDER_PRIVILEGES;
            }
            if (pathSegments.length > userSplittingRule.getNumberOfSegments() + 1 // user subfolder
                    && username.equals(pathSegments[userSplittingRule.getNumberOfSegments()])) {
                return USER_SUBFOLDER_PROVILEGES;
            }
        }
        return READ_PROVILEGES;
    }

    @Override
    public boolean isAvailable() throws RepositoryException {
        return userGroupProvider.isAvailable();
    }
}
