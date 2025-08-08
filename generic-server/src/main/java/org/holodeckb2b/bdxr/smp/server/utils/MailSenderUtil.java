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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.bdxr.smp.server.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.holodeckb2b.commons.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateEngineException;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.expression.ThymeleafEvaluationContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Is a utility component for sending emails from the SMP Server. It is based on the {@link JavaMailSender} Spring 
 * component and therefore configured through the <code>spring.mail.*</code> properties in the server's main
 * configuration file <code>common.properties</code>. In addition to the regular properties the <i>sender</i> property 
 * is added which must include the email address to use as sender of the messages.
 * <p>
 * The message body is a plain text created using Thymeleaf templates. The templates should be located in the <code>
 * /mail</code> resource folder. The extension should be <code>.txt</code>. Attachments can be added to the message 
 * either as inline or regular attachment. For inline attachments the <i>Content-Id</i> can be specified and for regular 
 * attachments the filename.
 * <p>
 * As access to an email server may not be available in all environments, it's possible to disable the sending of emails 
 * by leaving out the <code>spring.mail</code> properties in the configuration file. Components using this utility class
 * should check whether the mailer is available before sending emails.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Slf4j
@Component
public class MailSenderUtil {

	/**
	 * Represents an attachment to the email message to send. It has three fields:<ol>
	 * <li><code>content</code> : a {@link DataSource} holding the actual content of the attachment and its Mime type</li>
	 * <li><code>reference</code> : String containing the reference to the attachment. This is the Content-Id as used
	 * 	in the <code>cid:«reference»</code> link to the attachment when it is inlined. For regular attachments it is
	 * 	used to set the filename.</li>
	 * <li><code>inline</code> : boolean that indicates whether this is an inline attachment</li></ol>
	 *
	 */
	public static record Attachment(DataSource content, String reference, boolean inline) {};

	@Autowired(required = false)
	protected JavaMailSender	mailer;
	@Autowired
	protected ApplicationContext	appContext;
	
	/**
	 * The email address to use for the sender of the emails
	 */
	@Value("${spring.mail.sender:}")
	protected String			sender;

	/**
	 * The Thymeleaf engine to use for processing email messages. This is a separate engine to make configuration easier
	 * and avoid conflicts with the default engine used for rendering the web pages.
	 */
	private TemplateEngine		thEngine;

	/**
	 * Indicates whether the mailer is available for sending emails. 
	 *
	 * @return <code>true</code> when the mailer is available, <code>false</code> otherwise
	 */
	public boolean isAvailable() {
		if (mailer != null && !Utils.isNullOrEmpty(sender))
			return true;
		else if (mailer == null) 
			return false;
		else {
			log.warn("Missing sender's address for mail");
			return false;
		}			
	}
	
	/**
	 * Sends an email without attachments to a single recipient.
	 *
	 * @param to			email address of the recipient
	 * @param subject		the subject of the email
	 * @param template		name of the Thymeleaf template to use for constructing the message body
	 * @param vars			map containing the variables to be used in template processing
	 * @throws MailException when an error occurs preparing or sending the message
	 */
	public void sendMail(final String to, final String subject, final String template, final Map<String, Object> vars)
																							throws MailException {
		sendMail(new String[] { to }, null, subject, template, vars, null);
	}
	
	/**
	 * Sends an email without attachments to a single recipient.
	 *
	 * @param to			email address of the recipient
	 * @param from			email address of the sender
	 * @param subject		the subject of the email
	 * @param template		name of the Thymeleaf template to use for constructing the message body
	 * @param vars			map containing the variables to be used in template processing
	 * @throws MailException when an error occurs preparing or sending the message
	 */
	public void sendMail(final String to, final String from, final String subject, final String template, 
						 final Map<String, Object> vars) throws MailException {
		sendMail(new String[] { to }, from, subject, template, vars, null);
	}

	/**
	 * Sends an email with attachments to a single recipient.
	 *
	 * @param to			email address of the recipient
	 * @param subject		the subject of the email
	 * @param template		name of the Thymeleaf template to use for constructing the message body
	 * @param vars			map containing the variables to be used in template processing
	 * @param att			collection of {@link Attachment} objects representing the attachments to include
	 * @throws MailException when an error occurs preparing or sending the message
	 */
	public void sendMail(final String to, final String subject, final String template, final Map<String, Object> vars,
						 final Collection<Attachment> att) throws MailException {
		sendMail(new String[] { to }, null, subject, template, vars, att);
	}
	
	/**
	 * Sends an email with attachments to a single recipient.
	 *
	 * @param to			email address of the recipient
	 * @param from			email address of the sender
	 * @param subject		the subject of the email
	 * @param template		name of the Thymeleaf template to use for constructing the message body
	 * @param vars			map containing the variables to be used in template processing
	 * @param att			collection of {@link Attachment} objects representing the attachments to include
	 * @throws MailException when an error occurs preparing or sending the message
	 */
	public void sendMail(final String to, final String from, final String subject, final String template, 
						 final Map<String, Object> vars, final Collection<Attachment> att) throws MailException {
		sendMail(new String[] { to }, from, subject, template, vars, att);
	}

