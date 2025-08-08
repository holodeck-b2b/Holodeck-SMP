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
package org.holodeckb2b.bdxr.smp.server.db.entities;

import org.holodeckb2b.bdxr.smp.server.datamodel.ITypeSpecification;
import org.holodeckb2b.bdxr.smp.server.datamodel.Identifier;
import org.holodeckb2b.commons.util.Utils;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

/**
 * Is a base implementation for entities that store a {@link ITypeSpecification type specification}. 
 * 
 * @param <I>	the data model class of the identifier used by the entity 
 * @param <EI>	the storage class of the identifier, must be a subclass of <code>&lt;I&gt;</code>
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@MappedSuperclass
abstract class AbstractTypeSpecificationEntity<I extends Identifier, EI extends I>
												extends AbstractIdBasedEntity<I, EI> implements ITypeSpecification<I> {

	@Column
	@Getter
	@Setter
	private String	specificationRef;	
	
	@PrePersist 
	@PreUpdate 
	private void setDefaultName(){
		if (Utils.isNullOrEmpty(name))
        	name = getId() != null ? getId().toString() : null;
    }
	
	protected AbstractTypeSpecificationEntity() {
		super();
	}

	protected AbstractTypeSpecificationEntity(I id) {
		super(id);
	}
	
	protected AbstractTypeSpecificationEntity(I id, String name, String specificationRef) {
		super(id);		
		setName(name);
		this.specificationRef = specificationRef;
	}
}
