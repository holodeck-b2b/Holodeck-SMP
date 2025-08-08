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

import java.io.IOException;

import org.holodeckb2b.bdxr.smp.server.utils.DataEncryptor;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles the registration of the TOTP based second factor during the login process. When 2FA is configured to be 
 * required and the user has not yet enabled 2FA the user will be redirected to the registration page handled by this
 * controller.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Controller
@RequestMapping("/mfa/register")
@Slf4j
public class TOTPRegistrationController {

	@Value("${smp.external-url:http://localhost:${server.port:8080}}")
	private String portalUrl;
	
	@Autowired
	private DataEncryptor	encryptor;
	
	@Autowired
	private UserAccountMgmtService	userMgmtSvc;

	private AuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();

	private static final CodeVerifier verifier;
	
	static {
		verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
		// When verifying the registration we are a bit more relaxed and will extend the time period
		((DefaultCodeVerifier) verifier).setAllowedTimePeriodDiscrepancy(2);
	}
	
	@GetMapping("/")
	public String getRegistrationPage(HttpServletRequest request, HttpServletResponse response) 
																				throws IOException, ServletException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof MFAAuthToken))
			return "redirect:/";
		
		UserAccount user = (UserAccount) authentication.getPrincipal();
		if (user.getTotpSecret() != null) {
			Authentication origAuth = ((MFAAuthToken) authentication).getFirst();
			SecurityContextHolder.getContext().setAuthentication(origAuth);
			successHandler.onAuthenticationSuccess(request, response, origAuth);
			return null;
		} else {
			return "authentication/totp_registration";
		}
		
	}
	
	/**
	 * Represents the response to the request to prepare a TOTP registration. Contains the base64 encoded and QR-code 
	 * representation of the secret for registration of the authenticator app.   
	 */
	public record TOTPSecret(String key, String qr) {
	}
	
	@GetMapping("prepare")	
	@ResponseBody
	public TOTPSecret getRegistrationData(HttpSession session) {
		final String secret = new DefaultSecretGenerator().generate();

		try {
			QrData qrData = new QrData.Builder()
										.secret(secret)
										.issuer("Holodeck SMP")
										.label("2FA Registration")										
										.algorithm(HashingAlgorithm.SHA1)
										.digits(6)
										.period(30)
										.build();
			QrGenerator imgGenerator = new QrCodeGenerator(portalUrl);
			TOTPSecret totpSecret = new TOTPSecret(secret, dev.samstevens.totp.util.Utils.getDataUriForImage(
													  imgGenerator.generate(qrData), imgGenerator.getImageMimeType()));
			session.setAttribute("totp-secret", secret);
			return totpSecret;
		} catch (QrGenerationException qrError) {
			log.error("Error creating the QR-code for TOTP registration : {}", qrError.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}		
	}
	
	@PostMapping("verify")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void verifyRegistration(@AuthenticationPrincipal UserAccount user, HttpSession session,
								 @RequestParam("deviceName") String device,
								 @RequestParam("verificationCode") String code) {
		final String secret = (String) session.getAttribute("totp-secret");		
		if (!verifier.isValidCode(secret, code)) {
			log.warn("Could not verify new 2FA for user {} due to code mismatch", user.getEmailAddress());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Code mismatch");
		} 
	}
	
	@PostMapping("verifyandsave")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void saveRegistration(@AuthenticationPrincipal UserAccount user, HttpSession session, 
								 @RequestParam("deviceName") String device,
								 @RequestParam("verificationCode") String code) {		
		final String secret = (String) session.getAttribute("totp-secret");		
		if (verifier.isValidCode(secret, code)) {
			try {
				user.setTotpDevice(device);
				user.setTotpSecret(encryptor.encrypt(secret.getBytes()));
				userMgmtSvc.updateUser(user, user);
				log.info("Registered new 2FA device {} for user {}", device, user.getEmailAddress());
			} catch (Exception updateError) {
				log.error("An error occurred registering 2FA for user {} : {}", user.getEmailAddress(), 
						  Utils.getExceptionTrace(updateError));
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} else {
			log.warn("Could not verify new 2FA for user {} due to code mismatch", user.getEmailAddress());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Code mismatch");
		} 
	}

}
