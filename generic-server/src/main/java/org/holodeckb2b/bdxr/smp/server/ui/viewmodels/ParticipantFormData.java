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
package org.holodeckb2b.bdxr.smp.server.ui.viewmodels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import org.holodeckb2b.bdxr.smp.datamodel.impl.IDSchemeImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.IdentifierImpl;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataTemplate;
import org.holodeckb2b.bdxr.smp.server.db.entities.ParticipantEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.ServiceMetadataTemplateEntity;
import org.holodeckb2b.commons.util.Utils;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
/**
 * UI model for editing the meta-data of a Participant. Extends {@link ParticipantEntity} with information about the SML
 * registration and migration status and handling of the additional identifiers using a comma separated list. 
 * <p>
 * Because the SML registration and migration state can be edited by the user their initial state when the form is 
 * loaded should be maintained to prevent incorrect formatting.  
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@NoArgsConstructor
public class ParticipantFormData extends ParticipantEntity {

	/**
	 * Indicates if the participant is currently registered in the SML. 
	 */
	@Getter
	@Setter
	private boolean isInSML;
	
	/**
	 * Indicates if the participant is currently being migrated to another SMP in the SML. 
	 */
	@Getter
	@Setter
	private boolean isMigrating;
				
	public ParticipantFormData(ParticipantEntity participantEntity) {
		super(participantEntity);
		this.isInSML = participantEntity.isRegisteredInSML();
		this.isMigrating = !Utils.isNullOrEmpty(participantEntity.getSMLMigrationCode());
	}
	
	public ParticipantEntity asEntity() {
		return super.clone();
	}
		
	public String getAdditionalIdsCSL() {
		return getAdditionalIds().stream().map(id -> id.toString()).collect(Collectors.joining(","));
	}
	
	public void setAdditionalIdsCSL(String idsCSL) {
		clearAdditionalIds();
		if (Utils.isNullOrEmpty(idsCSL))
			return;
		
		String[] ids = idsCSL.split(",");
		for (String id : ids) {
			int sep = id.indexOf("::");
			String sid = id.substring(0, Math.max(0, sep));
			String val = sep < 0 ? id: id.substring(sep + 2);					
			addAdditionalId(new IdentifierImpl(val, Utils.isNullOrEmpty(sid) ? null 
													: new IDSchemeImpl(sid, !val.equals(val.toLowerCase()))));
		}	
	}	
}
