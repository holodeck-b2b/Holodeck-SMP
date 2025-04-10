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
package org.holodeckb2b.bdxr.smp.server.svc;

import org.holodeckb2b.bdxr.smp.server.datamodel.Participant;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;

/**
 * Is a proxy to the {@link IDirectoryIntegrator} implementation so different implementations can be used depending on the 
 * network in which the SMP Server operates.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Service
@Slf4j
public class DirectoryIntegrationService {

	@Value("${directory.enabled:false}")
	protected boolean	directoryEnabled;
	@Value("${directory.implementatation:PEPPOLDirectoryClient}")
	protected String	implSvcName;
	@Autowired
	protected BeanFactory serviceFactory;
	@Autowired
	protected SMLIntegrationService smlService;
	
	private IDirectoryIntegrator directoryService;

	/**
	 * @return <code>true</code> when directory integration is enabled and the SMP is registered in the SML,
	 *		   <code>false</code> otherwise
	 */
	public boolean isDirectoryIntegrationAvailable() {
		return directoryEnabled && smlService.isSMLIntegrationAvailable();
	}

	/**
	 * Publishes the particpant's business card information to the directory. 
	 *
	 * @param p	the meta-data of the Participant whose business card needs to be published
	 * @throws DirectoryException when the Participant's business card could not be published in the directory
	 */
	public void publishParticipantInfo(Participant p) throws DirectoryException {
		try {
			integrator().publishParticipantInfo(p);
		} catch (DirectoryException e) {
			log.error("Error publishing participant info (PID={}) : {}", p.getId().toString(), Utils.getExceptionTrace(e));
			throw e;
		}

	}

	/**
	 * Remove the Participant's business card from the directory.
	 *
	 * @param p	the meta-data of the Participant to remove from the directory
	 * @throws DirectoryException when the Participant's business card could not be removed from the directory
	 */
	public void removeParticipantInfo(Participant p) throws DirectoryException {
		try {
			integrator().removeParticipantInfo(p);
		} catch (DirectoryException e) {
			log.error("Error removing participant info (PID={}) : {}", p.getId().toString(), Utils.getExceptionTrace(e));
			throw e;
		}
		
	}

	private IDirectoryIntegrator integrator() throws BeansException {
		if (directoryService == null) 
			try {
				directoryService = serviceFactory.getBean(implSvcName, IDirectoryIntegrator.class);
			} catch (BeansException svcUnavailable) {
				log.error("Error loading the Directory Client implementation {} : {}", implSvcName, Utils.getExceptionTrace(svcUnavailable));
				throw svcUnavailable;
			}

		return directoryService;
	}
	
}
