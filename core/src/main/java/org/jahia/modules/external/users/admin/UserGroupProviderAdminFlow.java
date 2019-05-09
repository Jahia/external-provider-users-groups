/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.external.users.admin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jahia.exceptions.JahiaException;
import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.users.ExternalUserGroupService;
import org.jahia.modules.external.users.UserGroupProvider;
import org.jahia.modules.external.users.UserGroupProviderConfiguration;
import org.jahia.modules.external.users.UserGroupProviderRegistration;
import org.jahia.modules.external.users.impl.UserDataSource;
import org.jahia.services.content.JCRStoreProvider;
import org.jahia.services.content.JCRStoreService;
import org.jahia.services.sites.JahiaSite;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.settings.SettingsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.core.collection.ParameterMap;

/**
 * Flow controller for the user/group providers.
 */
public class UserGroupProviderAdminFlow implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(UserGroupProviderAdminFlow.class);

    private static final long AVAILABILITY_TIMEOUT = 60 * 1000L;

    private static final long serialVersionUID = 4171622809934546645L;

    private static final int WAIT_SLEEP = 2000;

    @Autowired
    private transient ExternalUserGroupService externalUserGroupService;

    private transient JahiaSitesService jahiaSitesService;

    private transient JCRStoreService jcrStoreService;

    /**
     * Performs the creation of the provider.
     *
     * @param parameters
     *            flow parameter map
     * @param flashScope
     *            flow attribute map
     * @throws Exception
     *             in case of a creation error
     */
    public void createProvider(ParameterMap parameters, MutableAttributeMap<Object> flashScope, MessageContext messages) throws Exception {
        Map<String, UserGroupProviderConfiguration> configurations = externalUserGroupService.getProviderConfigurations();
        String providerClass = parameters.get("providerClass");
        String providerKey = configurations.get(providerClass).create(parameters.asMap(), flashScope.asMap()) + ".users";
        wait(providerKey, true, messages);
    }

    /**
     * Performs deletion of the provider
     *
     * @param providerKey
     *            the key of the provider
     * @param providerClass
     *            provider class name
     * @param flashScope
     *            the flow attribute map
     * @throws Exception
     *             in case of an error during deletion
     */
    public void deleteProvider(String providerKey, String providerClass, MutableAttributeMap<Object> flashScope, MessageContext messages) throws Exception {
        Map<String, UserGroupProviderConfiguration> configurations = externalUserGroupService.getProviderConfigurations();
        configurations.get(providerClass).delete(providerKey, flashScope.asMap());
        providerKey += ".users";
        wait(providerKey, false, messages);
    }

    /**
     * Performs the edition of the provider configuration.
     *
     * @param parameters
     *            flow parameter map
     * @param flashScope
     *            flow attribute map
     * @throws Exception
     *             in case of an error during edition
     */
    public void editProvider(ParameterMap parameters, MutableAttributeMap<Object> flashScope, MessageContext messages) throws Exception {
        Map<String, UserGroupProviderConfiguration> configurations = externalUserGroupService.getProviderConfigurations();
        String providerKey = parameters.get("providerKey");
        String providerClass = parameters.get("providerClass");
        configurations.get(providerClass).edit(providerKey, parameters.asMap(), flashScope.asMap());
        providerKey += ".users";
        wait(providerKey, true, messages);
    }

    /**
     * Returns the provider create configuration map.
     *
     * @return the provider create configuration map
     */
    public Map<String, UserGroupProviderConfiguration> getCreateConfigurations() {
        HashMap<String, UserGroupProviderConfiguration> map = new HashMap<String, UserGroupProviderConfiguration>();
        for (Map.Entry<String, UserGroupProviderConfiguration> entry : externalUserGroupService.getProviderConfigurations().entrySet()) {
            if (entry.getValue().isCreateSupported()) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    /**
     * Returns a list of registered user/group providers.
     *
     * @return a list of registered user/group providers
     */
    public List<UserGroupProviderInfo> getUserGroupProviders() {
        ArrayList<UserGroupProviderInfo> infos = new ArrayList<UserGroupProviderInfo>();
        Map<String, JCRStoreProvider> providers = jcrStoreService.getSessionFactory().getProviders();
        for (Map.Entry<String, UserGroupProviderRegistration> entry : externalUserGroupService.getRegisteredProviders().entrySet()) {
            UserGroupProviderInfo providerInfo = new UserGroupProviderInfo();
            providerInfo.setKey(entry.getKey());
            UserDataSource dataSource = (UserDataSource) entry.getValue().getUserProvider().getDataSource();
            UserGroupProvider userGroupProvider = dataSource.getUserGroupProvider();
            String userGroupProviderClass = userGroupProvider.getClass().getName();
            providerInfo.setProviderClass(userGroupProviderClass);
            providerInfo.setGroupSupported(userGroupProvider.supportsGroups());
            JCRStoreProvider prov = providers.get(entry.getKey() + ".users");
            providerInfo.setRunning(prov != null && prov.isAvailable());
            Map<String, UserGroupProviderConfiguration> configurations = externalUserGroupService.getProviderConfigurations();
            UserGroupProviderConfiguration configuration = configurations.get(userGroupProviderClass);
            if (configuration != null) {
                providerInfo.setEditSupported(configuration.isEditSupported());
                providerInfo.setEditJSP(configuration.getEditJSP());
                providerInfo.setDeleteSupported(configuration.isDeleteSupported());
            }
            String siteKey = entry.getValue().getSiteKey();
            providerInfo.setSiteKey(siteKey);
            JahiaSite targetSite = null;
            if (siteKey != null) {
                try {
                    targetSite = jahiaSitesService.getSiteByKey(siteKey);
                } catch (JahiaException e) {
                    logger.debug("Cannot get site " + siteKey, e);
                }
            }
            providerInfo.setTargetAvailable(siteKey == null || targetSite != null);
            infos.add(providerInfo);
        }
        return infos;
    }

    /**
     * Resumes the specified provider.
     *
     * @param providerKey
     *            the key of the provider to be resumed
     * @throws JahiaInitializationException
     *             in case of a provider initialization error
     */
    public void resumeProvider(String providerKey, MessageContext messages) throws JahiaInitializationException {
        UserGroupProviderRegistration registration = externalUserGroupService.getRegisteredProviders().get(providerKey);

        boolean isUnavailable = true; // unavailable by default
        String msg = "Unavailable";
        try {
            JCRStoreProvider userProvider = registration.getUserProvider();
            if (userProvider != null) {
                isUnavailable = !userProvider.start(true);
            }

            JCRStoreProvider groupProvider = registration.getGroupProvider();
            if (groupProvider != null) {
                isUnavailable = isUnavailable || !groupProvider.start(true);
            }
        } catch (JahiaInitializationException e) {
            msg = e.getUserErrorMsg();
        }

        if (isUnavailable) {
            messages.addMessage(new MessageBuilder().error().code("label.userGroupProvider.resumeError").arg(msg).build());
        }
    }

    @Autowired
    public void setJcrStoreService(@Value("#{JCRStoreService}") JCRStoreService jcrStoreService) {
        this.jcrStoreService = jcrStoreService;
    }

    @Autowired
    public void setJahiaSitesService(@Value("#{JahiaSitesService}") JahiaSitesService jahiaSitesService) {
        this.jahiaSitesService = jahiaSitesService;
    }

    /**
     * Suspends the provider.
     *
     * @param providerKey the key of the provider to be resumed
     */
    public void suspendProvider(String providerKey, MessageContext messages) {
        UserGroupProviderRegistration registration = externalUserGroupService.getRegisteredProviders().get(providerKey);
        JCRStoreProvider userProvider = registration.getUserProvider();
        if (userProvider != null) {
            userProvider.stop();
        }
        JCRStoreProvider groupProvider = registration.getGroupProvider();
        if (groupProvider != null) {
            groupProvider.stop();
        }
    }

    private void wait(String providerKey, boolean shouldBeAvailable, MessageContext messages) {

        final long startTime = System.currentTimeMillis();
        long endTime = startTime + AVAILABILITY_TIMEOUT;

        final String registrationKey = providerKey.substring(0, providerKey.lastIndexOf('.'));
        final Map<String, UserGroupProviderRegistration> registeredProviders = externalUserGroupService.getRegisteredProviders();

        while (System.currentTimeMillis() < endTime) {

            final UserGroupProviderRegistration registration = registeredProviders.get(registrationKey);

            if (shouldBeAvailable) {
                if (registration != null) {
                    final ExternalContentStoreProvider provider = registration.getUserProvider();
                    if (provider != null) {
                        final boolean available = provider.isAvailable();
                        if (!available) {
                            final String statusMessage = provider.getMountStatusMessage();
                            if (statusMessage != null) {
                                messages.addMessage(new MessageBuilder().error().code("label.userGroupProvider.createError").arg(statusMessage).build());
                                // todo: maybe use error mount status?
                                provider.setMountStatusMessage(null);
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
            } else {
                if (registration == null) {
                    break;
                }
            }

            // wait for provider availability / unavailability if it's asynchronous
            try {
                Thread.sleep(WAIT_SLEEP);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

}
