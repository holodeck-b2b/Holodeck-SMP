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

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.holodeckb2b.bdxr.smp.datamodel.impl.IDSchemeImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.IdentifierImpl;
import org.holodeckb2b.bdxr.smp.server.datamodel.Contact;
import org.holodeckb2b.bdxr.smp.server.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.server.datamodel.Participant;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataTemplate;
import org.holodeckb2b.commons.util.Utils;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Defines the meta-data maintained by Holodeck SMP on a <i>Participant</i> in the network.
 * <p> 
 * The only required information element on a <i>Participant</i> is the identifier under which the <i>Participant</i> is
 * known in the network and which is used for querying the capabilities. When a directory service is available in the
 * network additional identifiers used by the <i>Participant</i> may be published to it, together with the other 
 * meta-data. These however cannot be used for querying. If that should be possible multiple registrations of the 
 * <i>Participant</i> must be created for each identifier that can be used for querying.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity(name = "Participant")
@Table(indexes = {
		@Index(name = "IDX_PART_ID", columnList = "idvalue"),
		@Index(name = "IDX_PART_NAME", columnList = "name"),
		@Index(name = "IDX_PART_LCNAME", columnList = "lcname")
})
@NamedQueries({
	@NamedQuery(name = "Participant.findByAdditionalId", 
				query = "SELECT p FROM Participant p WHERE locate(:additionalId, p.additionalIds,0) > 0"),
	@NamedQuery(name = "Participant.countByAdditionalId", 
	query = "SELECT count(p) FROM Participant p WHERE locate(:additionalId, p.additionalIds,0) > 0")
})
@NoArgsConstructor
public class ParticipantEntity extends AbstractIdBasedEntity<Identifier, EmbeddedIdentifier> implements Participant {
	
	@Column(name = "LCNAME")
	protected String	lcName;
	
	@Column(name = "country", length=2)
	@Getter
	@Setter
	protected String	registrationCountry;

	@Embedded
	@AttributeOverride(name = "name", column = @Column(name = "contactName"))
	protected EmbeddedContact	contactInfo;
	
	@Column(length=1024)
	@Getter
	@Setter
	protected String	locationInfo;
	
	@Column
	protected String	websiteURL;
	
	@Column(name = "firstRegistration")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	@Getter
	@Setter
	protected LocalDate firstRegistrationDate;
	
	@Column(length=1024)
	protected String	additionalIds;

	@Transient
	private Set<org.holodeckb2b.bdxr.smp.datamodel.Identifier> additionalIdsSet;
	
	@Column
	@Setter
	protected Boolean	registeredInSML;

	@Column
	@Setter
	protected Boolean	publishedInDirectory;
	
	@Column
	@Getter
	@Setter
	protected String	SMLMigrationCode;

	@Getter
	@Setter
	@ManyToMany(targetEntity = ServiceMetadataTemplateEntity.class, fetch = FetchType.EAGER)
	@JoinTable(name = "SERVICE_METADATA_BINDING", 		
		joinColumns =  @JoinColumn(name = "PARTICIPANT_OID", nullable = false), 
		inverseJoinColumns = @JoinColumn(name = "TEMPLATE_OID", nullable = false),
		indexes = { @Index(name = "IDX_BINDINGS_PARTICIPANT", columnList = "PARTICIPANT_OID"),
					@Index(name = "IDX_BINDINGS_TEMPLATE", columnList = "TEMPLATE_OID")
				}
	)
	protected Collection<ServiceMetadataTemplate>	bindings = new ArrayList<>();
			
	@PrePersist 
	@PreUpdate 
	private void prepare(){
        this.lcName = name == null ? null : name.toLowerCase();
    }
	
	protected ParticipantEntity(ParticipantEntity source) {
		super();
		if (source.getId() != null)
			setId(source.getId());
		this.oid = source.oid;		
		this.name = source.name;
		this.lcName = source.lcName;
		this.registrationCountry = source.registrationCountry;
		this.contactInfo = source.contactInfo;
		this.locationInfo = source.locationInfo;
		this.websiteURL = source.websiteURL;
		this.firstRegistrationDate = source.firstRegistrationDate;
		this.additionalIds = source.additionalIds;
		this.additionalIdsSet = source.additionalIdsSet;
		this.registeredInSML = source.registeredInSML;
		this.publishedInDirectory = source.publishedInDirectory;
		this.SMLMigrationCode = source.SMLMigrationCode;
		this.bindings = source.bindings;
	}
	
	protected ParticipantEntity clone() {
		return new ParticipantEntity(this);
	}
	
