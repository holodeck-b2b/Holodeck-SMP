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

import org.holodeckb2b.bdxr.smp.server.db.entities.ParticipantE;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author safi
 */
@Service
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
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Registers a Participant in the SML.
	 *
	 * @param p	the meta-data of the Participant to register
	 * @throws Exception when the Participant could not be registered in the SML
	 */
	public void registerParticipant(ParticipantE p) throws Exception {
		smlIntegrator().registerParticipant(p);
	}

	/**
	 * Remove the registration of a Participant from the SML.
	 *
	 * @param p	the meta-data of the Participant to remove from the SML
	 * @throws Exception when the Participant could not be removed in the SML
	 */
	public void unregisterParticipant(ParticipantE p) throws Exception {
		smlIntegrator().unregisterParticipant(p);
	}

	/**
	 * Indicates whether the SMP Certificate needs to be registered in the SML
	 *
	 * @return <code>true</code> if the SML integration is active and the certificate needs to be registered in the SML,
	 *		   <code>false</code> otherwise
	 */
	public boolean requiresSMPCertRegistration() throws BeansException {
		return isSMLIntegrationAvailable() && smlIntegrator().requiresSMPCertRegistration();
	}

	private ISMLIntegrator smlIntegrator() throws BeansException {
		if (smlService == null)
			smlService = smlServiceFactory.getBean(implSvcName, ISMLIntegrator.class);

		return smlService;
	}

}
