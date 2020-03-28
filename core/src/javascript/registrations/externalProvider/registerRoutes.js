import {registry} from '@jahia/ui-extender';

export const registerRoutes = function () {
    registry.add('adminRoute', 'manageUserGroupProviders', {
        targets: ['administration-server-usersAndRoles:60'],
        requiredPermission: 'adminUsers',
        icon: null,
        label: 'external-provider-users-groups:externalProvider.label',
        isSelectable: true,
        iframeUrl: window.contextJsParameters.contextPath + '/cms/adminframe/default/en/settings.manageUserGroupProviders.html?redirect=false'

    });
};
