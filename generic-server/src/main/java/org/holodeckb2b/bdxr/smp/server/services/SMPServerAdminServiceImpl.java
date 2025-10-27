package org.holodeckb2b.bdxr.smp.server.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Iterator;

import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogRecord;
import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogService;
import org.holodeckb2b.bdxr.smp.server.datamodel.NetworkServicesData;
import org.holodeckb2b.bdxr.smp.server.datamodel.SMPServerMetadata;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedCertificate;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServerConfigEntity;
import org.holodeckb2b.bdxr.smp.server.db.repos.ParticipantRepository;
import org.holodeckb2b.bdxr.smp.server.db.repos.ServerConfigRepository;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.services.core.SMPServerAdminService;
import org.holodeckb2b.bdxr.smp.server.services.network.DirectoryIntegrationService;
import org.holodeckb2b.bdxr.smp.server.services.network.SMLException;
import org.holodeckb2b.bdxr.smp.server.services.network.SMLIntegrationService;
import org.holodeckb2b.bdxr.smp.server.utils.DataEncryptor;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.security.KeystoreUtils;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * Implements the {@link SMPServerAdminService} and provides the API to access and manage the SMP server configuration.
 * Some settings can be managed during run-time, like the SMP certificate and the external URL and IP addresses where
 * the SMP server can be reached from the eDelivery network. Other settings, like the implementations to use for 
 * integrating with the network's SML and Directory services however are statically configured.    
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Service
@Slf4j
public class SMPServerAdminServiceImpl implements SMPServerAdminService {

	@Autowired
	protected AuditLogService auditSvc;
	
	@Autowired
	protected ServerConfigRepository	configRepo;
	
	@Autowired
	protected ParticipantRepository 	participants;
	
	@Autowired
	private DataEncryptor	encryptor;
	
	/**
	 * The SML Integration Service implementation. As the SML integration is optional the autowiring is optional. 
	 * To allow the SML integration service to reference this service (because it may need information provided by it) 
	 * the implementation is loaded lazily. 
	 */
	@Autowired(required = false)
	private SMLIntegrationService 			smlServiceImpl;
	/**
	 * The Directory Integration Service implementation. Same as for the SML integration, the directory integration is 
	 * optional and may need information from this service and the autowiring is therefore optional and lazy.
	 */
	@Autowired(required = false)
	private DirectoryIntegrationService 	dirServiceImpl;
	
	@Override
	@Transactional(rollbackFor = SMLException.class)
	public void registerCertificate(UserDetails user, PrivateKeyEntry keypair) 
																			throws CertificateException, SMLException {
		registerCertificate(user, keypair, null);
	}

