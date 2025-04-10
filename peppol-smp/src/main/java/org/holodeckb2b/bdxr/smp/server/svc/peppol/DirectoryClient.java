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
package org.holodeckb2b.bdxr.smp.server.svc.peppol;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.holodeckb2b.bdxr.smp.server.db.repos.ParticipantRepository;
import org.holodeckb2b.bdxr.smp.server.svc.DirectoryException;
import org.holodeckb2b.bdxr.smp.server.svc.IDirectoryIntegrator;
import org.holodeckb2b.bdxr.smp.server.svc.SMPCertificateService;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * Implements the integration of the SMP with the Peppol Directory.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Service("PEPPOLDirectoryClient")
public class DirectoryClient implements IDirectoryIntegrator {

	@Value("${directory.prod_url:https://directory.peppol.eu/}")
	protected String prodURL;
	@Value("${directory.acc_url:https://test-directory.peppol.eu/}")
	protected String accURL;
	@Value("${sml.ssl-truststore:}")
	protected String sslTrustStorePath;
	@Value("${sml.ssl-truststore-pwd:}")
	protected String sslTrustStorePwd;

	@Autowired
	protected SMPCertificateService	certSvc;
	@Autowired
	protected ParticipantRepository participants;

	private URI targetURL;
	
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
	 * Determines the URL of the Peppol directory interface based on the installed SMP certificate. The default Peppol URLs can
	 * be overriden by setting the <code>directory.prod_url</code> and <code>directory.acc_url</code> application properties in
	 * <code>common.properties</code>.
	 *
	 * @return	the URL where the SML interface is located
	 */
	private URI targetURL() {
		if (targetURL == null) {
			try {
				KeyStore.PrivateKeyEntry keyPair = certSvc.getKeyPair();
				String baseURL = CertificateUtils.getIssuerName((X509Certificate) keyPair.getCertificate()).toLowerCase()
																.contains("test") ? accURL : prodURL;
				targetURL = new URI(baseURL + "indexer/1.0/");				
			} catch (CertificateException ex) {
				Logger.getLogger(DirectoryClient.class.getName()).log(Level.SEVERE,
																"Could not retrieve SMP cert : {0}", ex.getMessage());
			} catch (URISyntaxException invalidURL) {
				Logger.getLogger(DirectoryClient.class.getName()).log(Level.SEVERE,
						"Invalid URL specified for directory API : {0}", invalidURL.getMessage());
			}
		}
		return targetURL;
	}
	
	/**
	 * Prepares a Spring {@link WebServiceTemplate} for a call to the SML interface.
	 *
	 * @return a {@link WebServiceTemplate} instance configured for executing calls to the SML
	 * @throws SSLException
	 */
	private RestTemplate restTemplate() throws SSLException {
		RestTemplate restTemplate = new RestTemplate();

		/* When the configured SML base URL is for localhost we turn off hostname verification as otherwise the
		 * following error is probably thrown:
		 *		java.security.cert.CertificateException: No name matching localhost found
		 */
		KeyStore.PrivateKeyEntry keyPair;
		try {
			keyPair = certSvc.getKeyPair();
		} catch (CertificateException ex) {
			throw new SSLException("Could not retrieve SMP key pair");
		}
		SSLConnectionSocketFactory sslFactory = targetURL().getHost().contains("localhost") ?
													new SSLConnectionSocketFactory(sslContext(keyPair),
																				   NoopHostnameVerifier.INSTANCE)
												  : new SSLConnectionSocketFactory(sslContext(keyPair));

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
	 * customised trust store the <code>sml.ssl-truststore</code> and <code>sml.ssl-truststore-pwd</code> application
	 * properties must be set in <code>common.properties</code>.
	 *
	 * @param pk	the key pair to use for client authentication
	 * @return	the SSLContext for creating connections to the SML
	 * @throws SSLException	when either the key pair for client authentication or the custom trust store for server
	 *						authentication cannot be processed
	 */
	private SSLContext sslContext(KeyStore.PrivateKeyEntry pk) throws SSLException {
		SSLContextBuilder ctxBldr = SSLContexts.custom();
		try {
			char[] pwd = new char[] {};
			KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(null, null);
			keyStore.setEntry("1", pk, new KeyStore.PasswordProtection(pwd));
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
			throw new SSLException("Could not setup SSL context for SML connection", ex);
		}
	}	
}
