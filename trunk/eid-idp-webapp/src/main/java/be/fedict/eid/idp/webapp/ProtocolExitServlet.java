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

package be.fedict.eid.idp.webapp;

import be.fedict.eid.applet.service.Address;
import be.fedict.eid.applet.service.Identity;
import be.fedict.eid.applet.service.impl.handler.AuthenticationDataMessageHandler;
import be.fedict.eid.applet.service.impl.handler.IdentityDataMessageHandler;
import be.fedict.eid.idp.spi.IdentityProviderProtocolService;
import be.fedict.eid.idp.spi.ReturnResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.security.auth.x500.X500Principal;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.cert.X509Certificate;

/**
 * Protocol Exit Servlet. Operates as a broker towards protocol services.
 *
 * @author Frank Cornelis
 */
public class ProtocolExitServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Log LOG = LogFactory.getLog(ProtocolExitServlet.class);

    private String protocolErrorPageInitParam;

    private String protocolErrorMessageSessionAttributeInitParam;

    private String protocolResponsePostPageInitParam;

    private String responseActionSessionAttributeInitParam;

    private String responseAttributesSessionAttributeInitParam;

    @Override
    public void init(ServletConfig config) throws ServletException {
        this.protocolErrorPageInitParam = getRequiredInitParameter(config,
                "ProtocolErrorPage");
        this.protocolErrorMessageSessionAttributeInitParam = getRequiredInitParameter(
                config, "ProtocolErrorMessageSessionAttribute");
        this.protocolResponsePostPageInitParam = getRequiredInitParameter(
                config, "ProtocolResponsePostPage");
        this.responseActionSessionAttributeInitParam = getRequiredInitParameter(
                config, "ResponseActionSessionAttribute");
        this.responseAttributesSessionAttributeInitParam = getRequiredInitParameter(
                config, "ResponseAttributesSessionAttribute");
    }

    private String getRequiredInitParameter(ServletConfig config,
                                            String initParamName) throws ServletException {
        String value = config.getInitParameter(initParamName);
        if (null == value) {
            throw new ServletException(initParamName + " init-param required");
        }
        return value;
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        LOG.debug("doGet");
        IdentityProviderProtocolService protocolService = ProtocolEntryServlet
                .getProtocolService(request);

        // get eID data from Http Session
        HttpSession httpSession = request.getSession();
        Identity identity = (Identity) httpSession
                .getAttribute(IdentityDataMessageHandler.IDENTITY_SESSION_ATTRIBUTE);
        Address address = (Address) httpSession
                .getAttribute(IdentityDataMessageHandler.ADDRESS_SESSION_ATTRIBUTE);
        String authenticatedIdentifier = (String) httpSession
                .getAttribute(AuthenticationDataMessageHandler.AUTHENTICATED_USER_IDENTIFIER_SESSION_ATTRIBUTE);
        X509Certificate authnCertificate =
                (X509Certificate) httpSession.getAttribute(IdentityDataMessageHandler.AUTHN_CERT_SESSION_ATTRIBUTE);

        String userId;
        String givenName;
        String surName;
        if (null != identity) {
            userId = identity.getNationalNumber();
            givenName = identity.getFirstName();
            surName = identity.getName();
        } else {
            userId = authenticatedIdentifier;
            givenName = getGivenName(authnCertificate);
            surName = getSurName(authnCertificate);
        }

        // return protocol specific response
        ReturnResponse returnResponse;
        try {
            returnResponse = protocolService.handleReturnResponse(httpSession,
                    userId, givenName, surName, identity, address,
                    request, response);
        } catch (Exception e) {
            LOG.error("protocol error: " + e.getMessage(), e);
            httpSession.setAttribute(
                    this.protocolErrorMessageSessionAttributeInitParam, e
                    .getMessage());
            response.sendRedirect(request.getContextPath()
                    + this.protocolErrorPageInitParam);
            return;
        }
        if (null != returnResponse) {
            /*
                * This means that the protocol service wants us to construct some
                * Browser POST response towards the Service Provider landing site.
                */
            LOG.debug("constructing generic Browser POST response...");
            httpSession.setAttribute(
                    this.responseActionSessionAttributeInitParam,
                    returnResponse.getActionUrl());
            httpSession.setAttribute(
                    this.responseAttributesSessionAttributeInitParam,
                    returnResponse.getAttributes());
            response.sendRedirect(request.getContextPath()
                    + this.protocolResponsePostPageInitParam);
        }
    }

    private static String getGivenName(X509Certificate authnCertificate) {

        X500Principal subjectPrincipal = authnCertificate.getSubjectX500Principal();
        String subjectName = subjectPrincipal.toString();
        return getAttributeFromSubjectName(subjectName, "GIVENNAME");
    }

    private static String getSurName(X509Certificate authnCertificate) {

        X500Principal subjectPrincipal = authnCertificate.getSubjectX500Principal();
        String subjectName = subjectPrincipal.toString();
        return getAttributeFromSubjectName(subjectName, "SURNAME");
    }

    private static String getAttributeFromSubjectName(String subjectName, String attributeName) {

        int attributeBegin = subjectName.indexOf(attributeName + '=');
        if (-1 == attributeBegin) {
            throw new IllegalArgumentException("attribute name does not occur in subject: " + attributeName);
        }
        attributeBegin += attributeName.length() + 1; // "attributeName="
        int attributeEnd = subjectName.indexOf(',', attributeBegin);
        if (-1 == attributeEnd)
        // last field has no trailing ","
        {
            attributeEnd = subjectName.length();
        }
        return subjectName.substring(attributeBegin, attributeEnd);
    }

}