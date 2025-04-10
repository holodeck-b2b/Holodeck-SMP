/*
 * Copyright (C) 2023 The Holodeck B2B Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Affero GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.bdxr.smp.server.svc.peppol;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.GregorianCalendar;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLKeyException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.busdox.servicemetadata.locator._1.MigrationRecordType;
import org.busdox.servicemetadata.locator._1.ObjectFactory;
import org.busdox.servicemetadata.locator._1.PublisherEndpointType;
import org.busdox.servicemetadata.locator._1.ServiceMetadataPublisherServiceForParticipantType;
import org.busdox.servicemetadata.locator._1.ServiceMetadataPublisherServiceType;
import org.busdox.transport.identifiers._1.ParticipantIdentifierType;
import org.holodeckb2b.bdxr.smp.server.datamodel.Participant;
import org.holodeckb2b.bdxr.smp.server.db.CertificateUpdate;
import org.holodeckb2b.bdxr.smp.server.db.SMLRegistration;
import org.holodeckb2b.bdxr.smp.server.db.SMLRegistrationRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ParticipantRepository;
import org.holodeckb2b.bdxr.smp.server.svc.ISMLIntegrator;
import org.holodeckb2b.bdxr.smp.server.svc.SMLException;
import org.holodeckb2b.bdxr.smp.server.svc.SMPCertificateService;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.ws.transport.http.HttpComponents5MessageSender;

import ec.services.wsdl.bdmsl.data._1.PrepareChangeCertificateType;
import jakarta.xml.bind.JAXBElement;

/**
 * Implements the integration of the SMP with the Peppol SML.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Service("PEPPOLSMLClient")
public class SMLClient implements ISMLIntegrator {

	@Value("${sml.sml_url:https://edelivery.tech.ec.europa.eu/edelivery-sml}")
	protected String smlURL;
	@Value("${sml.smk_url:https://acc.edelivery.tech.ec.europa.eu/edelivery-sml}")
	protected String smkURL;
	@Value("${sml.ssl-truststore:}")
	protected String sslTrustStorePath;
	@Value("${sml.ssl-truststore-pwd:}")
	protected String sslTrustStorePwd;

	@Autowired
	protected SMLRegistrationRepository	smlRegs;
	@Autowired
	protected SMPCertificateService	certSvc;
	@Autowired
	protected ParticipantRepository participants;

	private String targetURL;

	/**
	 * @return <code>true</code> when the SMP is registered in the SML, <code>false</code> otherwise
	 */
	@Override
	public boolean isSMPRegistered() {
		return smlRegs.count() > 0;
	}

	@Override
	public boolean requiresSMPCertRegistration() {
		return true;
	}

	/**
	 * Saves the SMP meta-data to the SML by executing either the <i>Create</i> or <i>Update</i> operation depending
	 * on whether the SMP was already registered at the SML.
	 *
	 * @param data		the meta-data of the SMP to register in the SML
	 * @param isUpdate	indicates whether this is an update of an existing registration
	 * @throws SSLException	if the key pair or trust store to use for the mutual TLS authentication to the SML cannot
	 *						be loaded
	 * @throws SoapFaultClientException when the SML responded with an error message to the registration (update)
	 *									request
	 */
	public void saveSMPRegistration(SMLRegistration data, boolean isUpdate) throws SSLException, SoapFaultClientException {
		ServiceMetadataPublisherServiceType smp = new ServiceMetadataPublisherServiceType();
		smp.setServiceMetadataPublisherID(data.getIdentifier());
		PublisherEndpointType endpoint = new PublisherEndpointType();
		endpoint.setPhysicalAddress(data.getIpAddress());
		endpoint.setLogicalAddress("http://" + data.getHostname());
		smp.setPublisherEndpoint(endpoint);

		JAXBElement request;
		String action;
		if (!isUpdate) {
			request = new ObjectFactory().createCreateServiceMetadataPublisherService(smp);
			action = "http://busdox.org/serviceMetadata/ManageServiceMetadataService/1.0/:createIn";
		} else {
			request = new ObjectFactory().createUpdateServiceMetadataPublisherService(smp);
			action = "http://busdox.org/serviceMetadata/ManageServiceMetadataService/1.0/:updateIn";
		}

		webServiceTemplate().marshalSendAndReceive(baseURL() + "/manageservicemetadata", request,
													new SoapActionCallback(action));
		smlRegs.save(data);
	}

	/**
	 * Removes the SMP meta-data from the SML by executing the <i>delete</i> operation.
	 *
	 * @throws SSLException	if the key pair or trust store to use for the mutual TLS authentication to the SML cannot
	 *						be loaded
	 * @throws SoapFaultClientException when the SML responded with an error message to the removal request
	 */
	public void removeSMPRegistration() throws SSLException, SoapFaultClientException {
		try {
			SMLRegistration reg = getSMLRegistration();
			webServiceTemplate().marshalSendAndReceive(baseURL() + "/manageservicemetadata",
										new ObjectFactory().createServiceMetadataPublisherID(reg.getIdentifier()),
										new SoapActionCallback("http://busdox.org/serviceMetadata/ManageServiceMetadataService/1.0/:deleteIn"));
			smlRegs.delete(reg);
			participants.unregisterAllFromSML();
		} catch (CertificateException regMissing) {
			throw new IllegalStateException();
		}
	}

	/**
	 * Registers the new SMP certificate in the SML and saves the pending update to the database.
	 *
	 * @param kp			the new key pair to activate
	 * @param activation	date on which the certificate will be activated
	 * @throws CertificateException when the given certificate or activation date cannot be encoded correctly
	 * @throws SSLException	if the key pair or trust store to use for the mutual TLS authentication to the SML cannot
	 *						be loaded
	 * @throws SoapFaultClientException when the SML responded with an error message to the certificate update request
	 * @since 1.1.0
	 */
	public void registerNewSMPCertificate(KeyStore.PrivateKeyEntry kp, LocalDate activation) throws CertificateException,
																				SSLException, SoapFaultClientException {

		if (!isSMPRegistered())
			throw new IllegalStateException("SMP is not registred in SML");

		try {
			PrepareChangeCertificateType certUpdate = new PrepareChangeCertificateType();
			XMLGregorianCalendar xmlDate = DatatypeFactory.newInstance()
														  .newXMLGregorianCalendar(GregorianCalendar.from(
																  activation.atStartOfDay(ZoneOffset.UTC)));
			certUpdate.setMigrationDate(xmlDate);
			certUpdate.setNewCertificatePublicKey(CertificateUtils.getPEMEncoded((X509Certificate) kp.getCertificate()));

			webServiceTemplate().marshalSendAndReceive(baseURL() + "/bdmslservice",
						new ec.services.wsdl.bdmsl.data._1.ObjectFactory().createPrepareChangeCertificate(certUpdate),
						new SoapActionCallback("ec:services:wsdl:BDMSL:1.0:prepareChangeCertificateIn"));
		} catch (DatatypeConfigurationException dex) {
			throw new CertificateException("Could not convert activation date", dex);
		}

		SMLRegistration reg = getSMLRegistration();
		reg.setPendingCertUpdate(new CertificateUpdate(kp, activation));
		smlRegs.save(reg);
	}
	
	/**
	 * Removes the pending certificate update.
	 */
	public void clearPendingUpdate() {		
		SMLRegistration registration;
		try {
			registration = getSMLRegistration();
		} catch (CertificateException invalidConfig) {
			registration = null;
		}
		if (registration == null)
			throw new IllegalStateException("SMP is not registred in SML");

		registration.setPendingCertUpdate(null);
		smlRegs.save(registration);
	}

	/**
	 * Registers the Participant in the SML.
	 *
	 * @param p	the meta-data on the Participant to register
	 * @throws SMLException when the Participant could not be registered in the SML
	 */
	@Override
	public void registerParticipant(Participant p) throws SMLException {
		updateParticipant(p, (pi) -> new ObjectFactory().createCreateParticipantIdentifier(pi),
						  "http://busdox.org/serviceMetadata/ManageBusinessIdentifierService/1.0/         :createIn");
	}

	/**
	 * Removes the Participant's registration from the SML.
	 *
	 * @param p	the meta-data on the Participant whose registration should be removed
	 * @throws SMLException when the Participant's registration could not be removed in the SML
	 */
	@Override
	public void unregisterParticipant(Participant p) throws SMLException {
		updateParticipant(p, (pi) -> new ObjectFactory().createDeleteParticipantIdentifier(pi),
						  "http://busdox.org/serviceMetadata/ManageBusinessIdentifierService/1.0/         :deleteIn");
	}

	/**
	 * Executes the actual registration or removal of the Participant's registration in/from the SML.
	 *
	 * @param p	the meta-data on the Participant
	 * @param f	function to create the correct root element, given the content
	 * @param action	the SOAP action to use
	 * @throws SMLException	when there is an error executing the update to the SML
	 */
	private void updateParticipant(Participant p,
								   Function<ServiceMetadataPublisherServiceForParticipantType, JAXBElement> f,
								   String action) throws SMLException {
		
		ServiceMetadataPublisherServiceForParticipantType pInfo = new ServiceMetadataPublisherServiceForParticipantType();
		pInfo.setServiceMetadataPublisherID(getSMPId());
		ParticipantIdentifierType partID = new ParticipantIdentifierType();
		partID.setScheme(p.getId().getScheme() != null ? p.getId().getScheme().getSchemeId() : null);
		partID.setValue(p.getId().getValue());
		pInfo.setParticipantIdentifier(partID);

		try {
			webServiceTemplate().marshalSendAndReceive(baseURL() + "/manageparticipantidentifier", f.apply(pInfo),
													new SoapActionCallback(action));
		} catch (Exception requestFailed) {
			throw new SMLException(requestFailed);
		}
	}
	
	/**
	 * Prepares the migration of the Participant by registering the migration code in the SML.
	 * 
	 * @param p		the meta-data on the Participant
	 * @param code	the migration code
	 * @throws SMLException when there is an error executing the update to the SML
	 */
	public void registerMigrationCode(Participant p, String code) throws SMLException {
		MigrationRecordType migrationRecord = new MigrationRecordType();
		migrationRecord.setServiceMetadataPublisherID(getSMPId());
		ParticipantIdentifierType partID = new ParticipantIdentifierType();
		partID.setScheme(p.getId().getScheme() != null ? p.getId().getScheme().getSchemeId() : null);
		partID.setValue(p.getId().getValue());
		migrationRecord.setParticipantIdentifier(partID);
		migrationRecord.setMigrationKey(code);
		
		try {
			webServiceTemplate().marshalSendAndReceive(baseURL() + "/manageparticipantidentifier", 
				new ObjectFactory().createPrepareMigrationRecord(migrationRecord),
				new SoapActionCallback("http://busdox.org/serviceMetadata/ManageBusinessIdentifierService/1.0/         :prepareMigrateIn")
			);
		} catch (Exception requestFailed) {
			throw new SMLException(requestFailed);
		}
	}

	/**
	 * Migrates the Participant to this SMP using the provided migration code.
	 * 
	 * @param p		the meta-data on the Participant being migrated
	 * @param code	the migration code
	 * @throws SMLException when there is an error executing the update to the SML
	 */
	public void migrateParticipant(Participant p, String code) throws SMLException {
		MigrationRecordType migrationRecord = new MigrationRecordType();
		migrationRecord.setServiceMetadataPublisherID(getSMPId());		
		ParticipantIdentifierType partID = new ParticipantIdentifierType();
		partID.setScheme(p.getId().getScheme() != null ? p.getId().getScheme().getSchemeId() : null);
		partID.setValue(p.getId().getValue());
		migrationRecord.setParticipantIdentifier(partID);
		migrationRecord.setMigrationKey(code);
		
		try {
			webServiceTemplate().marshalSendAndReceive(baseURL() + "/manageparticipantidentifier", 
				new ObjectFactory().createCompleteMigrationRecord(migrationRecord),
				new SoapActionCallback("http://busdox.org/serviceMetadata/ManageBusinessIdentifierService/1.0/         :migrateIn")
			);
		} catch (Exception requestFailed) {
			throw new SMLException(requestFailed);
		}
	}
	
	
	/**
	 * Gets the SMP meta-data currently registered in the SML, or if none is registered an empty record.
	 *
	 * @return	the registered meta-data, or a new empty record when no data is registered.
	 * @throws CertificateException when there is no SMP certificate installed or the installed certificate is not
	 *								issued by the Peppol CA
	 */
	public SMLRegistration getSMLRegistration() throws CertificateException {
		SMLRegistration reg = smlRegs.findAll().stream().findFirst().orElse(null);
		if (reg == null) {
			X509Certificate cert;
			try {
				KeyStore.PrivateKeyEntry keyPair = certSvc.getKeyPair();
				cert = keyPair != null ? (X509Certificate) keyPair.getCertificate() : null;
			} catch (CertificateException noCert) {
				cert = null;
			}
			if (cert == null)
				throw new CertificateException("There is no SMP certificate configured");
			String issuerName = CertificateUtils.getIssuerName(cert).toLowerCase();
			if (!issuerName.contains("peppol"))
				throw new CertificateException("The configured SMP certificate is not a Peppol issued certificate");

			reg = new SMLRegistration(issuerName.contains("test") ? "SMK" : "SML");
		}
		return reg;
	}

	/**
	 * Gets the identifier of the SMP currently registered in the SML.
	 * 
	 * @return the SMP identifier
	 * @throws SMLException when the SMP is not registered in the SML
	 */
	private String getSMPId() throws SMLException {
		SMLRegistration smlReg;
		try {
			smlReg = getSMLRegistration();
		} catch (CertificateException inavlidSMLReg) {
			smlReg = null;
		}
		if (smlReg == null || Utils.isNullOrEmpty(smlReg.getIdentifier()))
			throw new SMLException("The SMP is not registered in the SML");

		return smlReg.getIdentifier();
	}
	
	/**
	 * Determines the base URL of the SML interface based on the installed SMP certificate. The default Peppol URLs can
	 * be overriden by setting the <code>sml.sml_url</code> and <code>sml.smk_url</code> application properties in
	 * <code>common.properties</code>.
	 *
	 * @return	the URL where the SML interface is located
	 */
	private String baseURL() {
		if (targetURL == null) {
			try {
				KeyStore.PrivateKeyEntry keyPair = certSvc.getKeyPair();
				targetURL = CertificateUtils.getIssuerName((X509Certificate) keyPair.getCertificate()).toLowerCase()
						.contains("test") ? smkURL : smlURL;
			} catch (CertificateException ex) {
				Logger.getLogger(SMLClient.class.getName()).log(Level.SEVERE,
																"Could not retrieve SMP cert : {0}", ex.getMessage());
			}
		}
		return targetURL;
	}

	/**
	 * Prepares a Spring {@link WebServiceTemplate} for a call to the SML interface.
	 *
	 * @return a {@link WebServiceTemplate} instance configured for executing calls to the SML
	 * @throws SSLException
	 */
	private WebServiceTemplate webServiceTemplate() throws SSLException {
		WebServiceTemplate webServiceTemplate;

		Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
		jaxb2Marshaller.setContextPaths("ec.services.wsdl.bdmsl.data._1", "org.busdox.servicemetadata.locator._1");
		webServiceTemplate = new WebServiceTemplate();
		webServiceTemplate.setMarshaller(jaxb2Marshaller);
		webServiceTemplate.setUnmarshaller(jaxb2Marshaller);

		/* When the configured SML base URL is for localhost we turn off hostname verification as otherwise the
		 * following error is probably thrown:
		 *		java.security.cert.CertificateException: No name matching localhost found
		 */
		KeyStore.PrivateKeyEntry keyPair;
		try {
			keyPair = certSvc.getKeyPair();
		} catch (CertificateException ex) {
			throw new SSLException("Could not retrieve SMP key pair");
		}
		SSLConnectionSocketFactory sslFactory = baseURL().contains("localhost") ?
													new SSLConnectionSocketFactory(sslContext(keyPair),
																				   NoopHostnameVerifier.INSTANCE)
												  : new SSLConnectionSocketFactory(sslContext(keyPair));

		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("https", sslFactory)
				.register("http", new PlainConnectionSocketFactory())
				.build();
		
		webServiceTemplate.setMessageSender(
				new SMLMessageSender(
						HttpClients.custom()
								.setConnectionManager(new BasicHttpClientConnectionManager(socketFactoryRegistry))
								.addRequestInterceptorFirst(new HttpComponents5MessageSender.RemoveSoapHeadersInterceptor())
								.build()
						));

		return webServiceTemplate;
	}

	/**
	 * Creates the <code>SSLContext</code> for the connections to the SML. It uses the given SMP key pair for client
	 * authentication and can use a customised trust store for validation of the SML server certificate. To use a
	 * customised trust store the <code>sml.ssl-truststore</code> and <code>sml.ssl-truststore-pwd</code> application
	 * properties must be set in <code>common.properties</code>.
	 *
	 * @param pk	the key pair to use for client authentication
	 * @return	the SSLContext for creating connections to the SML
	 * @throws SSLException	when either the key pair for client authentication or the custom trust store for server
	 *						authentication cannot be processed
	 */
	private SSLContext sslContext(KeyStore.PrivateKeyEntry pk) throws SSLException {
		SSLContextBuilder ctxBldr = SSLContexts.custom();
		try {
			char[] pwd = new char[] {};
			KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(null, null);
			keyStore.setEntry("1", pk, new KeyStore.PasswordProtection(pwd));
			ctxBldr.loadKeyMaterial(keyStore, pwd);
		} catch (IOException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException
				| CertificateException keyFailure) {
			throw new SSLKeyException("Could not create key store for client authentication");
		}
		try {
			if (!Utils.isNullOrEmpty(sslTrustStorePath))
				ctxBldr.loadTrustMaterial(new File(sslTrustStorePath), sslTrustStorePwd.toCharArray());
		} catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException trustFailure) {
			throw new SSLKeyException("Could not load specified trust store for TLS authentication");
		}
		try {
			return ctxBldr.build();
		} catch (NoSuchAlgorithmException | KeyManagementException ex) {
			Logger.getLogger(SMLClient.class.getName()).severe("Error creating SSL context : "
																+ Utils.getExceptionTrace(ex));
			throw new SSLException("Could not setup SSL context for SML connection", ex);
		}
	}
}