	/**
	 * Sends <b>one</b> email without attachments to multiple recipient.
	 *
	 * @param to			array containing the email addresses of the recipients
	 * @param subject		the subject of the email
	 * @param template		name of the Thymeleaf template to use for constructing the message body
	 * @param vars			map containing the variables to be used in template processing
	 * @throws MailException when an error occurs preparing or sending the message
	 */
	public void sendMail(final String[] to, final String subject, final String template, 
						final Map<String, Object> vars) throws MailException {
		sendMail(to, null, subject, template, vars, null);
	}
	
	/**
	 * Sends <b>one</b> email without attachments to multiple recipient.
	 *
	 * @param to			array containing the email addresses of the recipients
	 * @param from			email address of the sender
	 * @param subject		the subject of the email
	 * @param template		name of the Thymeleaf template to use for constructing the message body
	 * @param vars			map containing the variables to be used in template processing
	 * @throws MailException when an error occurs preparing or sending the message
	 */
	public void sendMail(final String[] to, final String from, final String subject, final String template, 
						 final Map<String, Object> vars) throws MailException {
		sendMail(to, from, subject, template, vars, null);
	}

	/**
	 * Sends <b>one</b> email with attachments to multiple recipient.
	 *
	 * @param to			array containing the email addresses of the recipients
	 * @param subject		the subject of the email
	 * @param template		name of the Thymeleaf template to use for constructing the message body
	 * @param vars			map containing the variables to be used in template processing
	 * @param att			collection of {@link Attachment} objects representing the attachments to include
	 * @throws MailException when an error occurs preparing or sending the message
	 */
	public void sendMail(final String[] to, final String subject, final String template,
						 final Map<String, Object> vars, final Collection<Attachment> att) throws MailException {
		sendMail(to, null, subject, template, vars, att);
	}
	
	/**
	 * Sends <b>one</b> email with attachments to multiple recipient.
	 *
	 * @param to			array containing the email addresses of the recipients
	 * @param from			email address of the sender
	 * @param subject		the subject of the email
	 * @param template		name of the Thymeleaf template to use for constructing the message body
	 * @param vars			map containing the variables to be used in template processing
	 * @param att			collection of {@link Attachment} objects representing the attachments to include
	 * @throws MailException when an error occurs preparing or sending the message
	 */
	public void sendMail(final String[] to, final String from, final String subject, final String template,
						 final Map<String, Object> vars, final Collection<Attachment> att) throws MailException {
		if (to == null || to.length == 0)
			throw new IllegalArgumentException("At least one recipient must be specified");
		if (Utils.isNullOrEmpty(subject))
			throw new IllegalArgumentException("A subject must be specified");
		if (Utils.isNullOrEmpty(template))
			throw new IllegalArgumentException("A template must be specified");
		if (!isAvailable()) {
			log.error("Cannot send email as mail server is not configured");
			throw new MailSendException("Cannot send email as mail server is not configured");						
		}

		final boolean isMultipart = !Utils.isNullOrEmpty(att);
		MimeMessage mimeMessage;
		try {
			log.trace("Create message");
			mimeMessage = mailer.createMimeMessage();
			MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, "UTF-8");
			message.setSubject(subject);
			message.setFrom(!Utils.isNullOrEmpty(from) ? from : sender);

			for(String r : to)
				message.addTo(r);

			log.trace("Create Thymeleaf context to process template");
			final Context ctx = new Context();
			// Set the Thymeleaf evaluation context to allow access to Spring beans with @beanName in SpEL expressions
	        ctx.setVariable(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,
	                		new ThymeleafEvaluationContext(appContext, null));

	        // Set additional variables
			if (!Utils.isNullOrEmpty(vars))
				vars.forEach((k, v) -> ctx.setVariable(k, v));

			message.setText(thEngine.process(template, ctx));

			if (isMultipart) {
				log.trace("Add attachments to message");
				for(Attachment a : att) {
					if (a.inline)
						message.addInline(a.reference, a.content);
					else
						message.addAttachment(a.reference, a.content);
				}
			}
		} catch (MessagingException | TemplateEngineException mailFailure) {
			log.error("An error occurred preparing or sending email message (S={}) : {}", subject,
					Utils.getExceptionTrace(mailFailure));
			throw new MailPreparationException("Could not create message to send", mailFailure);
		}
		log.trace("Send message");
		mailer.send(mimeMessage);
		log.debug("Sent email (S={}) to {} recipient(s)", subject, to.length);
	}

	public MailSenderUtil() {
        thEngine = new SpringTemplateEngine();

        final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setOrder(Integer.valueOf(1));
        templateResolver.setResolvablePatterns(Collections.singleton("text/*"));
        templateResolver.setPrefix("/mail/");
        templateResolver.setSuffix(".txt");
        templateResolver.setTemplateMode(TemplateMode.TEXT);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);

        thEngine.addTemplateResolver(templateResolver);
    }
}
