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

package be.fedict.eid.idp.entity;

import javax.persistence.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

@Entity
@Table(name = Constants.DATABASE_TABLE_PREFIX + "rp")
@NamedQueries({@NamedQuery(name = RPEntity.LIST_ALL, query = "FROM RPEntity "),
        @NamedQuery(name = RPEntity.FIND_DOMAIN, query = "SELECT rp FROM RPEntity " +
                "AS rp WHERE rp.domain = :domain")})
public class RPEntity implements Serializable {

        private static final long serialVersionUID = 1L;

        public static final String LIST_ALL = "idp.rp.list.all";
        public static final String FIND_DOMAIN = "idp.rp.find.domain";

        private long id;

        private String name;

        private String domain;
        private byte[] encodedCertificate;
        private boolean requestSigningRequired;

        private String secretKey;

        public RPEntity(String name, String domain, X509Certificate certificate,
                        boolean requestSigningRequired, String secretKey)
                throws CertificateEncodingException {

                this.name = name;
                this.domain = domain;
                this.encodedCertificate = certificate.getEncoded();
                this.requestSigningRequired = requestSigningRequired;
                this.secretKey = secretKey;
        }

        public RPEntity() {
                super();
        }

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        public long getId() {
                return this.id;
        }

        public void setId(long id) {
                this.id = id;
        }

        @Column(nullable = false)
        public String getName() {
                return this.name;
        }

        public void setName(String name) {
                this.name = name;
        }

        @Column(nullable = true)
        public String getDomain() {
                return domain;
        }

        public void setDomain(String domain) {
                this.domain = domain;
        }

        @Column(length = 4 * 1024, nullable = true)
        @Basic(fetch = FetchType.LAZY)
        public byte[] getEncodedCertificate() {
                return encodedCertificate;
        }

        public void setEncodedCertificate(byte[] encodedCertificate) {
                this.encodedCertificate = encodedCertificate;
        }

        @Transient
        public X509Certificate getCertificate() {

                if (null == this.encodedCertificate) {
                        return null;
                }
                try {
                        CertificateFactory certificateFactory = CertificateFactory
                                .getInstance("X.509");
                        InputStream certificateStream = new ByteArrayInputStream(
                                this.encodedCertificate);
                        return (X509Certificate) certificateFactory
                                .generateCertificate(certificateStream);
                } catch (CertificateException e) {
                        throw new RuntimeException("cert factory error: " + e.getMessage());
                }
        }

        @Transient
        public void setCertificate(X509Certificate certificate)
                throws CertificateEncodingException {

                this.encodedCertificate = certificate.getEncoded();
        }

        @Transient
        public String getCertificateSubject() {

                if (null == this.encodedCertificate) {
                        return null;
                }
                return getCertificate().getSubjectDN().getName();
        }

        public boolean isRequestSigningRequired() {
                return requestSigningRequired;
        }

        public void setRequestSigningRequired(boolean requestSigningRequired) {
                this.requestSigningRequired = requestSigningRequired;
        }

        @Column(nullable = true)
        public String getSecretKey() {
                return secretKey;
        }

        public void setSecretKey(String secretKey) {
                this.secretKey = secretKey;
        }

        @SuppressWarnings("unchecked")
        public static List<RPEntity> listRPs(EntityManager entityManager) {

                Query query = entityManager.createNamedQuery(LIST_ALL);
                return query.getResultList();
        }

        public static RPEntity findRP(EntityManager entityManager, String domain) {

                Query query = entityManager.createNamedQuery(FIND_DOMAIN);
                query.setParameter("domain", domain);
                try {
                        return (RPEntity) query.getSingleResult();
                } catch (NoResultException e) {
                        return null;
                }
        }
}