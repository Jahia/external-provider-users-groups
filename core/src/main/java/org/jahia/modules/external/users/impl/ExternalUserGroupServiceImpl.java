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
import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.users.ExternalUserGroupService;
import org.jahia.modules.external.users.UserGroupProvider;
import org.jahia.modules.external.users.UserGroupProviderConfiguration;
import org.jahia.modules.external.users.UserGroupProviderRegistration;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRStoreProvider;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRMountPointNode;
import org.jahia.services.templates.JahiaModulesBeanPostProcessor;
import org.jahia.settings.SettingsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;

import javax.jcr.RepositoryException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Implementation of the external user/group service.
 */
public class ExternalUserGroupServiceImpl implements ExternalUserGroupService, JahiaModulesBeanPostProcessor {

    private static final List<String> EXTENDABLE_TYPES = Arrays.asList("nt:base");
    private static final List<String> OVERRIDABLE_ITEMS = Arrays.asList("jnt:user.*", "jnt:usersFolder.*", "mix:lastModified.*", "jmix:lastPublished.*");
    private static final String PROVIDERS_MOUNT_CONTAINER = "providers";
    private static final String USERS_FOLDER_NAME = "users";
    private static final String GROUPS_FOLDER_NAME = "groups";
    private static final Logger logger = LoggerFactory.getLogger(ExternalUserGroupServiceImpl.class);

    private String readOnlyUserProperties;

    private Map<String, UserGroupProviderRegistration> registeredProviders = new ConcurrentSkipListMap<String, UserGroupProviderRegistration>();
    private Map<String, UserGroupProviderRegistration> registeredProvidersUnmodifiable = Collections.unmodifiableMap(registeredProviders);

    private Map<String, UserGroupProviderConfiguration> providerConfigurations = new ConcurrentHashMap<String, UserGroupProviderConfiguration>();
    private Map<String, UserGroupProviderConfiguration> providerConfigurationsUnmodifiable = Collections.unmodifiableMap(providerConfigurations);

    @Override
    public void register(String providerKey, final UserGroupProvider userGroupProvider) {
        register(providerKey, null, userGroupProvider);
    }

    @Override
    public synchronized void register(String providerKey, final String siteKey, final UserGroupProvider userGroupProvider) {

        String userProviderKey = providerKey + ".users";
        String groupProviderKey = providerKey + ".groups";

        if (!registeredProviders.containsKey(providerKey)) {
            try {

                UserDataSource userDataSource = (UserDataSource) SpringContextSingleton.getBeanInModulesContext("UserDataSourcePrototype");
                userDataSource.setUserGroupProvider(userGroupProvider);
                ExternalContentStoreProvider userProvider = (ExternalContentStoreProvider) SpringContextSingleton.getBeanInModulesContext("ExternalStoreProviderPrototype");
                userProvider.setKey(userProviderKey);
                String sitePath = "/sites/" + siteKey + "/";
                userProvider.setMountPoint((siteKey == null ? "/" : sitePath) + USERS_FOLDER_NAME + "/" + PROVIDERS_MOUNT_CONTAINER + "/" + providerKey);
                userProvider.setDataSource(userDataSource);
                userProvider.setExtendableTypes(EXTENDABLE_TYPES);
                userProvider.setOverridableItems(OVERRIDABLE_ITEMS);
                String readOnlyProps = (String) SettingsBean.getInstance().getPropertiesFile().get("external.users.properties.readonly." + providerKey);
                if (StringUtils.isBlank(readOnlyProps)) {
                    readOnlyProps = readOnlyUserProperties;
                }
                if (StringUtils.isNotBlank(readOnlyProps)) {
                    List<String> nonOverridableItems = new ArrayList<String>();
                    for (String p : StringUtils.split(readOnlyProps, ',')) {
                        if (StringUtils.isNotBlank(p)) {
                            nonOverridableItems.add("jnt:user." + p.trim());
                        }
                    }
                    if (!nonOverridableItems.isEmpty()) {
                        userProvider.setNonOverridableItems(nonOverridableItems);
                    }
                }
                userDataSource.setContentStoreProvider(userProvider);

                ExternalContentStoreProvider groupProvider = null;
                if (userGroupProvider.supportsGroups()) {
                    GroupDataSource groupDataSource = (GroupDataSource) SpringContextSingleton.getBeanInModulesContext("GroupDataSourcePrototype");
                    groupDataSource.setUserDataSource(userDataSource);
                    groupDataSource.setUserGroupProvider(userGroupProvider);
                    groupProvider = (ExternalContentStoreProvider) SpringContextSingleton.getBeanInModulesContext("ExternalStoreProviderPrototype");
                    groupProvider.setKey(groupProviderKey);
                    groupProvider.setMountPoint((siteKey == null ? "/" : sitePath)+ GROUPS_FOLDER_NAME + "/" + PROVIDERS_MOUNT_CONTAINER + "/" + providerKey);
                    groupProvider.setDataSource(groupDataSource);
                    groupDataSource.setContentStoreProvider(groupProvider);
                }

                registeredProviders.put(providerKey, new UserGroupProviderRegistration(siteKey, userProvider, groupProvider));

                userProvider.start();
                if (groupProvider != null) {
                    groupProvider.start();
                }

            } catch (JahiaInitializationException e) {
                logger.error(e.getMessage(), e);
            }
        }

        createMissingStructure(siteKey, userGroupProvider.supportsGroups());
    }

