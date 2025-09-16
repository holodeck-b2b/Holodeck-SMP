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
package org.holodeckb2b.bdxr.smp.server.db.entities;

import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.holodeckb2b.bdxr.common.datamodel.Extension;
import org.holodeckb2b.bdxr.smp.datamodel.RedirectionV2;
import org.holodeckb2b.bdxr.smp.server.datamodel.Endpoint;
import org.holodeckb2b.bdxr.smp.server.datamodel.ProcessGroup;
import org.holodeckb2b.bdxr.smp.server.datamodel.ProcessInfo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Defines the JPA class for storing the {@link ProcessGroup} meta-data in the database. As a Process Group is always
 * part of a Service Metadata Template it is defined as an <code>Embeddable</code>.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity(name = "ProcessGroup")
@Getter
@NoArgsConstructor
public class ProcessGroupEntity implements ProcessGroup {
	private static final long serialVersionUID = 4890023156219630890L;

	@Id
	@GeneratedValue
	protected Long	oid;
	
	@ManyToOne
	protected ServiceMetadataTemplateEntity template;

	@OneToMany(mappedBy = "procgroup", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	protected List<ProcessInfoEntity>	processInfo = new ArrayList<>();

	@ManyToMany
	@JoinTable(name = "PROCESS_GROUP_ENDPOINTS")
	protected List<EndpointEntity>	endpoints = new ArrayList<>();

	@Embedded
	protected EmbeddedRedirection		redirection;

	public ProcessGroupEntity(ServiceMetadataTemplateEntity t, org.holodeckb2b.bdxr.smp.datamodel.ProcessGroup src) {
		this.template = t;
		if (src.getEndpoints() != null && !src.getEndpoints().parallelStream()
							.allMatch(ep -> (ep instanceof EndpointEntity) && ((EndpointEntity) ep).getOid() != null))
			throw new IllegalArgumentException("Not all Endpoint instances are managed");
		else if (src.getEndpoints() != null)
			src.getEndpoints().forEach(ep -> addEndpoint((Endpoint) ep));
		
		if (src.getProcessInfo() != null && !src.getProcessInfo().parallelStream()
							.allMatch(pi -> (pi instanceof ProcessInfo)))
			throw new IllegalArgumentException("Incorrect ProcessInfo type found");		
		else if (src.getProcessInfo() != null)
			src.getProcessInfo().forEach(pi -> addProcessInfo((ProcessInfo) pi));
		
		if (src.getRedirection() != null)
			if (src.getRedirection() instanceof RedirectionV2)
				setRedirection((RedirectionV2) src.getRedirection());
			else 
				throw new IllegalArgumentException("Incorrect Redirection type - " + 
													src.getRedirection().getClass().getName());				
	}

	@Override
	public void addProcessInfo(ProcessInfo pi) {
		processInfo.add(new ProcessInfoEntity(this, pi));
	}
	
	@Override
	public void removeProcessInfo(ProcessInfo pi) {
		processInfo.remove(pi);		
	}
	
	@Override
	public void addEndpoint(Endpoint ep) {
		if (!(ep instanceof EndpointEntity) || ((EndpointEntity) ep).getOid() == null)
			throw new IllegalArgumentException("Endpoint instance is not managed");
		endpoints.add((EndpointEntity) ep);
	}
	
	@Override
	public void removeEndpoint(Endpoint ep) {
		endpoints.remove(ep);	
	}
	
	@Override
	public void setRedirection(RedirectionV2 r) {
		EmbeddedRedirection er = new EmbeddedRedirection();
		er.setRedirectionURL(r.getNewSMPURL());
		try {
			er.setSMPCertificate(r.getSMPCertificate());
		} catch (CertificateEncodingException e) {
			throw new IllegalArgumentException("Invalid X509 Certificate", e);
		}
		this.redirection = er;
	}
	
	@Override
	public void removeRedirection() {
		redirection = null;		
	}
	
	@Override
	public List<Extension<?>> getExtensions() {
		return null;
	}
}
