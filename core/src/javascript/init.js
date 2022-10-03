import {registry} from '@jahia/ui-extender';
import i18next from 'i18next';
import registrations from './registrations';

export default function () {
    registry.add('callback', 'external-provider-users-groups', {
        targets: ['jahiaApp-init:2'],
        callback: () => {
            i18next.loadNamespaces('external-provider-users-groups');
            registrations();
            console.log('%c External Provider Users Groups routes have been registered', 'color: #3c8cba');
        }
    });
}
