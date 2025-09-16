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
package org.holodeckb2b.bdxr.smp.server.services.core;

import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.CertificateException;
import java.time.ZonedDateTime;
import java.util.concurrent.Future;

import org.holodeckb2b.bdxr.smp.server.datamodel.NetworkServicesData;
import org.holodeckb2b.bdxr.smp.server.datamodel.SMPServerMetadata;
import org.holodeckb2b.bdxr.smp.server.services.network.DirectoryIntegrationService;
import org.holodeckb2b.bdxr.smp.server.services.network.SMLException;
import org.holodeckb2b.bdxr.smp.server.services.network.SMLIntegrationService;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Defines the Holodeck SMP Spring Service that provides the API to manage the SMP server configuration. The API 
 * provides function to manage:<ul>
 * <li>the certificate used by SMP server to sign responses and authenticate itself when calling the network's SML and 
 * Directory services.</li>
 * <li>if needed, the registration of the SMP server with the network's SML service</li>
 * <li>if needed, the registration of the SMP server with the network's Directory service</li>
 * </ol>
 * Only one certificate can be in use at one point in time, but a change of certificate can be scheduled. This may be 
 * needed if the network's SML service requires the certificate change to be announced in advance. It is up to user or 
 * other components to take into account such advance notification. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface SMPServerAdminService {

	/**
	 * Registers a new key pair to be used for signing responses. The new key pair will directly be used for signing.
	 * When the SMP server is integrated with a SML service that requires registration of the SMP certificate the new
	 * certificate will also be registered with the SML. Please note that some SML services may require that the update
	 * is notified in advance. In such cases use the {@link #registerCertificate(PrivateKeyEntry, ZonedDateTime)} method
	 * to register the update. 
	 * 
	 * @param user		the User registering the new SMP certificate
	 * @param keypair	the new key pair to be used by the SMP for signing responses	
	 * @throws CertificateException when the key pair could not be registered, for example because the certificate is 
	 * 								currently not valid. It may also be caused by a problem in the registration of the 
	 * 								certificate with SML service.
	 */
	void registerCertificate(UserDetails user, PrivateKeyEntry keypair) throws CertificateException;

	/**
	 * Registers a new key pair that should be used from the specified activation date for signing responses. After
	 * activation of the new key pair the old key pair will automatically be removed.
	 * 
	 * @param user		the User registering the new SMP certificate
	 * @param keypair	 the new key pair to be used by the SMP for signing responses	
	 * @param activation the date and time when the key pair should be used
	 * @throws CertificateException when the key pair update could not be registered, for example because the 
	 * 								certificate is not valid at the specified activation time. Another cause can be a 
	 * 								problem in the registration of the certificate with SML service.
	 */	
	void registerCertificate(UserDetails user, PrivateKeyEntry keypair, ZonedDateTime activation) throws CertificateException;

	/**
	 * Removes the registered SMP certificate, including any pending update. When the SMP server is integrated with a 
	 * SML service that requires registration the SMP certificate, the certificate will also be removed from the SML.
	 * 
	 * @param user		the User removing the SMP certificate
	 * @throws CertificateException when the SMP certificate could not be removed. Could be caused by a problem in the 
	 * 								removal of the certificate with SML service.
	 */
	void removeCertificate(UserDetails user) throws CertificateException;
			
	/**
	 * Updates the SMP server's meta-data. When the SMP server is registered with the network's SML service the update
	 * will also be sent to the SML.
	 * 
	 * @param user			the User updating the SMP server's meta-data
	 * @param serverData	the new meta-data of the server.
	 * @throws SMLException when the update of the SMP server meta-data could not be registered in the SML. When this
	 * 						exception is thrown the update is not saved.
	 * @throws PersistenceException when the meta-data could not be updated
	 */
	void updateServerMetadata(UserDetails user, SMPServerMetadata serverData) throws SMLException, PersistenceException;
	
	/**
	 * @return the current meta-data of the SMP server.	
	 */
	SMPServerMetadata getServerMetadata();
	
	/**
	 * Gets the currently active key pair that should be used to sign responses and authenticate the SMP server. 
	 * 
	 * @return	the currently active key pair or <code>null</code> if no key pair has been configured
	 */
	PrivateKeyEntry getActiveKeyPair();

	/**
	 * Gets the meta-data about the status of the network services (SML and Directory) that the SMP server is integrated 
	 * with.  
	 * 
	 * @return	the meta-data about the network services 
	 */
	NetworkServicesData getNetworkServicesInfo();
	
	/**
	 * Gets the SML integration service that must be used to manage Participant registrations in the network's SML 
	 * service. Only available if the SMP server is connected to a network's SML service, i.e. <code>{@link 
	 * #getNetworkServicesInfo()}.smlServiceAvailable == true</code> 
	 *  
	 * @return the SML integration service to manage Participant registrations in the network's SML service
	 */
	SMLIntegrationService getSMLIntegrationService();
	
	/**
	 * Gets the Directory integration service that must be used to manage Participant publication to the network's 
	 * Directory service. Only available if the SMP server is connected to a network's Directory service, i.e. <code>
	 * {@link #getNetworkServicesInfo()}.directoryServiceAvailable == true</code> 
	 *  
	 * @return the SML integration service to manage Participant registrations in the network's SML service
	 */
	DirectoryIntegrationService getDirectoryIntegrationService();
		
	/**
	 * Registers the SMP server with the network SML. Note that this operation only needs to be executed if the 
	 * network's SML requires SMP server to register themselves. This method only needs to be called once as updates to
	 * the SMP server meta-data will automatically be sent to the SML after registration. Repeated calls when already
	 * registered will be ignored. 
	 * 
	 * @param user	the User that executed the SML registration of the SMP
	 * @throws SMLException	when the SMP server cannot be registered in the SML. This can be caused by a network error,
	 * 						but also by a duplicate SMP-id 
	 * @see SMLIntegrationService#requiresSMPRegistration()
	 */
	void registerServerInSML(UserDetails user) throws SMLException;
	
	/**
	 * Indicates whether the SMP server is registered with the network's SML service.
	 * 
	 * @return	<code>true</code> when the SMP is registered in the SML, <code>false</code> otherwise
	 */
	boolean isRegisteredInSML();
	
	/**
	 * Removes the SMP server from the network SML. 
	 * <p>
	 * NOTE: If the server is also connected to the network's directory and publication of Participants requires them
	 * to be also registered in the SML, removing the SML is only allowed when no Participant is still published. 
	 * 
	 * @param user	the User that executed the removal of the SMP from the SML 
	 * @return	a {@link Future} that will return <code>null</code> if the SMP was successfully removed from the SML, or
	 * 			a {@link SMLException} if an error occurred removing the SMP from the SML
	 * @throws SMLException if the SMP server cannot be removed from the SML, for example because there still exist
	 * 						Participants that are published in the directory and publication of Participants requires
	 * 						them to be registered in the SML   
	 */
	void removeServerFromSML(UserDetails user) throws SMLException;
}
