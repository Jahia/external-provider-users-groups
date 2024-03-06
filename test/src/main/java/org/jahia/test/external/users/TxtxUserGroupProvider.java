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
package org.jahia.test.external.users;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.external.users.BaseUserGroupProvider;
import org.jahia.modules.external.users.GroupNotFoundException;
import org.jahia.modules.external.users.Member;
import org.jahia.modules.external.users.UserNotFoundException;
import org.jahia.services.usermanager.JahiaGroup;
import org.jahia.services.usermanager.JahiaGroupImpl;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserImpl;

import javax.jcr.RepositoryException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TxtxUserGroupProvider extends BaseUserGroupProvider {

    private List<String> users = Arrays.asList("tata", "tete", "titi", "yaya");

    private List<String> groups = Arrays.asList("toto", "tutu", "tyty");

    @Override
    public JahiaUser getUser(String name) throws UserNotFoundException {
        if (users.contains(name)) {
            Properties properties = new Properties();
            properties.put("j:email", "mail@tx.tx");
            String capitalizedName = StringUtils.capitalize(name);
            properties.put("j:firstName", capitalizedName);
            properties.put("j:lastName", "Mc" + capitalizedName);
            properties.put("j:publicProperties", new String[] { "j:firstName", "j:lastName" });
            return new JahiaUserImpl(name, name, properties, getKey());
        }
        throw new UserNotFoundException("Cannot find user " + name);
    }

    @Override
    public JahiaGroup getGroup(String name) throws GroupNotFoundException {
        if (groups.contains(name)) {
            Properties properties = new Properties();
            return new JahiaGroupImpl(name, name, null, properties);
        }
        throw new GroupNotFoundException("Cannot find group " + name);
    }

    @Override
    public List<Member> getGroupMembers(String groupName) {
        ArrayList<Member> members = new ArrayList<Member>();
        if ("toto".equals(groupName)) {
            members.add(new Member("tata", Member.MemberType.USER));
            members.add(new Member("tete", Member.MemberType.USER));
            members.add(new Member("tutu", Member.MemberType.GROUP));
        }
        if ("tutu".equals(groupName)) {
            members.add(new Member("titi", Member.MemberType.USER));
        }
        if ("tyty".equals(groupName)) {
            members.add(new Member("tete", Member.MemberType.USER));
            members.add(new Member("titi", Member.MemberType.USER));
        }
        return members;
    }

    @Override
    public List<String> getMembership(Member member) {
        if ("tata".equals(member.getName())) {
            return Arrays.asList("toto");
        } else if ("tete".equals(member.getName())) {
            return Arrays.asList("toto", "tyty");
        } else if ("titi".equals(member.getName())) {
            return Arrays.asList("tutu", "tyty");
        } else if ("toto".equals(member.getName())) {
            return Collections.emptyList();
        } else if ("tutu".equals(member.getName())) {
            return Arrays.asList("toto");
        } else if ("tyty".equals(member.getName())) {
            return Collections.emptyList();
        }
        return null;
    }

    @Override
    public List<String> searchUsers(Properties searchCriterias, long offset, long limit) {
        if (searchCriterias.get("cookieauth") != null) {
            return Collections.emptyList();
        }
        String filter = (String) searchCriterias.get("username");
        if (filter == null) {
            filter = (String) searchCriterias.get("*");
        }
        List<String> l;
        if (filter != null) {
            final String pattern = "^".concat(filter.replace("*", ".*")).concat("$");
            l = users.stream().filter(user -> Pattern.matches(pattern, user)).collect(Collectors.toList());
        } else {
            l = new ArrayList<>(users);
        }
        return l.subList(Math.min((int) offset, l.size()), limit < 0 ? l.size() : Math.min((int) (offset + limit), l.size()));
    }

    @Override
    public List<String> searchGroups(Properties searchCriterias, long offset, long limit) {
        String filter = (String) searchCriterias.get("groupname");
        if (filter == null) {
            filter = (String) searchCriterias.get("*");
        }
        List<String> l;
        if (filter != null) {
            final String pattern = "^".concat(filter.replace("*", ".*")).concat("$");
            l = groups.stream().filter(group -> Pattern.matches(pattern, group)).collect(Collectors.toList());
        } else {
            l = new ArrayList<String>(groups);
        }
        return l.subList(Math.min((int) offset, l.size()), limit < 0 ? l.size() : Math.min((int) (offset + limit), l.size()));
    }

    @Override
    public boolean verifyPassword(String userName, String userPassword) {
        return "password".equals(userPassword);
    }

    @Override
    public boolean supportsGroups() {
        return true;
    }

    @Override
    public boolean isAvailable() throws RepositoryException {
        return true;
    }
}