	@Override
	@Transactional(rollbackFor = SMLException.class)
	public void registerCertificate(UserDetails user, PrivateKeyEntry keypair, ZonedDateTime activation)
																			throws CertificateException, SMLException {
		if (activation != null) 
			log.trace("Request to schedule server certificate update");
		else
			log.trace("Request to update server certificate");
		
		ZonedDateTime start = activation == null ? ZonedDateTime.now() : activation;
		
		X509Certificate newCert = (X509Certificate) keypair.getCertificate();
		ZonedDateTime notBefore = ZonedDateTime.from(newCert.getNotBefore().toInstant().atZone(ZoneOffset.UTC));
		ZonedDateTime notAfter = ZonedDateTime.from(newCert.getNotAfter().toInstant().atZone(ZoneOffset.UTC));
		if (start.isBefore(notBefore) || start.isAfter(notAfter)) {
			log.warn("Requested activation date {} is outside of certificate validity period ({}-{})",
					 String.format("%tFT%<tT%<tz", activation), String.format("%tFT%<tT%<tz", notBefore), 
					 String.format("%tFT%<tT%<tz", notAfter));
			throw new CertificateException("Requested activation date is outside of certificate validity period");
		}
		
		log.trace("Update server configuration with new certificate");
		ServerConfigEntity config = getConfig();
		config.setActivationDate(activation);
		if (activation == null) 
			config.setCurrentKeyPair(encrypt(keypair));
		else
			config.setNextKeyPair(encrypt(keypair));
		configRepo.save(config);
				
		if (config.isRegisteredSML() && smlServiceImpl.requiresSMPCertRegistration()) {
			try {
				log.debug("Register new server certificate in SML");
				EmbeddedCertificate updateInfo = new EmbeddedCertificate();
				updateInfo.setX509Cert(newCert);
				updateInfo.setActivationDate(activation);
				smlServiceImpl.updateSMPCertificate(config.getSmpId(), updateInfo);
			} catch (SMLException smlUpdateFailed) {
				log.error("Failed to register new server certificate in SML : {}", 
							Utils.getExceptionTrace(smlUpdateFailed));
				throw smlUpdateFailed;
			}
		} 
		log.trace("Update audit log");
		auditSvc.log(new AuditLogRecord(Instant.now(), user.getUsername(), "Update certificate", "Server", 
					String.format("Subject=%s,Issuer=%s,SerialNo=%s,NotBefore=%tFT%<tT%<tz,NotAfter=%tFT%<tT%<tz", 
							CertificateUtils.getSubjectName(newCert), CertificateUtils.getIssuerName(newCert), 
							newCert.getSerialNumber().toString(), notBefore, notAfter)));			
	}

	@Override
	public void removeCertificate(UserDetails user) throws PersistenceException, SMLException {
		log.trace("Request to remove server certificate");
		ServerConfigEntity config = getConfig();
		
		if (config.getCurrentKeyPair() == null) {
			log.trace("Nothing to do, as no certificate has been configured");
			return;
		} else if (config.isRegisteredSML() && smlServiceImpl.requiresSMPCertRegistration()) {
			log.warn("Attempt to remove server certificate while registered in SML");
			throw new SMLException("Cannot remove certificate while registered in SML");
		} 		
		config.setCurrentKeyPair(null);
		config.setNextKeyPair(null);
		config.setActivationDate(null);
		configRepo.save(config);	
		
		log.trace("Update audit log");
		auditSvc.log(new AuditLogRecord(Instant.now(), user.getUsername(), "Remove certificate", "Server", null));
	}
	
	@Override
	@Transactional(rollbackFor = SMLException.class)
	public void updateServerMetadata(UserDetails user, SMPServerMetadata serverData) throws PersistenceException {
		log.trace("Request to update server metadata");
		ServerConfigEntity config = getConfig();
		if (config.isRegisteredSML() && !Utils.nullSafeEqual(config.getSmpId(), serverData.getSMPId())) {
			log.warn("Attempt to change SMP ID while registered in SML or Directory");
			throw new PersistenceException("SMP ID cannot be changed while registered in SML or Directory");
		} 
		
		config.setSmpId(serverData.getSMPId());
		config.setBaseUrl(serverData.getBaseUrl());
		config.setIpv4Address(serverData.getIPv4Address());
		config.setIpv6Address(serverData.getIPv6Address());
			
		log.trace("Save metadata");
		configRepo.save(config);
		if (config.isRegisteredSML()) 
			try {
				log.debug("Update SML registration with new server metadata");
				smlServiceImpl.updateSMPServer(convertToSMPServerMetadata(config));
			} catch (SMLException smlUpdateFailed) {
				log.error("Error while updating SML registration: {}", Utils.getExceptionTrace(smlUpdateFailed));
				throw smlUpdateFailed;
			} 		

		log.trace("Update audit log");
		auditSvc.log(new AuditLogRecord(Instant.now(), user.getUsername(), "Update metadata", "Server", 
										getAuditDetails(config)));
		log.info("Updated server metadata");
	}

	@Override
	public SMPServerMetadata getServerMetadata() {
		return convertToSMPServerMetadata(getConfig());
	}
	
