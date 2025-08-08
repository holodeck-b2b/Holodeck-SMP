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
package org.holodeckb2b.bdxr.smp.server.ui.auth;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;

/**
 * Generates the QR-Code for registration of the TOTP in an authenticator app. 
 * <p>
 * This generator includes an additional <i>image</i> parameter in the URL so the correct logo is shown in the 
 * authenticator app. Because {@link QrData} cannot be extended the image parameter is included in this class.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class QrCodeGenerator implements QrGenerator {
	private String portalUrl;
	private int    size = 350;
	
	/** 
	 * Creates a new generator which will use the given URL as base path for the image parameter and generate a QR code
	 * of 350px.   
	 * 
	 * @param url 	base URL to construct the image parameter
	 */
	public QrCodeGenerator(String url) {
		portalUrl = url;
	}
	
	/** 
	 * Creates a new generator which will use the given URL as base path for the image parameter and generate a QR code
	 * of 350px.   
	 * 
	 * @param url 	base URL to construct the image parameter
	 * @param size  the size of the QR code to generate, in pixels
	 */
	public QrCodeGenerator(String url, int size) {
		portalUrl = url;
		this.size = size;
	}

    @Override
    public byte[] generate(QrData data) throws QrGenerationException {
        try {
        	String url = data.getUri();
        	if (portalUrl != null)         		
        		url += "&image=" + URLEncoder.encode(portalUrl 
        											+ (portalUrl.endsWith("/") ? "/" : "" ) + "/img/logo.png",         			
        											StandardCharsets.UTF_8).replaceAll("\\+", "%20");        						 
        	
        	BitMatrix bitMatrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, size, size);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            throw new QrGenerationException("Failed to generate QR code. See nested exception.", e);
        }
    }

	@Override
	public String getImageMimeType() {
		return "image/png";
	}		
}
