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

import java.util.ArrayList;
import java.util.List;

import org.holodeckb2b.bdxr.smp.datamodel.ProcessGroup;
import org.holodeckb2b.bdxr.smp.server.datamodel.Service;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataTemplate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Is the JPA entity for {@link ServiceMetadataTemplate}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity(name = "ServiceMetadataTemplate")
@Getter
@NoArgsConstructor
public class ServiceMetadataTemplateEntity extends BaseMetadataRegistrationEntity<Long> implements ServiceMetadataTemplate {

	@ManyToOne(optional = false)
	@NotNull(message = "A Service must be selected")
	protected ServiceEntity	service;

	@OneToMany(mappedBy = "template", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	protected List<ProcessGroupEntity>	processMetadata = new ArrayList<>();

	@Override
	public Long getId() {
		return oid;
	}
	
	@Override
	public void setId(Long id) {
		oid = id;
	}

	@Override
	public void setService(Service svc) {
		if (svc == null )
			throw new IllegalArgumentException("Service shall not be null");
		else if (!(svc instanceof ServiceEntity) || ((ServiceEntity) svc).getOid() == null)
			throw new IllegalArgumentException("Service instance is not managed");
		this.service = (ServiceEntity) svc;
	}

	@Override
	public void addProcessGroup(ProcessGroup pg) {
		this.processMetadata.add(new ProcessGroupEntity(this, pg));
	}

	@Override
	public void removeProcessGroup(org.holodeckb2b.bdxr.smp.server.datamodel.ProcessGroup pg) {
		this.processMetadata.remove(pg);		
	}
}
