/*
 * eID Identity Provider Project.
 * Copyright (C) 2011 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package be.fedict.eid.idp.sp.protocol.saml2.spi;

import be.fedict.eid.idp.common.SamlAuthenticationPolicy;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * SPI for authentication response services.
 *
 * @author Wim Vandenhaute.
 */
public interface AuthenticationResponseService {

        /**
         * Validation of the certificate chain in the SAML v2.0 response signature.
         *
         * @param authenticationPolicy SAML v2.0 authentication policy.
         * @param certificateChain     the service certificate chain
         * @throws SecurityException in case the certificate is invalid/not accepted
         */
        void validateServiceCertificate(SamlAuthenticationPolicy authenticationPolicy,
                                        List<X509Certificate> certificateChain)
                throws SecurityException;

        /**
         * @return the maximum offset allowed on the SAML v2.0 response's assertion
         *         condition fields. Specified in minutes. A negative value
         *         results in skipping validation of the condition's time
         *         fields.
         */
        int getMaximumTimeOffset();
}
