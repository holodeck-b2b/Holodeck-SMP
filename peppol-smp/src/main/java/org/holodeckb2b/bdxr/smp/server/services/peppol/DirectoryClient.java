/*
 * Copyright (C) 2023 The Holodeck B2B Team
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
package org.holodeckb2b.bdxr.smp.server.services.peppol;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLKeyException;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.holodeckb2b.bdxr.smp.server.datamodel.Participant;
import org.holodeckb2b.bdxr.smp.server.services.core.SMPServerAdminService;
import org.holodeckb2b.bdxr.smp.server.services.network.DirectoryException;
import org.holodeckb2b.bdxr.smp.server.services.network.DirectoryIntegrationService;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.ws.client.core.WebServiceTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * Implements the integration of the SMP with the Peppol Directory.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Slf4j
@Service("PEPPOLDirectoryClient")
public class DirectoryClient implements DirectoryIntegrationService {

	@Value("${peppol.directory.prod.name:Peppol Production Directory}")
	protected String prodName;
	@Value("${peppol.directory.prod.url:https://directory.peppol.eu/}")
	protected String prodURL;

	@Value("${peppol.directory.acc.name:Peppol Acceptance Directory}")
	protected String testName;
	@Value("${peppol.directory.acc.url:https://test-directory.peppol.eu/}")
	protected String testURL;

	@Value("${peppol.directory.ssl.verifyhostname:true}")
	protected boolean verifyHostname;

	@Value("${peppol.directory.ssl.truststore:}")
	protected String sslTrustStorePath;
	@Value("${peppol.directory.ssl.truststore.pwd:}")
	protected String sslTrustStorePwd;

	@Lazy
	@Autowired
	protected SMPServerAdminService	adminSvc;
	
	@Override
	public String getDirectoryName() {
		X509Certificate smpCert = adminSvc.getServerMetadata().getCertificate();
		if (smpCert == null) {
			log.warn("Directory name cannot be determined as SMP certificate is not available!");
			return null;
		}
		return CertificateUtils.getIssuerName(smpCert).toLowerCase().contains("test") ? testName : prodName;
	}

	@Override
	public boolean isSMLRegistrationRequired() {
		return true;
	}	
	
	@Override
	public void publishParticipantInfo(Participant p) throws DirectoryException {		
		try {
			restTemplate().put(targetURL(), p.getId().toString());
		} catch (Exception failedRequest) {
			Logger.getLogger(DirectoryClient.class.getName()).log(Level.SEVERE,
					"Error registering participant in directory : {0}", Utils.getExceptionTrace(failedRequest));
			throw new DirectoryException(failedRequest);
		}
	}

	@Override
	public void removeParticipantInfo(Participant p) throws DirectoryException {
		try {
			restTemplate().delete(targetURL() + "/" + p.getId().getURLEncoded());
		} catch (Exception failedRequest) {
			Logger.getLogger(DirectoryClient.class.getName()).log(Level.SEVERE,
					"Error registering participant in directory : {0}", Utils.getExceptionTrace(failedRequest));
			throw new DirectoryException(failedRequest);
		}
	}

	/**
	 * Determines the base URL of the SML interface based on the installed SMP certificate. The default Peppol URLs can
	 * be overridden by setting the <code>peppol.sml.prod.url</code> and <code>peppol.sml.acc.url</code> application 
	 * properties in <code>common.properties</code>.
	 *
	 * @return	the URL where the SML interface is located
	 */
	private String targetURL() {
		X509Certificate smpCert = adminSvc.getServerMetadata().getCertificate();
		if (smpCert == null) {
			log.error("Directory function called before SMP certificate is available!");
			return null;
		} else
			return CertificateUtils.getIssuerName(smpCert).toLowerCase().contains("test") ? testURL : prodURL;		
	}
	
	/**
	 * Prepares a Spring {@link WebServiceTemplate} for a call to the SML interface.
	 *
	 * @return a {@link WebServiceTemplate} instance configured for executing calls to the SML
	 * @throws SSLException
	 */
	private RestTemplate restTemplate() throws SSLException {
		RestTemplate restTemplate = new RestTemplate();

		SSLConnectionSocketFactory sslFactory = verifyHostname ?
													new SSLConnectionSocketFactory(sslContext()) :
													new SSLConnectionSocketFactory(sslContext(),
																				   NoopHostnameVerifier.INSTANCE);

		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("https", sslFactory)
				.register("http", new PlainConnectionSocketFactory())
				.build();
		
		CloseableHttpClient httpClient = HttpClients.custom()
				.setConnectionManager(new BasicHttpClientConnectionManager(socketFactoryRegistry))
				.build();
        
		ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        
		restTemplate.setRequestFactory(requestFactory);

		return restTemplate;
	}

	/**
	 * Creates the <code>SSLContext</code> for the connections to the SML. It uses the given SMP key pair for client
	 * authentication and can use a customised trust store for validation of the SML server certificate. To use a
	 * customised trust store the <code>peppol.directory.ssl.truststore</code> and 
	 * <code>peppol.directory.ssl.truststore</code> application properties should be set in 
	 * <code>common.properties</code>.
	 *
	 * @return	the SSLContext for creating connections to the SML
	 * @throws SSLException	when either the key pair for client authentication or the custom trust store for server
	 *						authentication cannot be processed
	 */
	private SSLContext sslContext() throws SSLException {
		SSLContextBuilder ctxBldr = SSLContexts.custom();
		try {
			char[] pwd = new char[] {};
			KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(null, null);
			keyStore.setEntry("1", adminSvc.getActiveKeyPair(), new KeyStore.PasswordProtection(pwd));
			ctxBldr.loadKeyMaterial(keyStore, pwd);
		} catch (IOException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException
				| CertificateException keyFailure) {
			throw new SSLKeyException("Could not create key store for client authentication");
		}
		try {
			if (!Utils.isNullOrEmpty(sslTrustStorePath))
				ctxBldr.loadTrustMaterial(new File(sslTrustStorePath), sslTrustStorePwd.toCharArray());
		} catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException trustFailure) {
			throw new SSLKeyException("Could not load specified trust store for TLS authentication");
		}
		try {
			return ctxBldr.build();
		} catch (NoSuchAlgorithmException | KeyManagementException ex) {
			Logger.getLogger(SMLClient.class.getName()).severe("Error creating SSL context : "
																+ Utils.getExceptionTrace(ex));
			throw new SSLException("Could not setup SSL context for Directory connection", ex);
		}
	}

	
}
