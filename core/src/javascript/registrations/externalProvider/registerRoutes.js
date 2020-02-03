import {registry} from '@jahia/ui-extender';

export const registerRoutes = function (t) {
    const level = 'server';
    const parentTarget = 'administration-server';

    const path = '/administration/manageUserGroupProviders';
    const route = 'manageUserGroupProviders';
    registry.addOrReplace('adminRoute', `${level}-${path.toLowerCase()}`, {
        id: route,
        targets: [`${parentTarget}-usersandroles:3`],
        path: path,
        route: route,
        defaultPath: path,
        icon: null,
        label: t('externalProvider.label'),
        childrenTarget: 'usersandroles',
        isSelectable: true,
        level: level
    });
};
