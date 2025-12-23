package org.holodeckb2b.bdxr.smp.server.services;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.holodeckb2b.bdxr.smp.server.auditlogging.AuditLogRecord;
import org.holodeckb2b.bdxr.smp.server.datamodel.Contact;
import org.holodeckb2b.bdxr.smp.server.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.datamodel.NetworkServicesData;
import org.holodeckb2b.bdxr.smp.server.datamodel.Participant;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataTemplate;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedContact;
import org.holodeckb2b.bdxr.smp.server.db.entities.ParticipantEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataTemplateEntity;
import org.holodeckb2b.bdxr.smp.server.db.repos.ParticipantRepository;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException.ViolationType;
import org.holodeckb2b.bdxr.smp.server.services.core.ParticipantsService;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.bdxr.smp.server.services.core.SMPServerAdminService;
import org.holodeckb2b.bdxr.smp.server.services.network.DirectoryException;
import org.holodeckb2b.bdxr.smp.server.services.network.DirectoryIntegrationService;
import org.holodeckb2b.bdxr.smp.server.services.network.SMLException;
import org.holodeckb2b.bdxr.smp.server.services.network.SMLIntegrationService;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * Implements the {@link ParticipantsService}.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Slf4j
@Service
public class ParticipantsServiceImpl 
						extends IdBasedEntityMgmtServiceImpl<Identifier, ParticipantEntity, ParticipantRepository> 
						implements ParticipantsService {

	@Autowired
	protected SMPServerAdminService	smpConfigService;
	
	private SMLIntegrationService 			smlService;
	private DirectoryIntegrationService 	directoryService;
		
	@Override
	protected String getAuditDetails(ParticipantEntity entity) {
		StringBuilder sb = new StringBuilder("Name=").append(entity.getName())
				.append(",Country=").append(entity.getRegistrationCountry())
				.append(",FirstRegistration=");
		if (entity.getFirstRegistrationDate() != null)
			sb.append(String.format("%tFT", entity.getFirstRegistrationDate()));
		sb.append(",Location=").append(entity.getLocationInfo())
		  .append(",Website=");
		if (!Utils.isNullOrEmpty(entity.getWebsites())) 
			sb.append(entity.getWebsites().iterator().next().toString());
		sb.append(",ContactInfo={");
		if (!Utils.isNullOrEmpty(entity.getContactInfo())) {
			Contact contact = entity.getContactInfo().iterator().next();
			sb.append("name=").append(contact.getName())
			  .append(",jobTitle=").append(contact.getJobTitle())
			  .append(",department=").append(contact.getDepartment())
			  .append(",email=").append(contact.getEmailAddress())
			  .append(",phone=").append(contact.getTelephone());
		}
		sb.append("},AdditionalIds=[");
		if (!Utils.isNullOrEmpty(entity.getAdditionalIds())) {
			entity.getAdditionalIds().forEach(id -> sb.append(id.toString()).append(','));
		}
		sb.append(']');
		
		return sb.toString();
	}
	
	@Override
	public Participant addParticipant(UserDetails user, Participant p) throws PersistenceException {
		validateRegistrationData(p);
		try {
			idUtils.toEmbeddedIdentifier(p.getId());
		} catch (NoSuchElementException unknownScheme) {
			log.warn("Participant ID contains an unmanaged IDScheme (schemeID={})", p.getId().getValue(),
						p.getId().getScheme().getSchemeId());
			throw new ConstraintViolationException(ViolationType.INVALID_FIELD, p, "Identifier.IDScheme");
		}
		
		ParticipantEntity entity = new ParticipantEntity();
		entity.setId(idUtils.toEmbeddedIdentifier(p.getId()));
		entity.setName(p.getName());
		entity.setRegistrationCountry(p.getRegistrationCountry());		
		entity.setFirstRegistrationDate(p.getFirstRegistrationDate());
		entity.setLocationInfo(p.getLocationInfo());
		
		if (!Utils.isNullOrEmpty(p.getWebsites())) {
			entity.addWebsite(p.getWebsites().iterator().next());
			if (p.getWebsites().size() > 1)
				log.warn("Provided Participant (ID={}) contains more than one website", p.getId().toString());
		}
		if (!Utils.isNullOrEmpty(p.getContactInfo())) {
			Contact contact = p.getContactInfo().iterator().next();
			entity.addContactInfo(new EmbeddedContact(contact.getName(),
													  contact.getJobTitle(),
													  contact.getDepartment(),
													  contact.getEmailAddress(),
													  contact.getTelephone()));
			if (p.getContactInfo().size() > 1)
				log.warn("Provided Participant (ID={}) contains more than one contact", p.getId().toString());			
		}				
		if (p.getAdditionalIds() != null) 
			p.getAdditionalIds().forEach(id -> entity.addAdditionalId(id));
		
		return executeCRUD(CrudOps.Add, user, entity);
	}

	@Override
	@Transactional
	public Participant updateParticipant(UserDetails user, Participant p) throws PersistenceException {
		ParticipantEntity entity = checkManaged(p);		
		ParticipantEntity current = repo.getReferenceById(entity.getOid());
		
		if (current.isRegisteredInSML() && !current.getId().equals(entity.getId())) {
			log.warn("Attempt to change ParticipantID (Current={},New={}) while registered in SML", 
					current.getId().toString(),	entity.getId().toString());
			throw new PersistenceException("Participant ID cannot be changed while registered in SML");
		}		
		validateRegistrationData(p);
		// Ensure that registration states are not changed when updating meta-data
		entity.setRegisteredInSML(current.isRegisteredInSML());
		entity.setSMLMigrationCode(current.getSMLMigrationCode());
		entity.setPublishedInDirectory(current.isPublishedInDirectory());		
		if (!Utils.areEqual(current.getBoundSMT(), entity.getBoundSMT())) {
			log.warn("Attempt to change bound SMT of Participant (ID={}) in update", entity.getId().toString());
			throw new PersistenceException("Bound SMT cannot be changed");
		}		
		ParticipantEntity updated = executeCRUD(CrudOps.Update, user, entity);
		
		if (updated.isPublishedInDirectory()) {
			log.trace("Notify directory about update of Participant meta-data");
			getDirectoryService().publishParticipantInfo(updated);
		}
		return updated;
	}
	
	/**
	 * Validates that the registration country and first date of registration included in the given Participant 
	 * meta-data conform to the specified constraints, i.e. the country code is two characters long and the registration
	 * date is not in the future. 
	 *  
	 * @param p	the Participant meta-data to validate
	 * @throws ConstraintViolationException when either of the fields fails validation
	 */
	private void validateRegistrationData(Participant p) throws ConstraintViolationException {
		String country = p.getRegistrationCountry();
		if (!Utils.isNullOrEmpty(country) && country.length() != 2) {
			log.warn("Participant (ID={}) contains an invalid country code : {}", p.getId().toString(), country);
			throw new ConstraintViolationException(ViolationType.INVALID_FIELD, p, "RegistrationCountry");
		}
		LocalDate registrationDate = p.getFirstRegistrationDate();
		if (registrationDate != null && registrationDate.isAfter(LocalDate.now())) {
			log.warn("Participant (ID={}) contains an future registration date : {}", p.getId().toString(), registrationDate);
			throw new ConstraintViolationException(ViolationType.INVALID_FIELD, p, "FirstRegistrationDate");
		}
	}

	@Override
	public void deleteParticipant(UserDetails user, Participant p) throws PersistenceException {
		ParticipantEntity entity = checkManaged(p);
		
		boolean isPublished = entity.isPublishedInDirectory();
		try {
			if (isPublished) {
				log.trace("Remove Participant (ID={}) from directory", entity.getId().toString());
				getDirectoryService().removeParticipantInfo(entity);
			}
			if (entity.isRegisteredInSML() && Utils.isNullOrEmpty(entity.getSMLMigrationCode())) {
				log.trace("Remove Participant (ID={}) from SML", entity.getId().toString());
				getSMLService().deregisterParticipant(entity);
			} else if (entity.isRegisteredInSML()) {
				log.debug("Not removing Participant (ID={}) from SML as it is migrated", entity.getId().toString());
			}
			executeCRUD(CrudOps.Delete, user, entity);
		} catch (DirectoryException directoryRemovalFailed) {
			log.error("Could not remove Participant (ID={}) from directory : {}", entity.getId().toString(),
						Utils.getExceptionTrace(directoryRemovalFailed));
			throw directoryRemovalFailed;
		} catch (SMLException smlRemovalFailed) {
			log.error("Could not remove Participant (ID={}) from SML : {}", entity.getId().toString(),
					Utils.getExceptionTrace(smlRemovalFailed));
			if (isPublished) {
				log.debug("Try to republish Participant in directory");
				try {
					getDirectoryService().publishParticipantInfo(entity);
					log.debug("Republished Participant in directory");
				} catch (DirectoryException directoryRepublishFailed) {
					log.error("Could not republish Participant to directory : {}", 
								Utils.getExceptionTrace(directoryRepublishFailed));
					entity.setPublishedInDirectory(false);
					repo.save(entity);
				}
			}			
			throw new PersistenceException("Error removing Participant from SML, but could not republish to directory",
											smlRemovalFailed);
		}		
	}

	@Override
	public Page<? extends Participant> getParticipants(PageRequest request) throws PersistenceException {
		return retrieveSubset(request);
	}

	@Override
	public Participant getParticipant(org.holodeckb2b.bdxr.common.datamodel.Identifier pid) throws PersistenceException {
		try {
			return getById(idUtils.toEmbeddedIdentifier(pid));
		} catch (NoSuchElementException unknownScheme) {			
			return null;
		}
	}

	@Override
	public Collection<? extends Participant> findParticipantsByName(String startsWith) throws PersistenceException {
		log.trace("Retrieving participants with name starting with {}", startsWith);
		return repo.findByLcNameStartsWith(startsWith.toLowerCase());
	}

	@Override
	public Collection<? extends Participant> findParticipantsByAdditionalId(org.holodeckb2b.bdxr.common.datamodel.Identifier id)
																						throws PersistenceException {
		log.trace("Retrieving participants with additional Id {}", id.toString());
		return repo.findByAdditionalId(id);
	}	
	
	@Override
	public Page<? extends Participant> findParticipantsBySMLRegistration(boolean registered, Pageable request)
			throws PersistenceException {
		log.trace("Retrieving page {} with {} participants {}registered in SML", request.getPageNumber(),
					request.getPageSize(), registered ? "" : "not ");
		return repo.findByRegisteredInSML(registered, request);
	}

	@Override
	public Page<? extends Participant> findParticipantsByDirectoryPublication(boolean published, Pageable request)
			throws PersistenceException {
		log.trace("Retrieving page {} with {} participants {}published in directory", request.getPageNumber(),
					request.getPageSize(), published ? "" : "not ");
		return repo.findByPublishedInDirectory(published, request);
	}
	
	@Override
	public Page<? extends Participant> findParticipantsBySMLRegistrationAndDirectoryPublication(boolean registered,
			boolean published, Pageable request) throws PersistenceException {
		log.trace("Retrieving page {} with {} participants {}registered in SML and {}published in directory", 
					request.getPageNumber(), request.getPageSize(), registered ? "" : "not ", published ? "" : "not ");
		return repo.findByRegisteredInSMLAndPublishedInDirectory(registered, published, request);
	}
	
	public long countParticipantsSupporting(ServiceMetadataTemplate smt) throws PersistenceException {
		log.trace("Counting number of participants supporting SMT (id={}, name={})", smt.getId(), smt.getName()); 
		return repo.countParticipantsSupporting(smt.getId());
	}	

	@Override
	public Page<? extends Participant> findParticipantsSupporting(ServiceMetadataTemplate smt, Pageable request)
			throws PersistenceException {
		if (!(smt instanceof ServiceMetadataTemplateEntity) || ((ServiceMetadataTemplateEntity) smt).getOid() == null) {
			log.warn("Provided SMT for binding to Participant (ID={}) is not managed");
			throw new IllegalArgumentException("Service Metadata Template instance is not managed");
		}
		log.trace("Retrieving page {} with {} participants supporting SMT (id={},name={})", request.getPageNumber(),
					request.getPageSize(), smt.getId(), smt.getName());		
		return repo.findByBindingsContains((ServiceMetadataTemplateEntity) smt, request);
	}

	@Override
	@Transactional
	public Participant bindToSMT(UserDetails user, Participant p, ServiceMetadataTemplate smt)
			throws PersistenceException {
		ParticipantEntity entity = reloadEntity(p);
		if (!(smt instanceof ServiceMetadataTemplateEntity) || ((ServiceMetadataTemplateEntity) smt).getOid() == null) {
			log.warn("Provided SMT for binding to Participant (ID={}) is not managed");
			throw new IllegalArgumentException("Service Metadata Template instance is not managed");
		}
		try {
			log.trace("Binding SMT (OID={}) to Participant (ID={}) ", ((ServiceMetadataTemplateEntity) smt).getId(), 
						entity.getId().toString());
			entity.addBinding((ServiceMetadataTemplateEntity) smt);		
			repo.save(entity);
			auditSMTAction(user, "Add Service to Participant", entity, smt);
			log.info("Bound SMT (OID={},name={},svcID={}) to Participant (ID={})", smt.getId().toString(),
						smt.getName(), smt.getService().getId().toString(), entity.getId().toString());
			return entity;			
		} catch (Throwable bindingFailed) {
			log.error("Failed to bind SMT (OID={},name={},svcID={}) to Participant (ID={}) : {}", smt.getId().toString(),	
						smt.getName(), smt.getService().getId().toString(), entity.getId().toString(), 
						Utils.getExceptionTrace(bindingFailed));
			throw bindingFailed;
		} 		
	}

	@Override
	@Transactional
	public Participant removeSMTBinding(UserDetails user, Participant p, ServiceMetadataTemplate smt)
			throws PersistenceException {
		ParticipantEntity entity = reloadEntity(p);
		try {
			log.trace("Removing binding of SMT (OID={}) to Participant (ID={}) ", ((ServiceMetadataTemplateEntity) smt).getId(), 
						entity.getId().toString());
			entity.removeBinding((ServiceMetadataTemplateEntity) smt);		
			repo.save(entity);
			auditSMTAction(user, "Remove Service from Participant", entity, smt);
			log.info("Removed binding of SMT (OID={},name={},svcID={}) to Participant (ID={})", smt.getId().toString(),
					smt.getName(), smt.getService().getId().toString(), entity.getId().toString());
			return entity;			
		} catch (ClassCastException notSMTEntity) {
			log.warn("Provided SMT to be removed from Participant (ID={}) is not managed", p.getId().toString());
			throw new IllegalArgumentException("Service Metadata Template instance is not managed");
		} catch (Throwable removeBindingFailed) {
			log.error("Failed to remove binding SMT (OID={},name={},svcID={}) to Participant (ID={}) : {}", 
						smt.getId().toString(), smt.getName(), smt.getService().getId().toString(), 
						entity.getId().toString(), Utils.getExceptionTrace(removeBindingFailed));
			throw removeBindingFailed;
		}
	}

	/**
	 * Helper method to create an audit log entry for (un)binding of a SMT.
	 * 
	 * @param user		the User executing the action
	 * @param action	description of the action performed, i.e. binding or unbinding the SMT
	 * @param p			the Participant registration the action is performed on
	 * @param smt		the Service Metadata Template that is bound to/removed from the Participant registration 
	 */
	private void auditSMTAction(UserDetails user, String action, ParticipantEntity p, ServiceMetadataTemplate smt) {
		auditSvc.log(new AuditLogRecord(Instant.now(), user.getUsername(), action, 
								p.getAuditLogId(), "SMT: id=" + smt.getId().toString() + ",name=" + smt.getName() 
											+ ",svcID=" + smt.getService().getId().toString()));		
	}
	
	@Override
	public boolean isSMLRegistrationAvailable() {
		NetworkServicesData networkServicesInfo = smpConfigService.getNetworkServicesInfo();
		return networkServicesInfo.smlServiceAvailable() && 
				(!networkServicesInfo.smlRegistrationRequired() || smpConfigService.isRegisteredInSML()); 
	}

	@Override
	@Transactional(rollbackFor = SMLException.class)
	public Participant registerInSML(UserDetails user, Participant p) throws PersistenceException {
		if (!isSMLRegistrationAvailable()) {
			// This should not happen as registrations should only be attempted when the SML service is available and if
			// needed, the SMP registered
			log.warn("Attempt to register Participant (ID={}) in SML, but SML is not available", p.getId().toString());
			throw new SMLException("SML registration not available");
		}
		ParticipantEntity entity = reloadEntity(p);
		try {
			log.trace("Update Participant meta-data");
			entity.setRegisteredInSML(true);
			repo.save(entity);
			log.trace("Registering Participant (ID={}) in SML", entity.getId().toString());
			getSMLService().registerParticipant(entity);
			auditSvc.log(new AuditLogRecord(Instant.now(), user.getUsername(), "Register in SML", 
						entity.getAuditLogId(), null));
			log.info("Registered Participant (ID={}) in SML", entity.getId().toString());
			return entity;			
		} catch (SMLException smlRegFailed) {
			log.error("SML Registration of Participant (ID={}) failed : {}", entity.getId().toString(), 
					Utils.getExceptionTrace(smlRegFailed));
			throw smlRegFailed;
		} 
	}

	@Override
	@Transactional(rollbackFor = SMLException.class)
	public Participant migrateInSML(UserDetails user, Participant p, String migrationCode) throws PersistenceException {
		if (!isSMLRegistrationAvailable()) {
			// This should not happen as migrations should only be attempted when the SML service is available and if
			// needed, the SMP registered
			log.warn("Attempt to migrate Participant (ID={}) in SML, but SML is not available", p.getId().toString());
			throw new SMLException("SML registration not available");
		}
		if (Utils.isNullOrEmpty(migrationCode)) 
			throw new IllegalArgumentException("Migration code cannot be empty");
		
		ParticipantEntity entity = reloadEntity(p);
		try {
			log.trace("Update Participant meta-data");
			entity.setRegisteredInSML(true);
			repo.save(entity);
			log.trace("Migrating Participant (ID={}) in SML", entity.getId().toString());
			getSMLService().migrateParticipant(entity, migrationCode);
			auditSvc.log(new AuditLogRecord(Instant.now(), user.getUsername(), "Migrate in SML", 
						entity.getAuditLogId(), "Used migration code : " + migrationCode));
			log.info("Migrated Participant (ID={}) in SML", entity.getId().toString());
			return entity;			
		} catch (SMLException smlRegFailed) {
			log.error("SML migration of Participant (ID={}) failed : {}", entity.getId().toString(), 
					Utils.getExceptionTrace(smlRegFailed));
			throw smlRegFailed;
		} 				
	}

	@Override
	@Transactional(rollbackFor = SMLException.class)
	public Participant prepareForSMLMigration(UserDetails user, Participant p, String... pMigrationCode) 
																						throws PersistenceException {
		ParticipantEntity entity = reloadEntity(p);
		if (!entity.isRegisteredInSML()) {
			log.warn("Attempt to prepare SML migration of unregistered Participant (ID={})", p.getId().toString());
			throw new SMLException("Participant is not registered in SML");
		}		
		log.trace("Preparing Participant (ID={}) for SML migration", p.getId().toString());
		String migrationCode;
		if (pMigrationCode == null || pMigrationCode.length == 0 || Utils.isNullOrEmpty(pMigrationCode[0])) {
			migrationCode = generateMigrationCode();
			log.trace("Generated migration code : {}", migrationCode);
		} else { 
			migrationCode = pMigrationCode[0];
			log.trace("Using provided migration code : {}", migrationCode);
		} 
	
		try {
			log.trace("Save migration code to Participant meta-data");
			entity.setSMLMigrationCode(migrationCode);
			repo.save(entity);
			log.trace("Prepare migration of Participant (ID={}) in SML", entity.getId().toString());
			getSMLService().registerMigrationCode(entity, migrationCode);
			auditSvc.log(new AuditLogRecord(Instant.now(), user.getUsername(), "Prepare SML migration", 
						entity.getAuditLogId(), "Used migration code : " + migrationCode));
			return entity;			
		} catch (SMLException smlRegFailed) {
			log.error("SML migration of Participant (ID={}) failed : {}", entity.getId().toString(), 
					Utils.getExceptionTrace(smlRegFailed));
			throw smlRegFailed;
		}
	}

	private static final String UCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String LCASE = "abcdefghijklmnopqrstuvwxyz";
	private static final String DIGITS = "0123456789";
	private static final String SPECIAL = "@#$%()[]{}*^-!~|+=";
	private static final String ALLCHARS = UCASE + LCASE + DIGITS + SPECIAL;
	private static final Random RANDOM = new Random();
	private static final Function<String, Character> getRandomChar = s -> s.charAt(RANDOM.nextInt(s.length()));
										
	/**
	 * Generates a 24 character long string containing at least 2 uppercase letters, 2 lowercase letters, 2 digits and 
	 * 2 special characters and which can be used a SML migration code.
	 * 
	 * @return	a 24 character long random string
	 */
	public static String generateMigrationCode() {		
		List<Character> characters = new ArrayList<>();
        characters.add(getRandomChar.apply(UCASE)); characters.add(getRandomChar.apply(UCASE));
        characters.add(getRandomChar.apply(LCASE)); characters.add(getRandomChar.apply(LCASE));
        characters.add(getRandomChar.apply(DIGITS)); characters.add(getRandomChar.apply(DIGITS));
        characters.add(getRandomChar.apply(SPECIAL)); characters.add(getRandomChar.apply(SPECIAL));        
        // Add additional random characters (optional)
        for (int i = 0; i < 16; i++) 
            characters.add(getRandomChar.apply(ALLCHARS));
        // Shuffle the characters to mix them
        Collections.shuffle(characters);
		return characters.stream().map(Object::toString).collect(Collectors.joining());
	}
	
	@Override
	@Transactional(rollbackFor = SMLException.class)
	public Participant cancelSMLMigration(UserDetails user, Participant p) throws PersistenceException {
		ParticipantEntity entity = reloadEntity(p);
		if (Utils.isNullOrEmpty(entity.getSMLMigrationCode())) {
			log.debug("Attempt to cancel SML migration of Participant (ID={}) not prepared for migration", 
						p.getId().toString());
			return entity;
		}		
		log.trace("Cancelling SML migration of Participant (ID={})", p.getId().toString());	
		try {
			log.trace("Clear migration code in Participant meta-data");
			String migrationCode = entity.getSMLMigrationCode();
			entity.setSMLMigrationCode(null);
			repo.save(entity);
			log.trace("Cancel migration of Participant (ID={}) in SML", entity.getId().toString());
			// As the SML API does not support cancelling migrations, cancelling the migration is done by migrating the
			// participant to self
			getSMLService().migrateParticipant(entity, migrationCode);
			auditSvc.log(new AuditLogRecord(Instant.now(), user.getUsername(), "Cancel SML migration", 
						entity.getAuditLogId(), null));
			
			log.info("Cancelled migration of Participant (ID={}) in SML", entity.getId().toString());
			return entity;			
		} catch (SMLException smlRegFailed) {
			log.error("Cancellation of SML migration of Participant (ID={}) failed : {}", entity.getId().toString(), 
						Utils.getExceptionTrace(smlRegFailed));
			throw smlRegFailed;
		}		
	}

	@Override
	@Transactional(rollbackFor = SMLException.class)
	public Participant removeFromSML(UserDetails user, Participant p) throws PersistenceException {
		ParticipantEntity entity = reloadEntity(p);		
		if (!entity.isRegisteredInSML()) {
			log.debug("Ignoring request to remove Participant (ID={}) from SML, as it is not registered", 
						entity.getId().toString());
			return entity;
		} 	
		if (entity.isPublishedInDirectory() 
									&& getDirectoryService().isSMLRegistrationRequired()) {
			log.warn("Cannot remove Participant (ID={}) from SML as it's published in directory", 
					entity.getId().toString());
			throw new SMLException("Participant is still published in directory");
		}					
		try {
			log.trace("Update Participant meta-data");
			entity.setRegisteredInSML(false);
			repo.save(entity);
			log.trace("Removing Participant (ID={}) from SML", p.getId().toString());
			getSMLService().deregisterParticipant(entity);
			auditSvc.log(new AuditLogRecord(Instant.now(), user.getUsername(), "Remove from SML", 
					entity.getAuditLogId(), null));
			log.info("Removed Participant (ID={}) from SML", entity.getId().toString());
			return entity;			
		} catch (SMLException smlRegFailed) {
			log.error("Removal of Participant (ID={}) from SML failed : {}", entity.getId().toString(), 
					Utils.getExceptionTrace(smlRegFailed));
			throw smlRegFailed;
		}
	}

	@Override
	public boolean isDirectoryPublicationAvailable() {
		DirectoryIntegrationService dirService = getDirectoryService();
		return dirService != null && dirService.isSMLRegistrationRequired() ? isSMLRegistrationAvailable() 
																			: dirService != null;		
	}

	@Override
	@Transactional(rollbackFor = DirectoryException.class)
	public Participant publishInDirectory(UserDetails user, Participant p) throws PersistenceException {
		if (!isDirectoryPublicationAvailable()) {
			// This should not happen as publication should only be attempted when the directory service is available 
			// and if needed, the SMP registered
			log.warn("Attempt to publish Participant (ID={}) in directory, but directory is not available", 
						p.getId().toString());
			throw new DirectoryException("Directory publication not available");
		}
		ParticipantEntity entity = reloadEntity(p);
		try {
			log.trace("Update Participant meta-data");
			entity.setPublishedInDirectory(true);
			repo.save(entity);
			log.trace("Publishing Participant (ID={}) to directory", entity.getId().toString());
			getDirectoryService().publishParticipantInfo(entity);
			auditSvc.log(new AuditLogRecord(Instant.now(), user.getUsername(), "Publish in directory", 
						entity.getAuditLogId(), null));
			log.info("Published Participant (ID={}) in directory", entity.getId().toString());
			return entity;			
		} catch (DirectoryException publicationFailed) {
			log.error("Directory publication of Participant (ID={}) failed : {}", entity.getId().toString(), 
					Utils.getExceptionTrace(publicationFailed));
			throw publicationFailed;
		} 	
	}

	@Override
	@Transactional(rollbackFor = SMLException.class)	
	public Participant removeFromDirectory(UserDetails user, Participant p) throws PersistenceException {
		ParticipantEntity entity = reloadEntity(p);
		if (!entity.isPublishedInDirectory()) {
			log.debug("Ignoring request to remove Participant (ID={}) from directory, as it is not published", 
						entity.getId().toString());
			return entity;
		} 
		try {
			log.trace("Update Participant meta-data");
			entity.setPublishedInDirectory(false);
			repo.save(entity);
			log.trace("Removing Participant (ID={}) from directory", entity.getId().toString());
			getDirectoryService().removeParticipantInfo(entity);
			auditSvc.log(new AuditLogRecord(Instant.now(), user.getUsername(), "Remove from directory", 
						entity.getAuditLogId(), null));
			log.info("Removed Participant (ID={}) from directory", entity.getId().toString());
			return entity;			
		} catch (DirectoryException removalFailed) {
			log.error("Directory removal of Participant (ID={}) failed : {}", entity.getId().toString(), 
					Utils.getExceptionTrace(removalFailed));
			throw removalFailed;
		} 	
	}
	
	private SMLIntegrationService getSMLService() {
		return smlService == null ? smlService = smpConfigService.getSMLIntegrationService() : smlService;
	}
	
	private DirectoryIntegrationService getDirectoryService() {
		return directoryService == null ? directoryService = smpConfigService.getDirectoryIntegrationService() 
										: directoryService;
	}

	/**
	 * Checks if the given Participant is managed by the server and reloads the meta-data from the database. Using this 
	 * method prevents that changes made to the entity object are accidently saved to the database.
	 * 
	 * @param p	the Participant instance to check and reload
	 * @return	the reloaded Participant instance
	 * @throws PersistenceException
	 */
	private ParticipantEntity reloadEntity(Participant p) throws PersistenceException {
		return repo.findById(checkManaged(p).getOid())
				.orElseThrow(() -> 
					new PersistenceException("Participant (ID=" + p.getId().toString() + ") does not exists anymore"));
	}
}
