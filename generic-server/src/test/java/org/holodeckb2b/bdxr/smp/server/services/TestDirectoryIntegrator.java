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

import java.util.ArrayList;
import java.util.List;

import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.datamodel.Participant;
import org.holodeckb2b.bdxr.smp.server.services.network.DirectoryException;
import org.holodeckb2b.bdxr.smp.server.services.network.DirectoryIntegrationService;
import org.springframework.stereotype.Service;

/**
 * Mock implementation of the Directory integration service.
 */
@Service("TestDirectoryIntegrator")
public class TestDirectoryIntegrator implements DirectoryIntegrationService {

	public boolean requireSMLRegistration = false;

	public List<Identifier> publications = new ArrayList<>();	
	
	private DirectoryException	rejection = null;

	public void reset() {
		publications.clear();
	}
	
	public void rejectNextWith(DirectoryException e) {
		rejection = e;
	}
	
	@Override
	public String getDirectoryName() {
		return "Test Directory";
	}

	@Override
	public boolean isSMLRegistrationRequired() {
		return requireSMLRegistration;
	}

	@Override
	public void publishParticipantInfo(Participant p) throws DirectoryException {
		checkRejection();
		this.publications.add(p.getId());
	}

	@Override
	public void removeParticipantInfo(Participant p) throws DirectoryException {
		checkRejection();
		publications.removeIf(l -> l.equals(p.getId()));
	}

	private void checkRejection() throws DirectoryException {
		if (rejection != null)
			throw rejection;		
	}
}
