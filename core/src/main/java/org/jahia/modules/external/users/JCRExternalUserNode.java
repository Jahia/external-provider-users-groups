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

import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.external.ExternalNodeImpl;
import org.jahia.modules.external.users.impl.UserDataSource;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRUserNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

/**
 * Extension of the user node in JCR to support external providers. 
 */
public class JCRExternalUserNode extends JCRUserNode {

    private static final Logger logger = LoggerFactory.getLogger(JCRExternalUserNode.class);

    /**
     * Initializes an instance of this class.
     * 
     * @param node
     *            the underlying JCR node
     */
    public JCRExternalUserNode(JCRNodeWrapper node) {
        super(node);
    }

    @Override
    public boolean isPropertyEditable(String name) {
        if (node.getRealNode() instanceof ExternalNodeImpl) {
            ExternalNodeImpl externalNode = (ExternalNodeImpl) node.getRealNode();
            try {
                if (!externalNode.canItemBeExtended(externalNode.getPropertyDefinition(name))) {
                    return false;
                }
            } catch (RepositoryException e) {
                logger.error("Error while verifying definition of property " + name, e);
            }
        }

        return super.isPropertyEditable(name);
    }

    @Override
    public boolean setPassword(String pwd) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean verifyPassword(String userPassword) {
        UserDataSource dataSource = (UserDataSource) ((ExternalContentStoreProvider) getProvider()).getDataSource();
        return dataSource.getUserGroupProvider().verifyPassword(getName(), userPassword);
    }
}