	@Override
	public NetworkServicesData getNetworkServicesInfo() {
		return new NetworkServicesData(smlServiceImpl != null,
									   smlServiceImpl != null ? smlServiceImpl.getSMLName() : null,
									   smlServiceImpl != null ? smlServiceImpl.requiresSMPRegistration() : null,
									   smlServiceImpl != null ? smlServiceImpl.requiresSMPCertRegistration() : null,
									   dirServiceImpl != null,
									   dirServiceImpl != null ? dirServiceImpl.getDirectoryName() : null);										   
	}
	
	@Override
	public PrivateKeyEntry getActiveKeyPair() {
		ServerConfigEntity config = getConfig();
		return config.getCurrentKeyPair() != null ? decrypt(config.getCurrentKeyPair()) : null;		
	}
	
	@Override
	public SMLIntegrationService getSMLIntegrationService() {						
		return smlServiceImpl;
	}

	@Override
	@Transactional(rollbackFor = SMLException.class)
	public void registerServerInSML(UserDetails user) throws SMLException {
		if (smlServiceImpl == null) {
			log.warn("Request to register server in SML, but no SML integration available!");
			throw new SMLException("SML integration not available");
		}
		ServerConfigEntity config = getConfig();
		if (config.isRegisteredSML()) {
			log.debug("Request to register server in SML, but already registered in SML");
			return;
		}
		config.setRegisteredSML(true);
		configRepo.save(config);
		try {
			log.trace("Register server in SML");
			smlServiceImpl.registerSMPServer(convertToSMPServerMetadata(config));			
		} catch (SMLException smlRegistrationFailed) {
			log.error("An error occurred registering the server in the SML : {}", 
						Utils.getExceptionTrace(smlRegistrationFailed));
			throw smlRegistrationFailed;
		}
		log.trace("Update audit log");
		auditSvc.log(new AuditLogRecord(Instant.now(), user.getUsername(), "Register in SML", "Server", 
					getAuditDetails(config)));
		log.info("Registered server in SML");
	}

	@Override
	public boolean isRegisteredInSML() {
		log.trace("Check if server is registered in SML");
		return getConfig().isRegisteredSML();
	}
	
	@Override
	@Transactional(rollbackFor = SMLException.class)
	public void removeServerFromSML(UserDetails user) throws SMLException {
		ServerConfigEntity config = getConfig();
		if (!config.isRegisteredSML()) {
			log.debug("Server is already removed from SML");
			return;
		} else if (dirServiceImpl != null && dirServiceImpl.isSMLRegistrationRequired() 
				&& participants.existsByPublishedInDirectory(true)) {
			log.warn("Cannot remove server from SML as there still exist published participants");
			throw new SMLException("Cannot remove server from SML as there still exist published participants");
		}		
		if (smlServiceImpl == null) {
			log.error("Cannot remove server from SML, as SML integration not available any more!");
			throw new SMLException("SML integration not available");
		}		
		config.setRegisteredSML(false);
		configRepo.save(config);
		participants.unregisterAllFromSML();
		try {
			log.trace("Remove server from SML");
			smlServiceImpl.deregisterSMPServer(config.getSmpId());
		} catch (SMLException smlDeregistrationFailed) {
			log.error("An error occurred removing the server from the SML : {}", 
						Utils.getExceptionTrace(smlDeregistrationFailed));
			throw smlDeregistrationFailed;
		}
		log.trace("Update audit log");
		auditSvc.log(new AuditLogRecord(Instant.now(), user.getUsername(), "Remove from SML", "Server", null));
		log.info("Removed server from SML");
	}

	@Override
	public DirectoryIntegrationService getDirectoryIntegrationService() {
		return dirServiceImpl;
	}