    private static void createMissingStructure(final String siteKey, final boolean supportsGroups) {

        try {
            JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Object>() {

                @Override
                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {

                    JCRNodeWrapper rootNode = null;
                    if (siteKey == null) {
                        rootNode = session.getNode("/");
                    } else {
                        String path = "/sites/" + siteKey;
                        if (session.nodeExists(path)) {
                            rootNode = session.getNode(path);
                        }
                    }
                    if (rootNode == null) {
                        return null;
                    }

                    boolean saveNeeded = false;
                    JCRNodeWrapper node = rootNode;
                    if (node.hasNode(USERS_FOLDER_NAME)) {
                        node = node.getNode(USERS_FOLDER_NAME);
                    } else {
                        node = node.addNode(USERS_FOLDER_NAME, "jnt:usersFolder");
                        saveNeeded = true;
                    }
                    if (!node.hasNode(PROVIDERS_MOUNT_CONTAINER)) {
                        node.addNode(PROVIDERS_MOUNT_CONTAINER, "jnt:usersFolder");
                        saveNeeded = true;
                    }
                    if (supportsGroups) {
                        node = rootNode;
                        if (node.hasNode(GROUPS_FOLDER_NAME)) {
                            node = node.getNode(GROUPS_FOLDER_NAME);
                        } else {
                            node = node.addNode(GROUPS_FOLDER_NAME, "jnt:groupsFolder");
                            saveNeeded = true;
                        }
                        if (!node.hasNode(PROVIDERS_MOUNT_CONTAINER)) {
                            node.addNode(PROVIDERS_MOUNT_CONTAINER, "jnt:groupsFolder");
                            saveNeeded = true;
                        }
                    }
                    if (saveNeeded) {
                        session.save();
                    }
                    return null;
                }
            });
        } catch (RepositoryException e) {
            logger.error("Failed to create providers mount containers", e);
        }
    }

    @Override
    public void initSiteForPendingProviders(String newSiteKey) {
        for (Map.Entry<String, UserGroupProviderRegistration> entry : registeredProviders.entrySet()) {
            String siteKey = entry.getValue().getSiteKey();
            if (!newSiteKey.equals(siteKey)) {
                continue;
            }
            createMissingStructure(newSiteKey, entry.getValue().getGroupProvider() != null);
        }
    }

    @Override
    public synchronized void unregister(String providerKey) {
        UserGroupProviderRegistration registration = registeredProviders.get(providerKey);
        stopProvider(registration.getUserProvider());
        stopProvider(registration.getGroupProvider());
        registeredProviders.remove(providerKey);
    }

    private void stopProvider(JCRStoreProvider provider) {
        if (provider == null) {
            return;
        }
        provider.stop();
    }

    @Override
    public void setConfiguration(String providerClass, UserGroupProviderConfiguration userGroupProviderConfig) {
        providerConfigurations.put(providerClass, userGroupProviderConfig);
    }

    @Override
    public void setMountStatus(String providerKey, JCRMountPointNode.MountStatus status, String message) {
        UserGroupProviderRegistration registration = registeredProviders.get(providerKey);
        setProviderMountStatus(registration.getUserProvider(), status, message);
        setProviderMountStatus(registration.getGroupProvider(), status, message);
    }

    private void setProviderMountStatus(JCRStoreProvider provider, JCRMountPointNode.MountStatus status, String message) {
        if (provider == null) {
            return;
        }
        provider.setMountStatus(status, message);
    }

    @Override
    public Map<String, UserGroupProviderConfiguration> getProviderConfigurations() {
        return providerConfigurationsUnmodifiable;
    }

    @Override
    public Map<String, UserGroupProviderRegistration> getRegisteredProviders() {
        return registeredProvidersUnmodifiable;
    }

    public void setReadOnlyUserProperties(String readOnlyUserProperties) {
        this.readOnlyUserProperties = readOnlyUserProperties;
    }

    @Override
    public void postProcessBeforeDestruction(Object o, String s) throws BeansException {
        if (o instanceof UserGroupProviderConfiguration) {
            String key = null;
            for (Map.Entry<String, UserGroupProviderConfiguration> entry : providerConfigurations.entrySet()) {
                if (entry.getValue() == o) {
                    key = entry.getKey();
                }
            }
            if (key != null) {
                providerConfigurations.remove(key);
            }
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object o, String s) throws BeansException {
        return o;
    }

    @Override
    public Object postProcessAfterInitialization(Object o, String s) throws BeansException {
        return o;
    }
}
