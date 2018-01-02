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
package org.jahia.modules.external.users;

import java.util.Map;

import org.jahia.services.content.decorator.JCRMountPointNode;

/**
 * Service to register and unregister a UserGroupProvider
 */
public interface ExternalUserGroupService {

    /**
     * Perform registration of a user/group provider with the specified key targeted for the defined site.
     *
     * @param providerKey
     *            the key to register the provider under
     * @param siteKey
     *            the key of the target site
     * @param userGroupProvider
     *            the user/group provider instance
     */
    void register(String providerKey, String siteKey, UserGroupProvider userGroupProvider);

    /**
     * Perform registration of a user/group provider with the specified key.
     *
     * @param providerKey
     *            the key to register the provider under
     * @param userGroupProvider
     *            the user/group provider instance
     */
    void register(String providerKey, UserGroupProvider userGroupProvider);

    /**
     * Sets the provider configuration for the specified provider class.
     *
     * @param providerClass
     *            the fully-qualified class name of the provider to set configuratoin for
     * @param userGroupProviderConfig
     *            the configuration object
     */
    void setConfiguration(String providerClass, UserGroupProviderConfiguration userGroupProviderConfig);

    /**
     * Unregisters the user/group provider for the specified key.
     *
     * @param providerKey
     *            the key of the provider to be unregistered
     */
    void unregister(String providerKey);


    /**
     * Allow to change the status of a given provider
     * @param providerKey
     * @param status
     * @param message
     */
    void setMountStatus(String providerKey, JCRMountPointNode.MountStatus status, String message);

    /**
     * @return the map of currently registered providers by provider key
     */
    Map<String, UserGroupProviderRegistration> getRegisteredProviders();

    /**
     * @return the map of provider configurations by provider class name
     */
    Map<String, UserGroupProviderConfiguration> getProviderConfigurations();

    /**
     * Initialize site for pending providers
     * @param siteKey
     */
    void initSiteForPendingProviders(String siteKey);
}
