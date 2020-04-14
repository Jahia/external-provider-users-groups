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
package org.jahia.modules.external.users.impl;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.ExternalData;
import org.jahia.modules.external.ExternalDataSource;
import org.jahia.modules.external.ExternalQuery;
import org.jahia.modules.external.users.GroupNotFoundException;
import org.jahia.modules.external.users.Member;
import org.jahia.modules.external.users.UserGroupProvider;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.usermanager.JahiaGroup;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import java.util.*;

/**
 * Data source implementation for retrieving groups.
 */
public class GroupDataSource implements ExternalDataSource, ExternalDataSource.Searchable, ExternalDataSource.Referenceable, ExternalDataSource.CanCheckAvailability, ExternalDataSource.Initializable {

    private static final Logger logger = LoggerFactory.getLogger(GroupDataSource.class);

    private static final String MEMBERS_SESSION_VAR = "membersByGroupName";

    private static final String MEMBER_REF_ATTR = "j:member";
    
    private static final String MEMBERS_ROOT_NAME = "j:members";
    
    public static final HashSet<String> SUPPORTED_NODE_TYPES = new HashSet<String>(Arrays.asList("jnt:group", "jnt:members", "jnt:member"));

    private ExternalContentStoreProvider contentStoreProvider;

    private JahiaUserManagerService jahiaUserManagerService;
    private JahiaGroupManagerService jahiaGroupManagerService;

    private UserGroupProvider userGroupProvider;

    private UserDataSource userDataSource;

    @Override
    public void start() {
        jahiaGroupManagerService.clearNonExistingGroupsCache();
    }

    @Override
    public void stop() {

    }

    @Override
    public List<String> getChildren(String path) throws RepositoryException {
        if (path == null || path.indexOf('/') == -1) {
            throw new PathNotFoundException(path);
        }
        if ("/".equals(path)) {
            return Collections.emptyList();
        }
        String[] pathSegments = StringUtils.split(path, '/');
        if (pathSegments.length == 1) {
            return Arrays.asList(MEMBERS_ROOT_NAME);
        }
        if (!MEMBERS_ROOT_NAME.equals(pathSegments[1])) {
            throw new PathNotFoundException(path);
        }
        if (pathSegments.length == 2) {
            List<String> types = new ArrayList<>();
            for (Member.MemberType t : Member.MemberType.values()) {
                types.add(t.name());
            }
            return types;
        }
        if (pathSegments.length == 3) {
            try {
                return getMembers(pathSegments[0], Member.MemberType.valueOf(pathSegments[2]));
            } catch (IllegalArgumentException e) {
                throw new PathNotFoundException(path);
            }
        }
        return Collections.emptyList();
    }

    public ExternalContentStoreProvider getContentStoreProvider() {
        return contentStoreProvider;
    }

