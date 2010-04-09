/*
 * eID Identity Provider Project.
 * Copyright (C) 2010 FedICT.
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

package be.fedict.eid.idp.model;

import java.security.cert.X509Certificate;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.LocalBinding;

import be.fedict.eid.applet.service.spi.AuthenticationService;
import be.fedict.trust.client.XKMS2Client;
import be.fedict.trust.client.exception.ValidationFailedException;

/**
 * eID Applet Service Authentication Service implementation.
 * 
 * @author Frank Cornelis
 * 
 */
@Stateless
@Local(AuthenticationService.class)
@LocalBinding(jndiBinding = "be/fedict/eid/idp/AuthenticationServiceBean")
public class AuthenticationServiceBean implements AuthenticationService {

	private static final Log LOG = LogFactory
			.getLog(AuthenticationServiceBean.class);

	@EJB
	private ConfigManager configManager;

	public void validateCertificateChain(List<X509Certificate> certificateChain)
			throws SecurityException {
		LOG.debug("validate certificate: "
				+ certificateChain.get(0).getSubjectX500Principal());
		String xkmsUrl = this.configManager.getXkmsUrl();
		if (null == xkmsUrl || xkmsUrl.trim().isEmpty()) {
			LOG.error("no XKMS URL configured!");
			return;
		}
		XKMS2Client xkms2Client = new XKMS2Client(xkmsUrl);
		try {
			xkms2Client.validate(certificateChain);
		} catch (ValidationFailedException e) {
			LOG.warn("invalid certificate");
			throw new SecurityException("invalid certificate");
		} catch (Exception e) {
			LOG.warn("eID Trust Service error: " + e.getMessage(), e);
			throw new SecurityException("eID Trust Service error");
		}
	}
}