	@Override
	public String getAuditLogId() {
		return getId().toString();
	}
	
	@Override
	public boolean isRegisteredInSML() {
		return registeredInSML != null && registeredInSML;
	}
	
	@Override
	public boolean isPublishedInDirectory() {
		return publishedInDirectory != null && publishedInDirectory;
	}
	
	@Override
	public Collection<ServiceMetadataTemplate> getBoundSMT() {
		return Collections.unmodifiableCollection(bindings);
	}

	/**
	 * Adds a binding of the given <i>Service Metadata Template</i> to the <i>Participant</i>.
	 * 
	 * @param smt	the entity object representing the Service Metadata Template to bind to the Participant
	 */
	public void addBinding(ServiceMetadataTemplateEntity smt) {
		bindings.add(smt);
	}

	/**
	 * Removes the binding of the given <i>Service Metadata Template</i> from the <i>Participant</i>.
	 * 
	 * @param smt	the entity object representing the Service Metadata Template to remove from the Participant
	 */
	public void removeBinding(ServiceMetadataTemplateEntity smt) {
		bindings.remove(smt);
	}
	
	@Override
	public Set<Contact> getContactInfo() {
		return contactInfo != null ? Set.of(contactInfo) : Collections.emptySet();
	}
	
	@Override
	public void addContactInfo(Contact contact) {
		if (contact == null)
			throw new IllegalArgumentException("Contact must not be null");
		
		this.contactInfo = new EmbeddedContact(contact.getName(), 
											   contact.getJobTitle(), 
											   contact.getDepartment(), 
											   contact.getEmailAddress(), 
											   contact.getTelephone());
	}
	
	@Override
	public void removeContactInfo(Contact contact) {
		if (this.contactInfo.equals(contact))
			this.contactInfo = null;		
	}
	
	@Override
	public Set<URL> getWebsites() {
		try {
			return Utils.isNullOrEmpty(websiteURL) ? Collections.emptySet() : Set.of(new URL(websiteURL));
		} catch (MalformedURLException e) {
			throw new IllegalStateException("Invalid website URL (" + websiteURL + ") for ParticipantID=" 
											+ getId().toString());
		}
	}
	
	@Override
	public void addWebsite(URL url) {
		if (url == null)
			throw new IllegalArgumentException("Website URL must not be null");		
		this.websiteURL = url.toString();
	}
	
	@Override
	public void removeWebsite(URL url) {
		if (url != null && this.websiteURL.equals(url.toString()))
			this.websiteURL = null;		
	}

	@Override
	public Set<org.holodeckb2b.bdxr.smp.datamodel.Identifier> getAdditionalIds() {
		if (additionalIdsSet == null) {
			if (Utils.isNullOrEmpty(additionalIds))
				additionalIdsSet = new HashSet<>();
			else {
				String[] ids = additionalIds.split(",");
				additionalIdsSet = new HashSet<>(ids.length);
				for (String id : ids) {
					int sep = id.indexOf("::");
					String sid = id.substring(0, Math.max(0, sep));
					String val = sep < 0 ? id: id.substring(sep + 2);					
					additionalIdsSet.add(new IdentifierImpl(val, Utils.isNullOrEmpty(sid) ? null 
															: new IDSchemeImpl(sid, !val.equals(val.toLowerCase()))));
				}
			}
		}		
		return Collections.unmodifiableSet(additionalIdsSet);
	}
	
	protected void clearAdditionalIds() {
		additionalIdsSet = new HashSet<>();
		convertAdditionalIds();
	}
		
	@Override
	public void addAdditionalId(org.holodeckb2b.bdxr.smp.datamodel.Identifier id) {
		if (additionalIdsSet == null)
			additionalIdsSet = new HashSet<>();
		additionalIdsSet.add(new IdentifierImpl(id));
		convertAdditionalIds();
	}
	
	@Override
	public void removeAdditionalId(org.holodeckb2b.bdxr.smp.datamodel.Identifier id) {
		if (additionalIdsSet != null)
			additionalIdsSet.remove(id);
		convertAdditionalIds();
	}
	
	/**
	 * Helper method to convert the in-memory <code>Set</code> representation of the additional identifiers to the
	 * concatenated string representation stored in the database.
	 */
	private void convertAdditionalIds() {
		if (Utils.isNullOrEmpty(additionalIdsSet))
			additionalIds = null;
		else
			additionalIds = additionalIdsSet.stream().map(id -> id.toString()).collect(Collectors.joining(","));
	}
}
