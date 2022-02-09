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
package org.holodeckb2b.bdxr.smp.server.ui.viewmodels;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Date;
import lombok.Getter;
import org.holodeckb2b.commons.security.CertificateUtils;

/**
 * UI model for showing the meta-data of a X509.v3 Certificate. Because {@link X509Certificate} cannot be accessed by
 * the view layer (due to access restrictions) we need to use a separate view model.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Getter
public class X509CertificateData {

	protected String	subjectName;
	protected String	issuerName;
	protected BigInteger serialNo;
	protected Date		notBefore;
	protected Date		notAfter;

	protected String	message;

	public X509CertificateData(X509Certificate c) {
		subjectName = CertificateUtils.getSubjectName(c);
		issuerName = CertificateUtils.getIssuerName(c);
		serialNo = c.getSerialNumber();
		notBefore = c.getNotBefore();
		notAfter = c.getNotAfter();
	}

	public X509CertificateData(String msg) {
		message = msg;
	}
}
