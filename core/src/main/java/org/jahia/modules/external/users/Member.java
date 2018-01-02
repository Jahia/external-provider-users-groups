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

import org.apache.commons.lang.StringUtils;

/**
 * Class used to describe a member with a name and a type (user or group)
 */
public class Member {

    /**
     * Supported group member types. 
     */
    public enum MemberType {
        USER, GROUP
    };

    private int hash;
    
    private String name;

    private MemberType type;

    /**
     * Initializes an instance of this class.
     * 
     * @param name
     *            the name of the group member
     * @param type
     *            the member type
     */
    public Member(String name, MemberType type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Returns the name of the group member.
     * 
     * @return the name of the group member
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the type of the group member.
     * 
     * @return the type of the group member
     */
    public MemberType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (null == o || o.getClass() != this.getClass()) {
            return false;
        }

        Member otherMember = (Member) o;

        return (type == otherMember.type) && StringUtils.equals(name, otherMember.name);
    }

    private int getHashCode() {
        int iTotal = 17;
        iTotal = 37 * iTotal + (name != null ? name.hashCode() : 0);
        iTotal = 37 * iTotal + (type != null ? type.hashCode() : 0);

        return iTotal;
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            hash = getHashCode();
        }
        
        return hash;
    }
}
