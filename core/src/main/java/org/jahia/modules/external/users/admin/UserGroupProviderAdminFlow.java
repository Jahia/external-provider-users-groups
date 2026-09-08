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
import java.util.concurrent.atomic.AtomicLong;

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
     * The same permission this screen's own template declares (see {@code repository.xml}), so the two
     * enforcement points ask for the same thing and a caller the template admits is not refused here.
     */
    private static final String REQUIRED_PERMISSION = "adminUsers";

    /**
     * Render mode in which a module's own definitions are edited, named as core names it.
     * <p>
     * Studio renders module content by design, and core's render conditions exempt it for that reason. It is
     * reachable only where {@code operatingMode} is {@code development}: the controller behind
     * {@code /cms/studio} declares {@code availableInProductionMode=false}. Withholding what this screen
     * lists there would leave it alone among its siblings in refusing the one mode a template developer
     * places it from, and would withhold nothing anywhere the screen is actually served.
     * <p>
     * The exemption stops at what the screen lists. A Studio render is admitted on {@code studioModeAccess}
     * over {@code /modules}, a permission that says nothing about administering this instance's identity
     * providers, so the operations and the forms that submit them are decided on the requirement in this
     * mode as in any other.
     */
    private static final String STUDIO_MODE = "studiomode";

    /**
     * One {@code WARN} per interval, for the whole class rather than per caller or per operation.
     * <p>
     * A refusal is written from a path unauthenticated traffic can drive, so an unthrottled report would let
     * the caller choose how fast this log grows. Core throttles the same class of refusal through
     * {@code LimiterExecutor.executeOncePerInterval}, which is not in the {@code 8.2.0.4} API this module
     * builds against; the shape is kept here instead of raising what the module requires. Every occurrence
     * is still written at {@code DEBUG}.
     */
    private static final long DECLINED_LOG_INTERVAL_MS = 5L * 60 * 1000;

    /** Time from which the next refusal report is allowed, in milliseconds. */
    private static final AtomicLong nextDeclinedLogTime = new AtomicLong();

    @Autowired
    private transient ExternalUserGroupService externalUserGroupService;

    private transient JahiaSitesService jahiaSitesService;

    private transient JCRStoreService jcrStoreService;

    /**
     * Performs the creation of the provider, or carries nothing out and reports why.
     * <p>
     * Three states end it early, asked in this order: the caller does not hold the requirement
     * ({@link #isAdministrationGranted(RenderContext)}), nothing is registered for the provider class the
     * request names, or that class says it does not support creation. The list this transition is reached
     * from offers no button in the last two, so a request in either state did not come from it.
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

        UserGroupProviderConfiguration configuration = declaredBy(
                externalUserGroupService.getProviderConfigurations(), parameters.get("providerClass"));
        if (configuration == null) {
            unknownProviderKind(messages);
            return;
        }
        if (!configuration.isCreateSupported()) {
            unsupportedOperation(messages);
            return;
        }

        String providerKey = configuration.create(parameters.asMap(), flashScope.asMap()) + ".users";
        wait(providerKey, true, messages);
    }

    /**
     * Performs deletion of the provider, or carries nothing out and reports why.
     * <p>
     * Ends early in the three states {@link #createProvider(ParameterMap, MutableAttributeMap, MessageContext, RenderContext)}
     * describes, with {@code isDeleteSupported} in place of the creation flag.
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

        UserGroupProviderConfiguration configuration =
                declaredBy(externalUserGroupService.getProviderConfigurations(), providerClass);
        if (configuration == null) {
            unknownProviderKind(messages);
            return;
        }
        if (!configuration.isDeleteSupported()) {
            unsupportedOperation(messages);
            return;
        }

        configuration.delete(providerKey, flashScope.asMap());
        providerKey += ".users";
        wait(providerKey, false, messages);
    }

    /**
     * Performs the edition of the provider configuration, or carries nothing out and reports why.
     * <p>
     * Ends early in the three states {@link #createProvider(ParameterMap, MutableAttributeMap, MessageContext, RenderContext)}
     * describes, with {@code isEditSupported} in place of the creation flag.
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

        String providerKey = parameters.get("providerKey");
        UserGroupProviderConfiguration configuration = declaredBy(
                externalUserGroupService.getProviderConfigurations(), parameters.get("providerClass"));
        if (configuration == null) {
            unknownProviderKind(messages);
            return;
        }
        if (!configuration.isEditSupported()) {
            unsupportedOperation(messages);
            return;
        }

        configuration.edit(providerKey, parameters.asMap(), flashScope.asMap());
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
        if (!isInventoryReadable(renderContext)) {
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
        if (!isInventoryReadable(renderContext)) {
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
     * The view a provider configuration declares for its create form.
     * <p>
     * Called from the flow with the provider class the request names, and it answers with the path that
     * class's own registered configuration declares. The path itself therefore never travels through the
     * request: a caller chooses which kind of provider to create, and the server decides what that renders.
     * <p>
     * A kind that declares a view it does not support resolves to none, so this answers for exactly the
     * kinds {@link #getCreateConfigurations(RenderContext)} keeps — the list that leads to this form.
     *
     * @param providerClass the provider class the request names
     * @return the create view declared for that class, or {@code null} when nothing declares one
     */
    public String resolveCreateJSP(String providerClass) {
        UserGroupProviderConfiguration configuration =
                declaredBy(externalUserGroupService.getProviderConfigurations(), providerClass);
        return configuration != null && configuration.isCreateSupported() ? configuration.getCreateJSP() : null;
    }

    /**
     * The name a provider configuration declares for itself, which is what the create form's heading
     * shows. Resolved for the same reason the views are: the heading renders it, so a request naming it
     * would decide part of the page.
     *
     * @param providerClass the provider class the request names
     * @return the name declared for that class, or {@code null} when nothing declares one — the heading
     *         has its own fallback for that
     */
    public String resolveProviderName(String providerClass) {
        UserGroupProviderConfiguration configuration =
                declaredBy(externalUserGroupService.getProviderConfigurations(), providerClass);
        return configuration != null ? configuration.getName() : null;
    }

    /**
     * The view a provider configuration declares for its edit form, resolved the way
     * {@link #resolveCreateJSP(String)} resolves the create one, and gated on the same flag the list
     * reads before it offers an Edit button.
     *
     * @param providerClass the provider class the request names
     * @return the edit view declared for that class, or {@code null} when nothing declares one
     */
    public String resolveEditJSP(String providerClass) {
        UserGroupProviderConfiguration configuration =
                declaredBy(externalUserGroupService.getProviderConfigurations(), providerClass);
        return configuration != null && configuration.isEditSupported() ? configuration.getEditJSP() : null;
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

    /**
     * Visible for testing: the provider service the resolvers read. Production wiring is the
     * {@code @Autowired} field, which Spring sets directly; this exists so that a unit test can reach
     * {@link #resolveCreateJSP(String)} without a container.
     */
    void setExternalUserGroupService(ExternalUserGroupService externalUserGroupService) {
        this.externalUserGroupService = externalUserGroupService;
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
     * Whether the caller may change this instance's identity providers, or read one's stored configuration.
     * <p>
     * The requirement is evaluated on the render's resource — the AJAX resource when there is one, the main
     * resource otherwise, the same selection {@code TemplatePermissionCheckFilter} makes — which is what an
     * administrator role is granted on. That target is load-bearing rather than incidental. What this screen
     * reaches belongs to the module's own services rather than to a node bound to the caller, so this
     * resource is the one thing here on which {@code hasPermission} can express a requirement.
     *
     * Called from the flow: each of the five transitions gates its write on it, and each of the three forms
     * gates its entry on it through a decision-state, so an unauthorized caller is never served a form that
     * reads an existing provider's stored configuration or that includes the JSP its own request named.
     * {@link #STUDIO_MODE} is not exempt here, for the reason given there.
     *
     * @param renderContext the context of the render the transition was submitted from
     * @return {@code true} when the caller holds {@link #REQUIRED_PERMISSION} on that resource
     */
    public boolean isAdministrationGranted(RenderContext renderContext) {
        Resource resource = null;
        if (renderContext != null) {
            // Same resource TemplatePermissionCheckFilter evaluates its requirement on: the AJAX resource
            // when the render has one, the main resource otherwise. Selecting differently would let the two
            // enforcement points disagree on an AJAX render and refuse a caller the template admits.
            resource = renderContext.getAjaxResource() != null
                    ? renderContext.getAjaxResource() : renderContext.getMainResource();
        }
        JahiaUser user = renderContext != null ? renderContext.getUser() : null;
        return grantsAdministration(resource != null ? resource.getNode() : null,
                user != null ? user.getName() : null);
    }

    /**
     * Whether the render may list what this instance has: the registered providers, and the kinds of provider
     * that can be created. Weaker than {@link #isAdministrationGranted(RenderContext)} by the
     * {@link #STUDIO_MODE} exemption alone, and package-private so that the two answers are pinned apart by
     * the unit suite.
     *
     * @param renderContext the context of the render this screen is being served from
     * @return {@code true} when the render is a Studio render, or when the caller holds the requirement
     */
    boolean isInventoryReadable(RenderContext renderContext) {
        return isStudioRender(renderContext) || isAdministrationGranted(renderContext);
    }

    private static boolean isStudioRender(RenderContext renderContext) {
        return renderContext != null && STUDIO_MODE.equals(renderContext.getEditModeConfigName());
    }

    /**
     * Visible for testing: the registered configuration for a provider class, or {@code null}.
     * <p>
     * Fails closed on a class nothing is registered for. A form with no view renders no fields, which is
     * the safe end of that branch: the alternative — falling back to a path the request carried — is the
     * thing resolving this server-side exists to remove.
     *
     * @param configurations the registered provider configurations, keyed by provider class
     * @param providerClass the provider class the request names
     * @return the configuration registered for it, or {@code null} when there is none
     */
    static UserGroupProviderConfiguration declaredBy(
            Map<String, UserGroupProviderConfiguration> configurations, String providerClass) {
        if (configurations == null || providerClass == null) {
            return null;
        }

        return configurations.get(providerClass);
    }

    /**
     * Visible for testing: the decision itself, on the node it is evaluated against.
     * <p>
     * Fails closed on a null node: with no node there is nothing to evaluate the requirement against, and
     * every operation this screen offers is an administration capability.
     * <p>
     * Reports at {@code DEBUG}: this runs on every render, so a louder level would make the log grow with
     * ordinary traffic. The report an operator acts on is written by {@link #declined(MessageContext)}, for
     * a refused transition and at the interval given there.
     *
     * @param contextNode the node the requirement is evaluated on, or {@code null} when there is none
     * @param callerName the name of the caller, for the debug report only
     * @return {@code true} when the caller holds {@link #REQUIRED_PERMISSION} on {@code contextNode}
     */
    static boolean grantsAdministration(JCRNodeWrapper contextNode, String callerName) {
        if (contextNode == null) {
            logger.debug("No render resource to evaluate {} against", REQUIRED_PERMISSION);
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
     * Reports that the requested operation was not carried out.
     * <p>
     * The message names the screen, not the condition: the same text stands whether the caller lacked the
     * permission or the render had no node to evaluate it against. The log line names the permission and
     * nothing caller-controlled; {@code DEBUG} on this class identifies the caller and the node.
     * <p>
     * Called from the five transitions, and not from the decision-state a refused form entry takes: an
     * entry that never claimed to change anything routes back to the list. The caller is told of every
     * refusal; the operator is told once per {@link #DECLINED_LOG_INTERVAL_MS}, for the reason given there.
     */
    /**
     * Reports that the kind of provider the request named does not offer the operation it asked for.
     * <p>
     * Distinct from {@link #unknownProviderKind(MessageContext)}: the kind is registered, and says of
     * itself that it does not do this. The list reads the same flags before it offers a button, so this
     * answers a request that did not come from one.
     */
    private static void unsupportedOperation(MessageContext messages) {
        logger.debug("A user and group provider operation was not offered by the provider kind the request named");
        messages.addMessage(new MessageBuilder().error().code("label.userGroupProvider.unsupportedOperation").build());
    }

    /**
     * Reports that the request named a kind of provider nothing is registered for.
     * <p>
     * Distinct from {@link #declined(MessageContext)}: that one answers a caller who may not use the
     * screen, this one a caller who may. Reports at {@code DEBUG} and names nothing caller-controlled —
     * the class the request named is caller-controlled, and the operator's own registry is what says
     * which kinds exist.
     */
    private static void unknownProviderKind(MessageContext messages) {
        logger.debug("A user and group provider operation named a provider kind with no registered configuration");
        messages.addMessage(new MessageBuilder().error().code("label.userGroupProvider.unknownProviderClass").build());
    }

    private static void declined(MessageContext messages) {
        long now = System.currentTimeMillis();
        long allowedFrom = nextDeclinedLogTime.get();
        if (now >= allowedFrom && nextDeclinedLogTime.compareAndSet(allowedFrom, now + DECLINED_LOG_INTERVAL_MS)) {
            logger.warn("A user and group provider operation was not carried out: the caller does not hold {} on"
                    + " the node the screen was rendered against. Enable DEBUG on this class for the caller and"
                    + " the node, and for every occurrence. (silent for {}min)", REQUIRED_PERMISSION,
                    DECLINED_LOG_INTERVAL_MS / 60000);
        } else {
            logger.debug("A user and group provider operation was not carried out: the caller does not hold {}"
                    + " on the node the screen was rendered against", REQUIRED_PERMISSION);
        }
        messages.addMessage(new MessageBuilder().error().code("label.userGroupProvider.notPermitted").build());
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
