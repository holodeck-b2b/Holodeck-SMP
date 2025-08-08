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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.holodeckb2b.bdxr.smp.datamodel.IDScheme;
import org.holodeckb2b.bdxr.smp.server.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.utils.IdUtils;

import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import jakarta.validation.Valid;

/**
 * Is a base implementation for entities that use an {@link Identifier} instance for identification of registrations.
 * It checks that when an identifier is set which declares an id scheme that the given {@link IDScheme} instance is 
 * managed by the SMP server. 
 * 
 * @param <I>	the data model class of the identifier used by the entity 
 * @param <EI>	the storage class of the identifier, must be a subclass of <code>&lt;I&gt;</code>
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@MappedSuperclass
public abstract class AbstractIdBasedEntity<I extends Identifier, EI extends I> extends BaseMetadataRegistrationEntity<I> {

	@Embedded
	@Valid
	private EI 		id;
	
	@Transient
	private Constructor<EI>	idConstructor;

	/**
	 * Initialises a new instance of this class without an identifier.
	 */
	protected AbstractIdBasedEntity() {
		super();
		init();
	}
	
	/**
	 * Initialises a new instance of this class and sets the identifier.
	 * 
	 * @param id	identifier of the new instance
	 */
	protected AbstractIdBasedEntity(I id) {
		super();
		init();
		setId(id);
	}
	
	@SuppressWarnings("unchecked")
	private void init() {		
		Type genericSuperclass = this.getClass().getGenericSuperclass();
		if (!(genericSuperclass instanceof ParameterizedType))
			try {
				genericSuperclass = Class.forName(genericSuperclass.getTypeName()).getGenericSuperclass();
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}		
		String idClsName = ((ParameterizedType) genericSuperclass).getActualTypeArguments()[1].getTypeName();
		try {
			idConstructor = ((Class<EI>) Class.forName(idClsName)).getConstructor();
		} catch (ClassNotFoundException e) {
			// Cannot happen because this class is already needed to compile the child classes
		} catch (NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException(idClsName + " is not a valid storage identifier - missing default constructor");
		}
	}
	
	public EI getId() {
		return id;
	}
	
	/**
	 * Sets the identifier of the registration.
	 * 
	 * @param id	identifier to set
	 */
	public void setId(I id) {
		if (id == null)
			throw new IllegalArgumentException("Id cannot be null");
		IDScheme ids = id.getScheme();
		IdUtils.checkManagedScheme(ids);
		
		EI newStorageId = null;
		try {
			newStorageId = idConstructor.newInstance();
			
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException("Could not create identifier class [" + 
										idConstructor.getDeclaringClass().getName() + "]", e);
		}
		newStorageId.setScheme((IDSchemeEntity) ids);
		newStorageId.setValue(id.getValue());
		this.id = newStorageId;
	}
}
