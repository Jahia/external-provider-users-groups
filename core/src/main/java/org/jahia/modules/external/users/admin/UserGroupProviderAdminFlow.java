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
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRStoreProvider;
import org.jahia.services.content.JCRStoreService;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.sites.JahiaSite;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.services.usermanager.JahiaUser;
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

    /**
     * Permission the caller must hold to use this screen.
     * <p>
     * The same permission this screen's own template declares, so the two enforcement points agree by
     * construction and a role that the template admits is not refused here. It is a child of the {@code admin}
     * aggregate rather than the aggregate itself: a role may hold it without holding {@code admin}, which is
     * how a delegated identity-administration role is expressed, whereas {@code admin} implies it.
     */
    private static final String REQUIRED_PERMISSION = "adminUsers";

    /**
     * Render mode in which a module's own definitions are edited, named as core names it.
     * <p>
     * Studio renders module content by design, and core's render conditions exempt it for that reason. It is
     * reachable only where {@code operatingMode} is {@code development}: the controller behind
     * {@code /cms/studio} declares {@code availableInProductionMode=false}. Applying the requirement here
     * would leave this screen alone among its siblings in refusing the one mode a template developer places
     * it from, and would withhold nothing anywhere the screen is actually served.
     */
    private static final String STUDIO_MODE = "studiomode";

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
     * @param renderContext
     *            the context of the render this transition was submitted from
     * @throws Exception
     *             in case of a creation error
     */
    public void createProvider(ParameterMap parameters, MutableAttributeMap<Object> flashScope, MessageContext messages,
            RenderContext renderContext) throws Exception {
        if (!isAdministrationGranted(renderContext)) {
            declined(messages);
            return;
        }

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
     * @param renderContext
     *            the context of the render this transition was submitted from
     * @throws Exception
     *             in case of an error during deletion
     */
    public void deleteProvider(String providerKey, String providerClass, MutableAttributeMap<Object> flashScope,
            MessageContext messages, RenderContext renderContext) throws Exception {
        if (!isAdministrationGranted(renderContext)) {
            declined(messages);
            return;
        }

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
     * @param renderContext
     *            the context of the render this transition was submitted from
     * @throws Exception
     *             in case of an error during edition
     */
    public void editProvider(ParameterMap parameters, MutableAttributeMap<Object> flashScope, MessageContext messages,
            RenderContext renderContext) throws Exception {
        if (!isAdministrationGranted(renderContext)) {
            declined(messages);
            return;
        }

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
     * @param renderContext
     *            the context of the render this screen is being served from
     * @return the provider create configuration map, empty when the caller may not use this screen
     */
    public Map<String, UserGroupProviderConfiguration> getCreateConfigurations(RenderContext renderContext) {
        if (!isAdministrationGranted(renderContext)) {
            return new HashMap<String, UserGroupProviderConfiguration>();
        }

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
     * @param renderContext
     *            the context of the render this screen is being served from
     * @return a list of registered user/group providers, empty when the caller may not use this screen
     */
    public List<UserGroupProviderInfo> getUserGroupProviders(RenderContext renderContext) {
        if (!isAdministrationGranted(renderContext)) {
            return new ArrayList<UserGroupProviderInfo>();
        }

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
     * Returns the provider key the edition and deletion forms work on.
     * <p>
     * These forms resolve the named provider's stored configuration, so the key carries the same requirement as
     * the rest of the screen. Without one they have nothing to resolve and render empty.
     *
     * @param providerKey
     *            the provider key submitted with the form
     * @param renderContext
     *            the context of the render this screen is being served from
     * @return the submitted key, when the caller may use this screen
     */
    public String resolveProviderKey(String providerKey, RenderContext renderContext) {
        return isAdministrationGranted(renderContext) ? providerKey : null;
    }

    /**
     * Resumes the specified provider.
     *
     * @param providerKey
     *            the key of the provider to be resumed
     * @param renderContext
     *            the context of the render this transition was submitted from
     * @throws JahiaInitializationException
     *             in case of a provider initialization error
     */
    public void resumeProvider(String providerKey, MessageContext messages, RenderContext renderContext) throws JahiaInitializationException {
        if (!isAdministrationGranted(renderContext)) {
            declined(messages);
            return;
        }

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

        addNoteForCluster(messages);
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
     * @param renderContext the context of the render this transition was submitted from
     */
    public void suspendProvider(String providerKey, MessageContext messages, RenderContext renderContext) {
        if (!isAdministrationGranted(renderContext)) {
            declined(messages);
            return;
        }

        UserGroupProviderRegistration registration = externalUserGroupService.getRegisteredProviders().get(providerKey);
        JCRStoreProvider userProvider = registration.getUserProvider();
        if (userProvider != null) {
            userProvider.stop();
        }
        JCRStoreProvider groupProvider = registration.getGroupProvider();
        if (groupProvider != null) {
            groupProvider.stop();
        }
        addNoteForCluster(messages);
    }

    /**
     * Whether the caller may read or change this instance's identity providers.
     * <p>
     * {@link #STUDIO_MODE} is exempt, for the reason given there. Otherwise the requirement is evaluated on
     * the render's <strong>main resource</strong>: the settings node, or the node the request was made
     * against, which is what an administrator role is granted on. That target is load-bearing rather than
     * incidental. What this screen reaches belongs to the module's own services rather than to a node bound
     * to the caller, so the main resource is the one thing here on which {@code hasPermission} can express a
     * requirement.
     *
     * @param renderContext the context of the render the transition was submitted from
     * @return {@code true} when the caller holds {@link #REQUIRED_PERMISSION} on the main resource
     */
    private boolean isAdministrationGranted(RenderContext renderContext) {
        if (renderContext != null && STUDIO_MODE.equals(renderContext.getEditModeConfigName())) {
            return true;
        }

        Resource mainResource = renderContext != null ? renderContext.getMainResource() : null;
        JahiaUser user = renderContext != null ? renderContext.getUser() : null;
        return grantsAdministration(mainResource != null ? mainResource.getNode() : null,
                user != null ? user.getName() : null);
    }

    /**
     * Visible for testing: the decision itself, on the node it is evaluated against.
     * <p>
     * Fails closed on a null node: with no node there is nothing to evaluate the requirement against, and
     * every operation this screen offers is an administration capability.
     * <p>
     * Reports at {@code DEBUG}: this runs on every render, so a louder level would make the log grow with
     * ordinary traffic. The one report an operator acts on is written once per attempted operation, by
     * {@link #declined(MessageContext)}.
     *
     * @param contextNode the node the requirement is evaluated on, or {@code null} when there is none
     * @param callerName the name of the caller, for the debug report only
     * @return {@code true} when the caller holds {@link #REQUIRED_PERMISSION} on {@code contextNode}
     */
    static boolean grantsAdministration(JCRNodeWrapper contextNode, String callerName) {
        if (contextNode == null) {
            logger.debug("No main resource to evaluate {} against", REQUIRED_PERMISSION);
            return false;
        }

        if (contextNode.hasPermission(REQUIRED_PERMISSION)) {
            return true;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("{} does not hold {} on {}", callerName != null ? callerName : "the current user",
                    REQUIRED_PERMISSION, contextNode.getPath());
        }
        return false;
    }

    /**
     * Reports that the requested operation was not carried out, with the screen's generic error.
     * <p>
     * The message shown says nothing about which condition was not met: it is the same text the screen shows
     * for any operation it could not complete. The log line names the permission and nothing
     * caller-controlled; {@code DEBUG} on this class identifies the caller and the node.
     */
    private static void declined(MessageContext messages) {
        logger.warn("A user and group provider operation was not carried out: the caller does not hold {} on the"
                + " node the screen was rendered against. Enable DEBUG on this class for the caller and the node.",
                REQUIRED_PERMISSION);
        messages.addMessage(new MessageBuilder().error().code("label.error").build());
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

    private void addNoteForCluster(MessageContext messages) {
        if (!SettingsBean.getInstance().isClusterActivated()) {
            return;
        }

        messages.addMessage(new MessageBuilder().info().code("label.userGroupProvider.clusterNote").build());
    }
}
