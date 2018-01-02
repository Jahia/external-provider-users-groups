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
package org.jahia.modules.external.users.admin;

import java.io.Serializable;

/**
 * Class to represent a user and group provider in a WebFlow
 */
public class UserGroupProviderInfo implements Serializable {

    private static final long serialVersionUID = 8377758659660801865L;

    private boolean deleteSupported;
    
    private String editJSP;
    
    private boolean editSupported;
    
    private boolean groupSupported;
    
    private String key;
    
    private String providerClass;
    
    private boolean running;

    private String siteKey;

    private boolean targetAvailable = true;

    public String getEditJSP() {
        return editJSP;
    }

    public String getKey() {
        return key;
    }

    public String getProviderClass() {
        return providerClass;
    }

    public String getSiteKey() {
        return siteKey;
    }

    public boolean isDeleteSupported() {
        return deleteSupported;
    }

    public boolean isEditSupported() {
        return editSupported;
    }

    public boolean isGroupSupported() {
        return groupSupported;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isTargetAvailable() {
        return targetAvailable;
    }

    public void setDeleteSupported(boolean deleteSupported) {
        this.deleteSupported = deleteSupported;
    }

    public void setEditJSP(String editJSP) {
        this.editJSP = editJSP;
    }

    public void setEditSupported(boolean editSupported) {
        this.editSupported = editSupported;
    }

    public void setGroupSupported(boolean groupSupported) {
        this.groupSupported = groupSupported;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setProviderClass(String providerClass) {
        this.providerClass = providerClass;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void setSiteKey(String siteKey) {
        this.siteKey = siteKey;
    }

    public void setTargetAvailable(boolean targetAvailable) {
        this.targetAvailable = targetAvailable;
    }
}
