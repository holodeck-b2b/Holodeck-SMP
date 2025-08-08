/*
 * Copyright (C) 2025 The Holodeck B2B Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.bdxr.smp.server.datamodel;

import java.net.URL;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;

import org.holodeckb2b.bdxr.smp.server.services.core.ParticipantsService;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Defines the meta-data maintained by Holodeck SMP on a <i>Participant</i> in the network.
 * <p> 
 * For the operation of the SMP server the only required information elements on a <i>Participant</i> are the identifier 
 * under which the <i>Participant</i> is known in the network and which <i>Service Metadata Templates</i> are bound to
 * the <i>Participant</i>.
 * <p>
 * All other meta-data is for informational purposes only, either internally or for publication in the network's 
 * directory. When information on the Participant is published in the network's directory this data model at least 
 * requires the name of the Participant to be set, but network's directory may also require other elements to be 
 * provided. These additional checks are performed by the network's directory service and not by the SMP server itself.
 * This may cause publication to fail if the required elements are not provided.   
 * <p>
 * NOTE 1: Most fields have a corresponding <code>setter</code> or <code>add</code>/<code>remove</code> method to manage
 * the meta-data. The registration of the Participant in the SML and/or in the network's directory and the bound Service
 * Meta-data Templates however need to be managed using the methods of the {@link ParticipantsService}.</br>
 * NOTE 2: For the multi-value elements the storage implementation may limit the number of elements that can be stored.
 * For example the default implementation included in the project limits the number of contacts and website URLs to one.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface Participant extends MetadataRegistration<Identifier> {

	/**
	 * Gets the collection of <i>Service Metadata Templates</i> that are bound to the Participant. This collection is
	 * read-only and implementations may return an unmodifiable collection. To update the Service Metadata Template 
	 * bindings of the Participant use the {@link ParticipantsService#bindToSMT(Participant, ServiceMetadataTemplate)} 
	 * method.   
	 * 
	 * @return 	collection of bound <i>Service Metadata Templates</i>. If no templates are bound to the Participant an
	 * 			empty collection SHALL be returned 
	 * @see ParticipantsService#bindToSMT(UserDetails, Participant, ServiceMetadataTemplate)
	 * @see ParticipantsService#removeSMTBinding(UserDetails, Participant, ServiceMetadataTemplate)
	 */
	Collection<ServiceMetadataTemplate> getBoundSMT();
	
	/**
	 * Indicates whether the Participant is registered in the SML. 
	 * <p>
	 * NOTE: As this indicator only has value if the SMP server is integrated with the network's SML, this method should 
	 * only be called when the integration with SML is available and ready for registration of Participants, i.e. {@link 
	 * ParticipantsService#isSMLRegistrationAvailable()} returns <code>true</code>.    
	 * 
	 * @return 	<code>true</code> when the Participant is registered in the SML, <code>false</code> otherwise
	 * @see ParticipantsService#registerInSML(UserDetails, Participant)
	 * @see ParticipantsService#removeFromSML(UserDetails, Participant)
	 */
	boolean isRegisteredInSML();	
	
	/**
	 * Gets the migration code that is associated with the Participant and which allows that the Participant's 
	 * registration in the SML is moved to another SMP. 
	 * <p>
	 * NOTE: As the migration can only be performed when the Participant is registered in the SML, this method should 
	 * only be called when the Participant is already registered in the SML, i.e. {@link #isRegisteredInSML()} returns 
	 * <code>true</code>.    
	 * 
	 * @return the migration code, <code>null</code> if the Participant has not been prepared for migration
	 * @see ParticipantsService#prepareForSMLMigration(UserDetails, Participant) 
	 * @see ParticipantsService#cancelSMLMigration(UserDetails, Participant) 
	 */
	String getSMLMigrationCode();
	
	/**
	 * Indicates whether the Participant is published in the network's directory.    
	 * <p>
	 * NOTE: As this indicator only has value if the SMP server is integrated with the network's directory service, this 
	 * method should only be called when the integration with the directory is available and ready for publication of 
	 * Participants, i.e. {@link ParticipantsService#isDirectoryPublicationAvailable()} returns <code>true</code>.    
	 * 
	 * @return	<code>true</code> when the Participant is published in the directory, <code>false</code> otherwise
	 * @see ParticipantsService#publishInDirectory(UserDetails, Participant)
	 * @see ParticipantsService#removeFromDirectory(UserDetails, Participant)
	 */
	boolean isPublishedInDirectory();
	
	/**
	 * Gets the [business entity] name of the Participant. Required in case the Participant is published in the 
	 * network's directory. 
	 * 
	 * @return 	name of the Participant, 
	 * 			SHALL NOT be <code>null</code> or empty when <code>{@link #isPublishedInDirectory()} == true</code>   
	 */
	String getName();
	
	/**
	 * Sets the [business entity] name of the Participant. Required in case the Participant is published in the 
	 * network's directory. 
	 * 
	 * @param name 	the name of the Participant
	 * 
	 */
	void setName(String name);
	
	/**
	 * Gets the country where the Participant is registered.  
	 *  
	 * @return	the 2-letter ISO 3166-1 country code 
	 */	
	String getRegistrationCountry();
	
	/**
	 * Sets the country where the Participant is registered.  
	 *  
	 * @param countryCode	the 2-letter ISO 3166-1 country code
	 */
	void setRegistrationCountry(String countryCode);
	
	/**
	 * Gets information on the location where the Participant is registered or physically located (semantics may be 
	 * specified by the network).</br> 
	 * Although this often is an structured address, it is defined as an unstructured string to allow more flexibility.   
	 *  
	 * @return the location where the Participant is registered or located 
	 */
	String getLocationInfo();
	
	/**
	 * Sets information on the location where the Participant is registered or physically located (semantics may be 
	 * specified by the network).  
	 * 
	 * @param locationInfo the location where the Participant is registered or located
	 */
	void setLocationInfo(String locationInfo);
	
	/**
	 * Gets the date when the Participant was first registered in the network. 
	 * 
	 * @return the first registration date
	 */
	LocalDate getFirstRegistrationDate();

	/**
	 * Sets the date when the Participant was first registered in the network. 
	 * 
	 * @param firstRegistrationDate the first registration date, MUST NOT be in the future.
	 */
	void setFirstRegistrationDate(LocalDate firstRegistrationDate);
	
	/**
	 * Gets the contact information of the Participant.  
	 * 
	 * @return set of {@link Contact}s of the Participant
	 */
	Set<Contact> getContactInfo();
	
	/**
	 * Adds the contact information of the Participant.
	 * <p>
	 * NOTE: The number of contacts that can be stored may be limited by the storage implementation. The default 
	 * implementation limits the number of contacts to one.
	 * 
	 * @param contact the contact information of the Participant
	 */
	void addContactInfo(Contact contact);

	/**
	 * Removes the contact information of the Participant.
	 * 
	 * @param contact	a {@link Contact} instance from the {@link #getContactInfo()} collection
	 */
	void removeContactInfo(Contact contact);
	
	/**
	 * Gets the URL(s) of the website(s) of the Participant.
	 *  
	 * @return set of URLs for the website(s) used by the Participant
	 */
	Set<URL> getWebsites();
	
	/**
	 * Adds the URL of a website used by the Participant.
	 * <p>
	 * NOTE: The number of website URLs that can be stored may be limited by the storage implementation. The default 
	 * implementation limits the number of URLs to one.
	 * 
	 * @param url	the URL of the Participant's website	
	 */
	void addWebsite(URL url);
	
	/**
	 * Removes the URL of a website used by the Participant.
	 * 
	 * @param url	a URL instance from the {@link #getWebsites()} collection
	 */
	void removeWebsite(URL url);
	
	/**
	 * Gets the collection of additional identifiers of the Participant. These identifiers are only for information and,
	 * when published in the network's directory, to assist other parties to find the Participant. These cannot be used 
	 * for querying the SMP server. If that should be possible multiple registrations of the Participant must be created 
	 * for each identifier that can be used for querying.
	 * 
	 * @return the set of additional identifiers of the Participant
	 */
	Set<org.holodeckb2b.bdxr.smp.datamodel.Identifier> getAdditionalIds();
	
	/**
	 * Adds an additional identifier used by the Participant. The argument passed to this method only needs to 
	 * implement {@link org.holodeckb2b.bdxr.smp.datamodel.Identifier}, i.e. it may be a read-only object. This is 
	 * because the identifier is not yet managed by the SMP server. However if the Identifier has an identifier scheme, 
	 * the scheme SHOULD be already registered in the SMP server so the correct case-sensitivity is used for processing
	 * of the identifier.   
	 * <p>
	 * NOTE: The number of additional identifiers that can be stored may be limited by the storage implementation. In 
	 * the default implementation the number of additional identifiers is limited by the length of concatenation of 
	 * their string representations which can not exceed 1024 characters. 
	 * 
	 * @param id	the additional identifier to add to the Participant
	 */
	void addAdditionalId(org.holodeckb2b.bdxr.smp.datamodel.Identifier id);
	
	/**
	 * Removes the specified additional identifier from the Participant.
	 * 
	 * @param id	a {@link org.holodeckb2b.bdxr.smp.datamodel.Identifier} instance from the {@link 
	 * 				#getAdditionalIds()} collection
	 */
	void removeAdditionalId(org.holodeckb2b.bdxr.smp.datamodel.Identifier id);
}