	/**
	 * Retrieves the configuration meta-data from the database. If no configuration exists, an empty one is returned.
	 * If a certificate update is pending, it is applied when needed.
	 * 
	 * @return	the {@link ServerConfigEntity} object representing the server configuration.
	 */
	private ServerConfigEntity getConfig() {
		Iterator<ServerConfigEntity> configs = configRepo.findAll().iterator();		
		if (!configs.hasNext()) 
			return new ServerConfigEntity();			
		
		ServerConfigEntity config = configs.next();
		ZonedDateTime activationDate = config.getActivationDate();
		ZonedDateTime now = ZonedDateTime.now();
		if (activationDate != null && ((activationDate.isBefore(now) || activationDate.isEqual(now)))) {
			log.debug("Applying pending certificate update");
			config.setCurrentKeyPair(config.getNextKeyPair());
			config.setNextKeyPair(null);
			config.setActivationDate(null);
			configRepo.save(config);
		}
		return config;
	}
	
	/**
	 * Gets the audit details for the given server configuration.
	 * 
	 * @param config	the server configuration
	 * @return	the information to include in the audit record
	 */
	private String getAuditDetails(ServerConfigEntity config) {
		return String.format("SMPid=%s,baseUrl=%s,IPv4=%s,IPv6=%s", 
							config.getSmpId(), config.getBaseUrl(), config.getIpv4Address(), config.getIpv6Address());
	}
	
	/**
	 * Converts the configuration data contained in the entity object to a SMP server metadata object specified by the
	 * <i>interfaces</i> layer.
	 * 
	 * @param config	the {@link ServerConfigEntity} object representing the server configuration
	 * @return	the {@link SMPServerMetadata} object representing the server metadata
	 */
	private SMPServerMetadataImpl convertToSMPServerMetadata(ServerConfigEntity config) {
		PrivateKeyEntry currentKeyPair = decrypt(config.getCurrentKeyPair());
		PrivateKeyEntry nextKeyPair = decrypt(config.getNextKeyPair());		
		
		X509Certificate cert = currentKeyPair != null ? (X509Certificate) currentKeyPair.getCertificate() : null;
		EmbeddedCertificate certUpdate = null;
		if (nextKeyPair != null) {
			certUpdate = new EmbeddedCertificate();
			certUpdate.setX509Cert((X509Certificate) nextKeyPair.getCertificate());
			certUpdate.setActivationDate(config.getActivationDate());
		}		
		return new SMPServerMetadataImpl(config.getSmpId(), 
										 config.getBaseUrl(), 
										 config.getIpv4Address(), 
										 config.getIpv6Address(),
										 cert,
										 certUpdate
										 );
	}	
	
	/**
	 * Encrypts the given key pair for storage in the database.
	 * 
	 * @param keyPair	the key pair to encrypt
	 * @return byte array containing the encrypted key pair to be stored in the database
	 */
	private byte[] encrypt(PrivateKeyEntry keyPair) {
		if (keyPair == null)
			return null;
		
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			KeystoreUtils.saveKeyPairToPKCS12(keyPair, baos, null);
			return encryptor.encrypt(baos.toByteArray());
		} catch (Exception encryptionError) {
			log.error("Error while encrypting server key pair: {}", Utils.getExceptionTrace(encryptionError));
			throw new RuntimeException("Error while encrypting server key pair", encryptionError);
		}
	}
	
	/**
	 * Decrypts the given encrypted key pair.
	 * 
	 * @param encryptedKeyPair byte array containing the encrypted key pair as stored in the database
	 * @return	the decrypted key pair
	 */
	private PrivateKeyEntry decrypt(byte[] encryptedKeyPair) {
		if (encryptedKeyPair == null)
			return null;
		
		try {
			return KeystoreUtils.readKeyPairFromKeystore(
												new ByteArrayInputStream(encryptor.decrypt(encryptedKeyPair)), null);			
		} catch (Exception decryptionError) {
			log.error("Error while decrypting server key pair: {}", Utils.getExceptionTrace(decryptionError));
			throw new IllegalStateException("Invalid certificate data in configuration");
		}		
	}

	
}
