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
package org.holodeckb2b.bdxr.smp.server.utils;

import java.beans.PropertyEditorSupport;

import org.holodeckb2b.bdxr.smp.datamodel.IDScheme;
import org.holodeckb2b.bdxr.smp.server.services.core.IdSchemeMgmtService;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;

/**
 * Is a property editor for the {@link IDScheme} class so the scheme ID string can be used in the templates.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class IDSchemeConvertor extends PropertyEditorSupport {
	
	private IdSchemeMgmtService idsSvc;

	public IDSchemeConvertor(IdSchemeMgmtService idsSvc) {
		this.idsSvc = idsSvc;
	}
	
	@Override
	public String getAsText() {
		IDScheme ids = (IDScheme) getValue();
		return ids != null ? ids.getSchemeId() : null;
	}
	
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		try {
			setValue(idsSvc.getIDScheme(text));
		} catch (PersistenceException e) {
			setValue(null);
		}
	}
}
