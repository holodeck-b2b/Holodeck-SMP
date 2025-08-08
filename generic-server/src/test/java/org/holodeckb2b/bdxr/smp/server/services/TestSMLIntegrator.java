/*
 * Copyright (C) 2025 The Holodeck B2B Team
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
package org.holodeckb2b.bdxr.smp.server.services;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.holodeckb2b.bdxr.smp.datamodel.Certificate;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.datamodel.Participant;
import org.holodeckb2b.bdxr.smp.server.datamodel.SMPServerMetadata;
import org.holodeckb2b.bdxr.smp.server.services.network.SMLException;
import org.holodeckb2b.bdxr.smp.server.services.network.SMLIntegrationService;
import org.springframework.stereotype.Service;

/**
 * Mock implementation of the SML integration service.
 */
@Service("TestSMLIntegrator")
public class TestSMLIntegrator implements SMLIntegrationService {

	public boolean requireSMPRegistration = false;
	public boolean requireSMPCertRegistration = false;

	public SMPServerMetadata smp;
	public Certificate cert; 
	
	public Set<Identifier> participants = new HashSet<>();
	public HashMap<Identifier, String> migrations = new HashMap<>();
	
	private SMLException	rejection = null;
	private boolean rejected;
	
	public void reset() {
		smp = null;
		requireSMPRegistration = false;
		requireSMPCertRegistration = false;
		participants.clear();
		migrations.clear();
	}
	
	public void rejectNextWith(SMLException e) {
		rejection = e;
		rejected = false;
	}
	
	@Override
	public String getSMLName() {
		return "Dev-Test SML";
	}
	
	@Override
	public boolean requiresSMPRegistration() {
		return requireSMPRegistration;
	}

	@Override
	public boolean requiresSMPCertRegistration() {
		return requireSMPCertRegistration;
	}
	
	@Override
	public void registerSMPServer(SMPServerMetadata smp) throws SMLException {
		checkRejection();
		this.smp = smp;
	}

	@Override
	public void updateSMPServer(SMPServerMetadata updated) throws SMLException {
		checkRejection();
		this.smp = updated;
	}

	@Override
	public void updateSMPCertificate(Certificate cert) throws SMLException {
		checkRejection();
		this.cert = cert;
	}
	
	@Override
	public void deregisterSMPServer(String smpId) throws SMLException {
		checkRejection();
		if (this.smp != null && this.smp.getSMPId().equals(smpId)) 
			this.smp = null;
		else
			throw new SMLException("SMP " + smpId + " not registered");
	}

	@Override
	public void registerParticipant(Participant p) throws SMLException {
		checkRejection();
		if (participants.contains(p.getId()))
			throw new SMLException("Participant " + p.getId() + " already registered");
		else
			participants.add(p.getId());
	}

	@Override
	public void deregisterParticipant(Participant p) throws SMLException {
		checkRejection();
		participants.remove(p.getId());
	}

	@Override
	public boolean isRegistered(Participant p) throws SMLException {
		checkRejection();
		return participants.contains(p.getId());
	}

	@Override
	public void registerMigrationCode(Participant p, String code) throws SMLException {
		checkRejection();
		if (!participants.contains(p.getId()))
			throw new SMLException("Participant " + p.getId().toString() + " not registered");
		migrations.put(p.getId(), code);
	}

	@Override
	public void migrateParticipant(Participant p, String code) throws SMLException {
		checkRejection();
		if (migrations.containsKey(p.getId()) && migrations.get(p.getId()).equals(code))
			migrations.remove(p.getId());
	}

	private void checkRejection() throws SMLException {
		if (rejection != null && !rejected) {
			rejected = true;
			throw rejection;	
		}
	}
}
