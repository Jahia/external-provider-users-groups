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

import org.jahia.services.usermanager.JahiaGroup;
import org.jahia.services.usermanager.JahiaUser;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Properties;

/**
 * Describes a service for retrieving user and group information. 
 */
public interface UserGroupProvider {

    /**
     * Returns the user having the specified name
     *
     * @param name the user name
     * @return a JahiaUser object
     * @throws UserNotFoundException if no user with the specified name exists
     */
    JahiaUser getUser(String name) throws UserNotFoundException;

    /**
     * Returns the group having the specified name
     *
     * @param name the group name
     * @return a JahiaGroup object
     * @throws GroupNotFoundException if no group with the specified name exists
     */
    JahiaGroup getGroup(String name) throws GroupNotFoundException;

    /**
     * Returns the members of a specified group
     *
     * @param groupName the group name
     * @return a list of {@link Member}
     */
    List<Member> getGroupMembers(String groupName);

    /**
     * Returns all the groups containing the specified member
     *
     * @param member a member (user or group)
     * @return a list of group names
     */
    List<String> getMembership(Member member);

    /**
     * Find users according to a table of name=value properties. If the left
     * side value is "*" for a property then it will be tested against all the
     * properties. ie *=test* will match every property that starts with "test"
     *
     * @param searchCriteria a Properties object that contains search criteria
     *                        in the format name,value (for example "*"="*" or "username"="*test*") or
     *                        null to search without criteria
     * @param offset the offset of search result used for paging
     * @param limit the search result size limit used for paging. Value can be negative for unlimited result.
     * @return a list of user names
     */
    List<String> searchUsers(Properties searchCriteria, long offset, long limit);

    /**
     * Find groups according to a table of name=value properties. If the left
     * side value is "*" for a property then it will be tested against all the
     * properties. ie *=test* will match every property that starts with "test"
     *
     * @param searchCriteria a Properties object that contains search criteria
     *                        in the format name,value (for example "*"="*" or "groupname"="*test*") or
     *                        null to search without criteria
     * @param offset the offset of search result used for paging
     * @param limit the search result size limit used for paging. Value can be negative for unlimited result.
     * @return a list of group names
     */
    List<String> searchGroups(Properties searchCriteria, long offset, long limit);

    /**
     * Verify if the specified password is correct for the specified user
     *
     * @param userName the user name
     * @param userPassword the user password
     * @return {@value true} if the password is correct, else {@value false}
     */
    boolean verifyPassword(String userName, String userPassword);

    /**
     * @return true if the provider supports groups, else false
     */
    boolean supportsGroups();

    /**
     * @return true if the provider is available will be call when the "/" node is ask for the provider
     * can throw a RepositoryException or return false in case of provider not available
     * @throws RepositoryException
     */
    boolean isAvailable() throws RepositoryException;
}
