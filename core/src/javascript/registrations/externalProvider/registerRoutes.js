import {registry} from '@jahia/ui-extender';

export const registerRoutes = function () {
    const level = 'server';
    const parentTarget = 'administration-server';

    const path = '/administration/manageUserGroupProviders';
    const route = 'manageUserGroupProviders';
    registry.add('adminRoute', `${level}-${path.toLowerCase()}`, {
        id: route,
        targets: [`${parentTarget}-usersandroles:3`],
        path: path,
        route: route,
        defaultPath: path,
        icon: null,
        label: 'external-provider-users-groups:externalProvider.label',
        childrenTarget: 'usersandroles',
        isSelectable: true,
        level: level
    });
};
