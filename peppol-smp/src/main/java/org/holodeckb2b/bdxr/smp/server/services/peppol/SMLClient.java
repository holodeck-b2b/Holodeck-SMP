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
package org.holodeckb2b.bdxr.smp.server.services.peppol;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.GregorianCalendar;
import java.util.function.Function;
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
import org.holodeckb2b.bdxr.smp.datamodel.Certificate;
import org.holodeckb2b.bdxr.smp.server.datamodel.Participant;
import org.holodeckb2b.bdxr.smp.server.datamodel.SMPServerMetadata;
import org.holodeckb2b.bdxr.smp.server.services.core.SMPServerAdminService;
import org.holodeckb2b.bdxr.smp.server.services.network.SMLException;
import org.holodeckb2b.bdxr.smp.server.services.network.SMLIntegrationService;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.ws.transport.http.HttpComponents5MessageSender;

import ec.services.wsdl.bdmsl.data._1.PrepareChangeCertificateType;
import jakarta.xml.bind.JAXBElement;
import lombok.extern.slf4j.Slf4j;

/**
 * Implements the integration of the SMP with the Peppol SML.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Slf4j
@Service("PEPPOLSMLClient")
public class SMLClient implements SMLIntegrationService {

	@Value("${peppol.sml.prod.name:Peppol Production SML}")
	protected String prodName;
	@Value("${peppol.sml.prod.url:https://edelivery.tech.ec.europa.eu/edelivery-sml}")
	protected String prodURL;

	@Value("${peppol.sml.acc.name:Peppol Acceptance SML (SMK)}")
	protected String testName;
	@Value("${peppol.sml.acc.url:https://acc.edelivery.tech.ec.europa.eu/edelivery-sml}")
	protected String testURL;

	@Value("${peppol.sml.ssl.verifyhostname:true}")
	protected boolean verifyHostname;

	@Value("${peppol.sml.ssl.truststore:}")
	protected String sslTrustStorePath;
	@Value("${peppol.sml.ssl.truststore.pwd:}")
	protected String sslTrustStorePwd;

	@Lazy
	@Autowired
	protected SMPServerAdminService	adminSvc;
	
	@Override
	public boolean requiresSMPRegistration() {
		return true;
	}

	@Override
	public boolean requiresSMPCertRegistration() {
		return true;
	}

	@Override
	public String getSMLName() {
		X509Certificate smpCert = adminSvc.getServerMetadata().getCertificate();
		if (smpCert == null) {
			log.warn("SML name cannot be determined as SMP certificate is not available!");
			return null;
		}
		return CertificateUtils.getIssuerName(smpCert).toLowerCase().contains("test") ? testName : prodName;
	}

	@Override
	public void registerSMPServer(SMPServerMetadata smp) throws SMLException {
		saveSMPRegistration(smp, false);		
	}

	@Override
	public void updateSMPServer(SMPServerMetadata smp) throws SMLException {
		saveSMPRegistration(smp, true);
	}
	
	@Override
	public void deregisterSMPServer(String smpId) throws SMLException {
		try {
			webServiceTemplate().marshalSendAndReceive(baseURL() + "/manageservicemetadata",
										new ObjectFactory().createServiceMetadataPublisherID(smpId),
										new SoapActionCallback("http://busdox.org/serviceMetadata/ManageServiceMetadataService/1.0/:deleteIn"));
		} catch (IOException connectionError) {
			log.error("A connection error occurred while executing SML request (remove) : {}",
						Utils.getExceptionTrace(connectionError));
			throw new SMLException("Connection error", connectionError);
		} catch (SoapFaultClientException smlError) {
			log.warn("Error response from SML while executing SML request (remove) : {}", 
					Utils.getRootCause(smlError).getMessage());
			throw new SMLException("SML error response", smlError);
		}
	}
	
	/**
	 * Saves the SMP meta-data to the SML by executing either the <i>Create</i> or <i>Update</i> operation depending
	 * on whether the SMP was already registered at the SML.
	 *
	 * @param data		the meta-data of the SMP to register in the SML
	 * @param isUpdate	indicates whether this is an update of an existing registration
	 * @throws SMLException if an error occurs creating/updating the SMP registration in the SML. This can be caused by
	 * 						a connection error, missing configuration or an error response from the SML
	 */
	private void saveSMPRegistration(SMPServerMetadata data, boolean isUpdate) throws SMLException {
		ServiceMetadataPublisherServiceType smp = new ServiceMetadataPublisherServiceType();
		smp.setServiceMetadataPublisherID(data.getSMPId());
		PublisherEndpointType endpoint = new PublisherEndpointType();
		endpoint.setPhysicalAddress(data.getIPv4Address());
		endpoint.setLogicalAddress(data.getBaseUrl().toString());
		smp.setPublisherEndpoint(endpoint);

		@SuppressWarnings("rawtypes")
		JAXBElement request;
		String action;
		if (!isUpdate) {
			request = new ObjectFactory().createCreateServiceMetadataPublisherService(smp);
			action = "http://busdox.org/serviceMetadata/ManageServiceMetadataService/1.0/:createIn";
		} else {
			request = new ObjectFactory().createUpdateServiceMetadataPublisherService(smp);
			action = "http://busdox.org/serviceMetadata/ManageServiceMetadataService/1.0/:updateIn";
		}

		try {
			webServiceTemplate().marshalSendAndReceive(baseURL() + "/manageservicemetadata", request,
														new SoapActionCallback(action));
		} catch (IOException connectionError) {
			log.error("A connection error occurred while executing SML request ({}) : {}", 
						isUpdate ? "update" : "create", Utils.getExceptionTrace(connectionError));
			throw new SMLException("Connection error", connectionError);
		} catch (SoapFaultClientException smlError) {
			log.warn("Error response from SML while executing SML request ({}) : {}", isUpdate ? "update" : "create",
					Utils.getRootCause(smlError).getMessage());
			throw new SMLException("SML error response", smlError);
		}
	}

	@Override
	public void updateSMPCertificate(String smpId, Certificate cert) throws SMLException {
		try {
			PrepareChangeCertificateType certUpdate = new PrepareChangeCertificateType();
			XMLGregorianCalendar xmlDate = DatatypeFactory.newInstance()
														  .newXMLGregorianCalendar(GregorianCalendar.from(
																  					cert.getActivationDate()));
			certUpdate.setMigrationDate(xmlDate);
			certUpdate.setNewCertificatePublicKey(CertificateUtils.getPEMEncoded(cert.getX509Cert()));

			webServiceTemplate().marshalSendAndReceive(baseURL() + "/bdmslservice",
						new ec.services.wsdl.bdmsl.data._1.ObjectFactory().createPrepareChangeCertificate(certUpdate),
						new SoapActionCallback("ec:services:wsdl:BDMSL:1.0:prepareChangeCertificateIn"));
		} catch (DatatypeConfigurationException | CertificateException invalidData) {
			log.error("Invalid data for updating SMP certificate: {}", Utils.getExceptionTrace(invalidData));
			throw new SMLException("Invalid SMP certificate data", invalidData);
		} catch (IOException connectionError) {
			log.error("A connection error occurred while executing SML request (cert) : {}",
					Utils.getExceptionTrace(connectionError));
			throw new SMLException("Connection error", connectionError);
		} catch (SoapFaultClientException smlError) {
			log.warn("Error response from SML while executing SML request (cert) : {}", 
					Utils.getRootCause(smlError).getMessage());
			throw new SMLException("SML error response", smlError);
		}		
	}
	

	@Override
	public boolean isRegistered(Participant p) throws SMLException {
		// TODO Auto-generated method stub
		return false;
	}
			
	@Override
	public void registerParticipant(Participant p) throws SMLException {
		updateParticipant(p, (pi) -> new ObjectFactory().createCreateParticipantIdentifier(pi),
						  "http://busdox.org/serviceMetadata/ManageBusinessIdentifierService/1.0/         :createIn");
	}

	public void deregisterParticipant(Participant p) throws SMLException {
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
								   @SuppressWarnings("rawtypes") 
								   Function<ServiceMetadataPublisherServiceForParticipantType, JAXBElement> f,
								   String action) throws SMLException {
		
		ServiceMetadataPublisherServiceForParticipantType pInfo = new ServiceMetadataPublisherServiceForParticipantType();
		pInfo.setServiceMetadataPublisherID(adminSvc.getServerMetadata().getSMPId());
		ParticipantIdentifierType partID = new ParticipantIdentifierType();
		partID.setScheme(p.getId().getScheme() != null ? p.getId().getScheme().getSchemeId() : null);
		partID.setValue(p.getId().getValue());
		pInfo.setParticipantIdentifier(partID);

		try {
			webServiceTemplate().marshalSendAndReceive(baseURL() + "/manageparticipantidentifier", f.apply(pInfo),
													new SoapActionCallback(action));
		} catch (IOException connectionError) {
			log.error("A connection error occurred while executing SML request (cert) : {}",
					Utils.getExceptionTrace(connectionError));
			throw new SMLException("Connection error", connectionError);
		} catch (SoapFaultClientException smlError) {
			log.warn("Error response from SML while executing SML request (cert) : {}", 
					Utils.getRootCause(smlError).getMessage());
			throw new SMLException("SML error response", smlError);
		}		
	}
	
	@Override
	public void registerMigrationCode(Participant p, String code) throws SMLException {
		MigrationRecordType migrationRecord = new MigrationRecordType();
		migrationRecord.setServiceMetadataPublisherID(adminSvc.getServerMetadata().getSMPId());
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
		} catch (IOException connectionError) {
			log.error("A connection error occurred while executing SML request (prepmigrate) : {}",
					Utils.getExceptionTrace(connectionError));
			throw new SMLException("Connection error", connectionError);
		} catch (SoapFaultClientException smlError) {
			log.warn("Error response from SML while executing SML request (prepmigrate) : {}", 
					Utils.getRootCause(smlError).getMessage());
			throw new SMLException("SML error response", smlError);
		}		
	}

	@Override
	public void migrateParticipant(Participant p, String code) throws SMLException {
		MigrationRecordType migrationRecord = new MigrationRecordType();
		migrationRecord.setServiceMetadataPublisherID(adminSvc.getServerMetadata().getSMPId());		
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
		} catch (IOException connectionError) {
			log.error("A connection error occurred while executing SML request (migrate) : {}",
					Utils.getExceptionTrace(connectionError));
			throw new SMLException("Connection error", connectionError);
		} catch (SoapFaultClientException smlError) {
			log.warn("Error response from SML while executing SML request (migrate) : {}", 
					Utils.getRootCause(smlError).getMessage());
			throw new SMLException("SML error response", smlError);
		}		
	}
	
	/**
	 * Determines the base URL of the SML interface based on the installed SMP certificate. The default Peppol URLs can
	 * be overridden by setting the <code>peppol.sml.prod.url</code> and <code>peppol.sml.acc.url</code> application 
	 * properties in <code>common.properties</code>.
	 *
	 * @return	the URL where the SML interface is located
	 */
	private String baseURL() {
		X509Certificate smpCert = adminSvc.getServerMetadata().getCertificate();
		if (smpCert == null) {
			log.error("SML function called before SMP certificate is available!");
			return null;
		} else
			return CertificateUtils.getIssuerName(smpCert).toLowerCase().contains("test") ? testURL : prodURL;		
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
		
		SSLConnectionSocketFactory sslFactory = verifyHostname ?
				 									new SSLConnectionSocketFactory(sslContext()) :
													new SSLConnectionSocketFactory(sslContext(),
																				   NoopHostnameVerifier.INSTANCE);

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
	 * @return	the SSLContext for creating connections to the SML
	 * @throws SSLException	when either the key pair for client authentication or the custom trust store for server
	 *						authentication cannot be processed
	 */
	private SSLContext sslContext() throws SSLException {
		SSLContextBuilder ctxBldr = SSLContexts.custom();
		try {
			char[] pwd = new char[] {};
			KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(null, null);
			keyStore.setEntry("1", adminSvc.getActiveKeyPair(), new KeyStore.PasswordProtection(pwd));
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