    private ExternalData getGroupData(JahiaGroup group) {
        String path = "/" + group.getName();
        Map<String,String[]> properties = new HashMap<String, String[]>();
        Properties groupProperties = group.getProperties();
        for (Object key : groupProperties.keySet()) {
            properties.put((String) key, new String[]{(String) groupProperties.get(key)});
        }
        properties.put("j:external", new String[]{"true"});
        properties.put("j:externalSource", new String[]{StringUtils.removeEnd(contentStoreProvider.getKey(), ".groups")});
        return new ExternalData(path, path, "jnt:group", properties);
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
            return new ExternalData(path, path, "jnt:groupsFolder", new HashMap<String, String[]>());
        }
        String[] pathSegments = StringUtils.split(path, '/');
        if (pathSegments.length == 1) {
            try {
                return getGroupData(userGroupProvider.getGroup(pathSegments[0]));
            } catch (GroupNotFoundException e) {
                throw new PathNotFoundException("Cannot find group " + path, e);
            }
        }
        if (!MEMBERS_ROOT_NAME.equals(pathSegments[1])) {
            throw new PathNotFoundException(path);
        }
        String memberPath = StringUtils.substringAfter(path, "/" + MEMBERS_ROOT_NAME);
        String userName = StringUtils.substringAfter(memberPath, "/" + Member.MemberType.USER.name() + "/");
        String groupName = StringUtils.substringAfter(memberPath, "/" + Member.MemberType.GROUP.name() + "/");
        if (StringUtils.isNotBlank(userName)) {
            if (StringUtils.contains(userName, "/")) {
                throw new PathNotFoundException(path);
            } else {
                return getMemberData(path,
                        jahiaUserManagerService.getUserSplittingRule().getRelativePathForUsername(userName),
                        userDataSource.getContentStoreProvider());
            }
        }
        if (StringUtils.isNotBlank(groupName)) {
            if (StringUtils.contains(groupName, "/")) {
                throw new PathNotFoundException(path);
            } else {
                return getMemberData(path, "/" + groupName, contentStoreProvider);
            }
        }
        return new ExternalData(path, path, "jnt:members", new HashMap<String, String[]>());
    }

    private ExternalData getMemberData(String path, String refPath, ExternalContentStoreProvider provider) {
        HashMap<String, String[]> properties = new HashMap<String, String[]>();
        try {
            properties.put(MEMBER_REF_ATTR, new String[]{provider.getOrCreateInternalIdentifier(refPath)});
        } catch (RepositoryException e) {
            logger.error("Failed to get UUID for member " + refPath, e);
        }
        return new ExternalData(path, path, "jnt:member", properties);
    }

    @SuppressWarnings("unchecked")
    private List<String> getMembers(String groupName, Member.MemberType type) throws RepositoryException {
        Map<String, Object> sessionVariables = ExternalContentStoreProvider.getCurrentSession().getSessionVariables();
        Map<String, Map<Member.MemberType, List<String>>> members;
        if (sessionVariables.containsKey(MEMBERS_SESSION_VAR)) {
            members = (Map<String, Map<Member.MemberType, List<String>>>) sessionVariables.get(MEMBERS_SESSION_VAR);
        } else {
            members = new HashMap<>();
            sessionVariables.put(MEMBERS_SESSION_VAR, members);
        }
        if (members.containsKey(groupName)) {
            return members.get(groupName).get(type);
        } else {
            Map<Member.MemberType, List<String>> membersByType = new HashMap<>();
            for (Member.MemberType t : Member.MemberType.values()) {
                membersByType.put(t, new ArrayList<String>());
            }
            for (Member member : userGroupProvider.getGroupMembers(groupName)) {
                membersByType.get(member.getType()).add(member.getName());
            }
            members.put(groupName, membersByType);
            return membersByType.get(type);
        }
    }

    @Override
    public List<String> getReferringProperties(String identifier, String propertyName) {
        if (MEMBER_REF_ATTR.equals(propertyName)) {
            String principalId = identifier;
            List<String> groups = null;

            Member member = null;
            if (principalId.startsWith("/")) {
                // already an externalId -> identifier of a group
                member = new Member(StringUtils.substringAfterLast(principalId, "/"), Member.MemberType.GROUP);
                groups = userGroupProvider.getMembership(member);
            } else {
                try {
                    ExternalContentStoreProvider provider = userDataSource.getContentStoreProvider();
                    if (identifier.startsWith(provider.getId())) {
                        principalId = provider.getExternalProviderInitializerService().getExternalIdentifier(identifier);
                        if (principalId != null && principalId.startsWith("/")) {
                            member = new Member(StringUtils.substringAfterLast(principalId, "/"), Member.MemberType.USER);
                            groups = userGroupProvider.getMembership(member);
                        }
                    }
                } catch (RepositoryException e) {
                    logger.debug("Error while treating member id as an external user one", e);
                }
            }

            List<String> properties = new ArrayList<String>();
            if (member != null && groups != null && !groups.isEmpty()) {
                for (String group : groups) {
                    properties.add("/" + group + "/" + MEMBERS_ROOT_NAME + "/" + member.getType().name() + "/" + member.getName() + "/" + MEMBER_REF_ATTR);
                }
            }
            return properties;
        }

        return null;
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
        SearchCriteriaHelper.fillCriteriaFromConstraint(externalQuery.getConstraint(), searchCriteria, "groupname");
        searchCriteria.remove("jcr:language");
        List<String> result = new ArrayList<String>();
        try {
            for (String groupName : userGroupProvider.searchGroups(searchCriteria, externalQuery.getOffset(), externalQuery.getLimit())) {
                result.add("/" + groupName);
            }
        } catch (Exception e) {
            logger.error("Error while executing query {} on provider {}, issue {}",new Object[]{externalQuery,userGroupProvider,e.getMessage()});
            if(logger.isDebugEnabled()){
                logger.debug(e.getMessage(),e);
            }
        }
        return result;
    }

    @Override
    public boolean isAvailable() throws RepositoryException {
        return this.userGroupProvider.isAvailable();
    }

    public void setContentStoreProvider(ExternalContentStoreProvider contentStoreProvider) {
        this.contentStoreProvider = contentStoreProvider;
    }

    public void setJahiaUserManagerService(JahiaUserManagerService jahiaUserManagerService) {
        this.jahiaUserManagerService = jahiaUserManagerService;
    }

    public void setJahiaGroupManagerService(JahiaGroupManagerService jahiaGroupManagerService) {
        this.jahiaGroupManagerService = jahiaGroupManagerService;
    }

    public void setUserGroupProvider(UserGroupProvider userGroupProvider) {
        this.userGroupProvider = userGroupProvider;
    }

    public void setUserDataSource(UserDataSource userDataSource) {
        this.userDataSource = userDataSource;
    }
}
