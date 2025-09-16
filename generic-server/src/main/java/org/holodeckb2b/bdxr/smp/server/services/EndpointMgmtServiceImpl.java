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
package org.holodeckb2b.bdxr.smp.server.services;

import java.security.cert.X509Certificate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.holodeckb2b.bdxr.smp.datamodel.Certificate;
import org.holodeckb2b.bdxr.smp.server.datamodel.Endpoint;
import org.holodeckb2b.bdxr.smp.server.datamodel.TransportProfile;
import org.holodeckb2b.bdxr.smp.server.db.entities.EndpointEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.TransportProfileEntity;
import org.holodeckb2b.bdxr.smp.server.db.repos.EndpointRepository;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException;
import org.holodeckb2b.bdxr.smp.server.services.core.ConstraintViolationException.ViolationType;
import org.holodeckb2b.bdxr.smp.server.services.core.EndpointMgmtService;
import org.holodeckb2b.bdxr.smp.server.services.core.PersistenceException;
import org.holodeckb2b.commons.security.CertificateUtils;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Implements the {@link EndpointMgmtService}
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Slf4j
@Service
public class EndpointMgmtServiceImpl extends BaseMgmtServiceImpl<Long, EndpointEntity, EndpointRepository> 
										implements EndpointMgmtService {

	
	@Override
	protected String getAuditDetails(EndpointEntity entity) {
		StringBuilder sb = new StringBuilder("URL=");
		if (entity.getUrl() != null)
			sb.append(entity.getUrl());
		sb.append(",tpID=").append(entity.getTransportProfile().getId().toString())
		  .append(",actDate=");
		if (entity.getServiceActivationDate() != null)
			sb.append(String.format("%tFT%<tT%<tz", entity.getServiceActivationDate()));
		sb.append(",expDate=");
		if (entity.getServiceExpirationDate() != null)
			sb.append(String.format("%tFT%<tT%<tz", entity.getServiceExpirationDate()));
		sb.append(",certs=[").append(getCertDetails(entity.getCertificates())).append(']');
		return sb.toString();
	}

	private String getCertDetails(List<? extends Certificate> certificates) {
		if (Utils.isNullOrEmpty(certificates))
			return "";
		
		StringBuilder sb = new StringBuilder('{');
		for (Iterator<? extends Certificate> it = certificates.iterator(); it.hasNext(); ) {
			Certificate cert = it.next();		
			X509Certificate c = cert.getX509Cert();
			sb.append("subject=").append(CertificateUtils.getIssuerName(c))
			  .append(",issuer/serialNo=").append(CertificateUtils.getIssuerName(c))
			  							  .append('/').append(c.getSerialNumber().toString())
			  .append(",usage=").append(cert.getUsage())
			  .append(",from(");
			ZonedDateTime from;
			if (cert.getActivationDate() != null) {
				sb.append('e');
				from = cert.getActivationDate();
			} else {
				sb.append('c');
				from = ZonedDateTime.from(c.getNotBefore().toInstant().atZone(ZoneOffset.UTC));
			}
			sb.append(')').append(String.format("%tFT%<tT%<tz", from)).append(",to(");
			ZonedDateTime to;
			if (cert.getExpirationDate() != null) {
				sb.append('e');
				to = cert.getExpirationDate();
			} else {
				sb.append('c');
				to = ZonedDateTime.from(c.getNotAfter().toInstant().atZone(ZoneOffset.UTC));
			}
			sb.append(')').append(String.format("%tFT%<tT%<tz", to)).append('}');
			if (it.hasNext())
				sb.append(",");
		}
		return sb.toString();
	}

	@Override
	public Endpoint addEndpoint(UserDetails user, Endpoint ep) throws PersistenceException {
		if (ep.getTransportProfile() == null) {
			log.warn("New Endpoint meta-data does not contain Transport Profile");
			throw new ConstraintViolationException(ViolationType.MISSING_MANDATORY_FIELD, ep, "TransportProfile");
		} 
		EndpointEntity entity = new EndpointEntity();
		try {
			entity.setTransportProfile(ep.getTransportProfile());
		} catch (IllegalArgumentException unmanagedTP) {
			log.warn("New Endpoint meta-data does contain unmanaged TransportProfile instance");
			throw new ConstraintViolationException(ViolationType.INVALID_FIELD, ep, "TransportProfile");
		}		
		entity.setEndpointURL(ep.getEndpointURL());
		checkActivationDates(ep);		
		entity.setServiceActivationDate(ep.getServiceActivationDate());
		entity.setServiceExpirationDate(ep.getServiceExpirationDate());
		entity.setContactInfo(ep.getContactInfo());
		entity.setDescription(ep.getDescription());
		entity.setName(ep.getName());
		
		if (!Utils.isNullOrEmpty(ep.getCertificates()))
			for(Certificate c : ep.getCertificates()) {
				try {
					checkCertificate(c, ep);				
				} catch (ConstraintViolationException cve) {
					log.warn("New Endpoint meta-data contains invalid Certificate instance : {}", cve.getMessage());
					throw cve;
				}
				entity.addCertificate(c);
			}
		
		return executeCRUD(CrudOps.Add, user, entity);
	}


	@Override
	public Endpoint updateEndpoint(UserDetails user, Endpoint ep) throws PersistenceException {
		checkManaged(ep);
		checkActivationDates(ep);
		for(Certificate c : ep.getCertificates()) 
			try {
				checkCertificate(c, ep);				
			} catch (ConstraintViolationException cve) {
				log.warn("Endpoint meta-data contains invalid Certificate instance : {}", cve.getMessage());
				throw cve;
			}
		
		return executeCRUD(CrudOps.Update, user, (EndpointEntity) ep);
	}

	@Override
	public void deleteEndpoint(UserDetails user, Endpoint ep) throws PersistenceException {
		executeCRUD(CrudOps.Delete, user, checkManaged(ep));
	}

	@Override
	public Collection<? extends Endpoint> getEndpoints() throws PersistenceException {
		return getAll();
	}
	
	@Override
	public Page<EndpointEntity> getEndpoints(PageRequest request) throws PersistenceException {
		return retrieveSubset(request);
	}

	@Override
	public Endpoint getEndpoint(Long id) throws PersistenceException {
		log.trace("Retrieving Endpoint with ID {}", id);
		return repo.findById(id).orElse(null);
	}
	
	@Override
	public int countEndpointsUsingProfile(TransportProfile profile) throws PersistenceException {
		log.trace("Count all Endpoints using Transport Profile with ID {}", profile.getId());
		return repo.countByTransportProfile((TransportProfileEntity) profile);
	}

	@Override
	public Collection<EndpointEntity> findEndpointsUsingProfile(TransportProfile profile) throws PersistenceException {
		log.trace("Retrieving all Endpoints using Transport Profile with ID {}", profile.getId());
		return repo.findByTransportProfile((TransportProfileEntity) profile);
	}

	/**
	 * Checks the constraints on the activation and expiration dates of the Endpoint.
	 * 
	 * @param ep	the Endpoint which dates should be checked 
	 * @throws ConstraintViolationException when a constraint is violated
	 */
	private void checkActivationDates(Endpoint ep) throws ConstraintViolationException {
		ZonedDateTime activationDate = ep.getServiceActivationDate();
		ZonedDateTime expirationDate = ep.getServiceExpirationDate();
		if (activationDate != null && expirationDate != null && activationDate.isAfter(expirationDate))
			throw new ConstraintViolationException(ViolationType.INVALID_FIELD, ep, "Invalid active period");
	}
	
	/**
	 * Checks that the given Certificate meta-data contains a X509.v3 certificate and has a valid usage period.
	 * 
	 * @param c		the Certificate meta-data to check
	 * @param ep	the Endpoint registration in which the Certificate is contained
	 * @throws ConstraintViolationException when a constraint is violated
	 */
	private void checkCertificate(Certificate c, Endpoint ep) throws ConstraintViolationException {
		X509Certificate x509Cert = c.getX509Cert();
		if (x509Cert == null) 
			throw new ConstraintViolationException(ViolationType.MISSING_MANDATORY_FIELD, ep, "X509Cert");		
		ZonedDateTime activationDate = c.getActivationDate();
		ZonedDateTime expirationDate = c.getExpirationDate();
		if (activationDate != null 
			&& activationDate.isAfter(ZonedDateTime.from(x509Cert.getNotAfter().toInstant().atZone(ZoneOffset.UTC))))
			throw new ConstraintViolationException(ViolationType.INVALID_FIELD, ep, "Activation after X509 cert validity");
		if (expirationDate != null 
			&& expirationDate.isBefore(ZonedDateTime.from(x509Cert.getNotBefore().toInstant().atZone(ZoneOffset.UTC))))
			throw new ConstraintViolationException(ViolationType.INVALID_FIELD, ep, "Expiration before X509 cert validity");
		if (activationDate != null && expirationDate != null && !activationDate.isBefore(expirationDate))
			throw new ConstraintViolationException(ViolationType.INVALID_FIELD, ep, "Activation after expiration");		
	}
}
