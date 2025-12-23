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

import java.util.Collection;

import org.holodeckb2b.bdxr.smp.server.datamodel.Endpoint;
import org.holodeckb2b.bdxr.smp.server.datamodel.Process;
import org.holodeckb2b.bdxr.smp.server.datamodel.Service;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Defines the interface of the central service for the management of <i>Service Metadata Template</i> registrations in 
 * the Holodeck SMP instance.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface SMTMgmtService {

	/**
	 * Adds a new Service Metadata Template to the data set managed by this Holodeck SMP instance and returns the 
	 * registered instance which must be used for further processing. This method will assign an identifier to the new 
	 * registration. Therefore the provided meta-data SHOULD NOT contain an identifier, but if one is provided it will 
	 * be ignored. 
	 *  
	 * @param user		the User registering the Service Metadata Template, required for audit logging
	 * @param smt 	the meta-data on the new Service Metadata Template
	 * @return	the registered Service Metadata Template, this object MUST be used for further processing of the 
	 * 			template
	 * @throws PersistenceException	when an error occurs saving the Service Metadata Template
	 */
	ServiceMetadataTemplate addTemplate(UserDetails user, ServiceMetadataTemplate smt) throws PersistenceException;
	
	/**
	 * Updates the meta-data of an existing Service Metadata Template.
	 * 
	 * @param user	the User updating the Service Metadata Template, required for audit logging
	 * @param smt	the updated meta-data of the template, MUST be an instance that was obtained from this service
	 * @return	the updated Service Metadata Template, this object MUST be used for further processing of the template
	 * @throws PersistenceException	when an error occurs saving the Service Metadata Template
	 */
	ServiceMetadataTemplate updateTemplate(UserDetails user, ServiceMetadataTemplate smt) throws PersistenceException;
	
	/**
	 * Removes the Service Metadata Template registration. Note that a Service Metadata Template can only be removed if
	 * it is not bound to any Participant.
	 * 
	 * @param user	the User removing the Service Metadata Template, required for audit logging
	 * @param smt	the Service Metadata Template to remove
	 * @throws PersistenceException	when an error occurs removing the Service Metadata Template, for example because it
	 * 								is still bound to a Participant
	 */
	void deleteTemplate(UserDetails user, ServiceMetadataTemplate smt) throws PersistenceException;
	
	/**
	 * Gets all registered Service Metadata Templates.
	 * 
	 * @return	collection with the meta-data of all registered Services Metadata Templates, empty collection if no 
	 * 			templates are registered
	 * @throws PersistenceException	when an error occurs retrieving the meta-data of the templates
	 */
	Collection<? extends ServiceMetadataTemplate> getTemplates() throws PersistenceException;
	
	/**
	 * Gets a subset of registered Service Metadata Templates.
	 * 
	 * @param request	a {@link PageRequest} specifying the requested subset
	 * @return	collection with the meta-data of the registered Services Metadata Templates, empty collection if the
	 * 			requested subset does not contain any templates
	 * @throws PersistenceException	when an error occurs retrieving the meta-data of the templates
	 */
	Page<? extends ServiceMetadataTemplate> getTemplates(PageRequest request) throws PersistenceException;
	
	/**
	 * Gets all registered Service Metadata Templates that apply to the given Service.
	 * 
	 * @param svc the Service registration to find the templates for
	 * @return	collection with the meta-data of the registered Service Metadata Templates that apply to the Service if 
	 * 			such registrations exist, empty collection if no templates are registered that apply to the specified 
	 * 			Service 
	 * @throws PersistenceException	when an error occurs retrieving the Service Metadata Templates
	 */
	Collection<? extends ServiceMetadataTemplate>  findTemplatesForService(Service svc) throws PersistenceException;
	
	/**
	 * Gets all registered Service Metadata Templates that apply to the given Process.
	 * 
	 * @param proc the Process registration to find the templates for
	 * @return	collection with the meta-data of the registered Service Metadata Templates that apply to the Process if 
	 * 			such registrations exist, empty collection if no templates are registered that apply to the specified 
	 * 			Process 
	 * @throws PersistenceException	when an error occurs retrieving the Service Metadata Templates
	 */
	Collection<? extends ServiceMetadataTemplate>  findTemplatesForProcess(Process svc) throws PersistenceException;

	/**
	 * Gets all registered Service Metadata Templates that use to the given Endpoint.
	 * 
	 * @param ep the Endpoint registration to find the templates for
	 * @return	collection with the meta-data of the registered Service Metadata Templates that include the given 
	 * 			Endpoint if such registrations exist, empty collection if no templates are registered that include the 
	 * 			specified Endpoint
	 * @throws PersistenceException	when an error occurs retrieving the Service Metadata Templates
	 */
	Collection<? extends ServiceMetadataTemplate>  findTemplatesUsingEndpoint(Endpoint ep) throws PersistenceException;
	
	/**
	 * Gets the registered Service Metadata Template with the given identifier.
	 * 
	 * @param id 	identifier of the Service Metadata Template to retrieve
	 * @return	the meta-data of the Service Metadata Template with the given identifier if it exists, 
	 * 			<code>null</code> otherwise
	 * @throws PersistenceException	when an error occurs retrieving the Service Metadata Template
	 */
	ServiceMetadataTemplate getTemplate(Long id) throws PersistenceException;
}
