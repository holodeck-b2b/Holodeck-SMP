/*
 * Copyright (C) 2022 The Holodeck B2B Team
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
package org.holodeckb2b.bdxr.smp.server.db.entities;

import org.holodeckb2b.bdxr.smp.server.datamodel.Process;
import org.holodeckb2b.bdxr.smp.server.datamodel.ProcessIdentifier;

import jakarta.persistence.Entity;

/**
 * Is the JPA entity for storing {@link Process} meta-data in the database.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity(name = "Process")
public class ProcessEntity extends AbstractTypeSpecificationEntity<ProcessIdentifier, EmbeddedProcessIdentifier> 
							implements Process {
	
	public ProcessEntity() {
		super();
	}
	
	public ProcessEntity(ProcessIdentifier id) {
		super(id);
	}
	
	public ProcessEntity(ProcessIdentifier id, String name, String specRef) {
		super(id, name, specRef);
	}	
	
	public void setId(ProcessIdentifier id) {
		super.setId(!id.isNoProcess() ? id : new EmbeddedProcessIdentifier(EmbeddedProcessIdentifier.NO_PROCESS));		
	}
}
