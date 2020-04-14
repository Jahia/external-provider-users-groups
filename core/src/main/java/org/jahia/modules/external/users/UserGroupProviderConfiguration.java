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
package org.jahia.modules.external.users;

import java.io.Serializable;
import java.util.Map;

/**
 * Interface to implement a custom behaviour for a user and group provider configuration
 */
public interface UserGroupProviderConfiguration extends Serializable {

    /**
     * @return the provider class name where this configuration apply to
     */
    String getProviderClass();

    /**
     * @return the name of provider configuration
     */
    String getName();

    /**
     * @return true if configuration creation is supported
     */
    boolean isCreateSupported();

    /**
     * @return the path of the JSP included in the creation form
     */
    String getCreateJSP();

    /**
     * Create configuration
     * @param parameters the request parameters
     * @param flashScope the scope to set variables necessary for a reload of creation form due to an exception
     * @return the provider key
     * @throws Exception in case of an error during configuration create
     */
    String create(Map<String, Object> parameters, Map<String, Object> flashScope) throws Exception;

    /**
     * @return true if configuration edit is supported
     */
    boolean isEditSupported();

    /**
     * @return the path of the JSP included in the edit form
     */
    String getEditJSP();

    /**
     * Edit configuration
     * @param providerKey the key of the provider
     * @param parameters the request parameters
     * @param flashScope the scope to set variables necessary for a reload of edit form due to an exception
     * @throws Exception in case of an error during configuration edit
     */
    void edit(String providerKey, Map<String, Object> parameters, Map<String, Object> flashScope) throws Exception;

    /**
     * @return true if configuration deletion is supported
     */
    boolean isDeleteSupported();

    /**
     * Delete configuration
     * @param providerKey the provider key
     * @param flashScope the scope to set variables necessary for a reload of deletion form due to an exception
     * @throws Exception in case of an error during configuration delete
     */
    void delete(String providerKey, Map<String, Object> flashScope) throws Exception;

}
