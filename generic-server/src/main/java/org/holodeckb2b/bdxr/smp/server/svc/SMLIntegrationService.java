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
import org.holodeckb2b.bdxr.smp.server.db.entities.ParticipantE;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Is a proxy to the {@link ISMLIntegrator} implementation so different implementations can be used depending on the 
 * network in which the SMP Server operates.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Service
@Slf4j
public class SMLIntegrationService {

	@Value("${sml.enabled:false}")
	protected boolean	smlEnabled;
	@Value("${sml.implementatation:PEPPOLSMLClient}")
	protected String	implSvcName;
	@Autowired
	protected BeanFactory smlServiceFactory;

	private ISMLIntegrator smlService;

	/**
	 * @return <code>true</code> when the SML integration is enabled and the SMP is registered in the SML,
	 *		   <code>false</code> otherwise
	 */
	public boolean isSMLIntegrationAvailable() {
		try {
			return smlEnabled && smlIntegrator().isSMPRegistered();
		} catch (BeansException ex) {
			return false;
		}
	}

	/**
	 * Registers a Participant in the SML.
	 *
	 * @param p	the meta-data of the Participant to register
	 * @throws SMLException when the Participant could not be registered in the SML
	 */
	public void registerParticipant(ParticipantE p) throws SMLException {
		try {
			smlIntegrator().registerParticipant(p);
		} catch (SMLException e) {
			log.error("Error registering participant (PID={}) in SML : {}", p.getId().toString(), Utils.getExceptionTrace(e));
			throw e;
		}
	}

	/**
	 * Remove the registration of a Participant from the SML.
	 *
	 * @param p	the meta-data of the Participant to remove from the SML
	 * @throws Exception when the Participant could not be removed in the SML
	 */
	public void unregisterParticipant(ParticipantE p) throws SMLException {
		try {
			smlIntegrator().unregisterParticipant(p);
		} catch (SMLException e) {
			log.error("Error registering participant (PID={}) in SML : {}", p.getId().toString(), Utils.getExceptionTrace(e));
			throw e;
		}
	}

	/**
	 * Prepares the migration of the Participant by registering the migration code in the SML.
	 * 
	 * @param p		the meta-data on the Participant
	 * @throws SMLException when there is an error executing the update to the SML
	 */
	public void registerMigrationCode(ParticipantE p) throws SMLException {
		try {
			smlIntegrator().registerMigrationCode(p, p.getMigrationCode());
		} catch (SMLException e) {
			log.error("Error registering migration code for participant (PID={}) in SML : {}", 
						p.getId().toString(), Utils.getExceptionTrace(e));
			throw e;
		}
	}
	
	/**
	 * Migrates the Participant to this SMP.
	 * 
	 * @param p		the meta-data on the Participant being migrated
	 * @param code  the migration code provided by the other SMP to register the Participant with this SMP 
	 * @throws SMLException when there is an error executing the update to the SML
	 */
	public void migrateParticipant(ParticipantE p, String migrationCode) throws SMLException {
		try {
			smlIntegrator().migrateParticipant(p, migrationCode);
		} catch (SMLException e) {
			log.error("Error migrating participant (PID={}) in SML : {}", 
						p.getId().toString(), Utils.getExceptionTrace(e));
			throw e;
		}
	}

	
	/**
	 * Indicates whether the SMP Certificate needs to be registered in the SML
	 *
	 * @return <code>true</code> if the SML integration is active and the certificate needs to be registered in the SML,
	 *		   <code>false</code> otherwise
	 */
	public boolean requiresSMPCertRegistration() throws SMLException {
		try {
			return isSMLIntegrationAvailable() && smlIntegrator().requiresSMPCertRegistration();
		} catch (BeansException missingSMLComp) {
			throw new SMLException(missingSMLComp);
		}
	}

	private ISMLIntegrator smlIntegrator() throws BeansException {
		if (smlService == null)
			try {
				smlService = smlServiceFactory.getBean(implSvcName, ISMLIntegrator.class);
			} catch (BeansException svcUnavailable) {
				log.error("Error loading the SML Client implementation {} : {}", implSvcName, Utils.getExceptionTrace(svcUnavailable));
				throw svcUnavailable;
			}

		return smlService;
	}

}
