import {registerRoutes as registerExternalProviderRoutes} from './externalProvider/registerRoutes';
import {useTranslation} from 'react-i18next';

export default function () {
    const {t} = useTranslation('external-provider-users-groups');

    registerExternalProviderRoutes(t);

    return null;
}
