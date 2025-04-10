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
package org.holodeckb2b.bdxr.smp.server.queryapi.oasisv2;

import java.util.Base64;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Is a custom {@link XmlAdapter} to ensure that the <code>Certificate/ContentBinaryObject</code> element in the
 * <i>ServiceMetadata</i> response is formatted as required by the PEM specification (RFC1421), i.e. uses a line lentgth
 * of 64 characters.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class PEMAdapter extends XmlAdapter<String, byte[]>{

	@Override
	public byte[] unmarshal(String v) throws Exception {
		return Base64.getDecoder().decode(v);
	}

	@Override
	public String marshal(byte[] v) throws Exception {
		return Base64.getMimeEncoder(64, "\r\n".getBytes()).encodeToString(v);
	}

}
