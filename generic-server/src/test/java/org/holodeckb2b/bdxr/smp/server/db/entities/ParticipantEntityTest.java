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
package org.holodeckb2b.bdxr.smp.server.db.entities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.time.LocalDate;

import org.holodeckb2b.bdxr.common.datamodel.Identifier;
import org.holodeckb2b.bdxr.common.datamodel.impl.IdentifierImpl;
import org.holodeckb2b.bdxr.smp.datamodel.impl.IDSchemeImpl;
import org.holodeckb2b.bdxr.smp.server.datamodel.Contact;
import org.holodeckb2b.commons.util.Utils;
import org.junit.jupiter.api.Test;

class ParticipantEntityTest extends BaseEntityTest<ParticipantEntity> {
	
	@Test
	void testSaveIdOnly() {
		ParticipantEntity pe = new ParticipantEntity();
		pe.setId(new EmbeddedIdentifier("T-PID-1"));
		
		assertDoesNotThrow(() -> save(pe));
		
		ParticipantEntity found = reload(pe);
		assertNotNull(found);
	
		assertEquals(pe.getId(), found.getId());		
		assertNull(found.getName());
		assertNull(found.getFirstRegistrationDate());
		assertNull(found.getLocationInfo());
		assertTrue(found.getContactInfo().isEmpty());
		assertTrue(found.getAdditionalIds().isEmpty());
		assertTrue(found.getWebsites().isEmpty());
		assertTrue(found.getBoundSMT().isEmpty());
	}

	@Test
	void testSaveFullBusinessCard() {
		ParticipantEntity pe = new ParticipantEntity();
		pe.setId(new EmbeddedIdentifier("T-PID-1"));
		pe.setName("My First Business");
		pe.setRegistrationCountry("XX");
		pe.setFirstRegistrationDate(LocalDate.now());
		pe.addWebsite(assertDoesNotThrow(() -> new URL("http://test.holodeck-smp.org")));
		
		EmbeddedContact contact = new EmbeddedContact("John Doe", "Director", "Management", "jdoe@first.business", "01234567890");
		pe.addContactInfo(contact);
		
		pe.addAdditionalId(new IdentifierImpl("ADDID-1"));
		pe.addAdditionalId(new IdentifierImpl("ADDID-2"));
		
		assertDoesNotThrow(() -> save(pe));
		
		ParticipantEntity found = reload(pe);
		assertNotNull(found);
		
		assertEquals(pe.getId(), found.getId());		
		assertEquals(pe.getName(), found.getName());
		assertEquals(pe.getRegistrationCountry(), found.getRegistrationCountry());
		assertEquals(pe.getFirstRegistrationDate(), found.getFirstRegistrationDate());
		assertEquals(pe.getLocationInfo(), found.getLocationInfo());
		assertEquals(pe.getContactInfo().size(), found.getContactInfo().size());
		Contact savedContact = found.getContactInfo().iterator().next();
		assertEquals(contact.getName(), savedContact.getName());
		assertEquals(contact.getJobTitle(), savedContact.getJobTitle());
		assertEquals(contact.getDepartment(), savedContact.getDepartment());
		assertEquals(contact.getEmailAddress(), savedContact.getEmailAddress());
		assertEquals(contact.getTelephone(), savedContact.getTelephone());
		
		assertEquals(pe.getAdditionalIds().size(), found.getAdditionalIds().size());
		assertTrue(pe.getAdditionalIds().parallelStream().allMatch(i -> found.getAdditionalIds().contains(i)));
		
		assertEquals(pe.getWebsites().size(), found.getWebsites().size());
		assertEquals(pe.getWebsites().iterator().next(), found.getWebsites().iterator().next());
	}

	@Test
	void testUpdateContactInfo() {
		ParticipantEntity pe = new ParticipantEntity();
		pe.setId(new EmbeddedIdentifier("T-PID-1"));
		
		EmbeddedContact contact = new EmbeddedContact("John Doe", "Director", "Management", "jdoe@first.business", "01234567890");
		pe.addContactInfo(contact);
		
		assertDoesNotThrow(() -> save(pe));
		
		ParticipantEntity found = reload(pe);
		
		final String newDept = "Board of Directors";
		found.getContactInfo().iterator().next().setDepartment(newDept);
		
		assertDoesNotThrow(() -> save(found));
		
		ParticipantEntity updated = reload(found);
		
		assertEquals(newDept, updated.getContactInfo().iterator().next().getDepartment());
	}
	
	@Test
	void testConvertAdditionalId() {
		assertEquals("addid-1", ParticipantEntity.convertAdditionalId(new IdentifierImpl("ADDID-1")));
		assertEquals("ids-1[false]::addid-1", 
						ParticipantEntity.convertAdditionalId(new IdentifierImpl("ADDID-1", "ids-1")));
		assertEquals("ids-2[true]::AddId-1", 
				ParticipantEntity.convertAdditionalId(new IdentifierImpl("AddId-1", new IDSchemeImpl("ids-2", true))));
	}
	
	@Test
	void testAddAdditionalId() {
		IDSchemeEntity ids1 = new IDSchemeEntity("T-IDS-1", true);
		IDSchemeEntity ids2 = new IDSchemeEntity("T-IDS-2", false);
		em.persist(ids1);
		em.persist(ids2);
		
		ParticipantEntity pe = new ParticipantEntity();
		pe.setId(new EmbeddedIdentifier("T-PID-1"));
		
		pe.addAdditionalId(new EmbeddedIdentifier(ids1, "ADDID-1"));
		pe.addAdditionalId(new EmbeddedIdentifier(ids1, "AddId-1"));
		pe.addAdditionalId(new EmbeddedIdentifier(ids2, "AddId-2"));
		
		assertDoesNotThrow(() -> em.persist(pe));
		
		ParticipantEntity found = em.find(ParticipantEntity.class, pe.getOid());
		
		assertTrue(Utils.areEqual(pe.getAdditionalIds(), found.getAdditionalIds()));
	}

	@Test
	void testIgnoreAddSameAdditionalId() {
		IDSchemeEntity ids = new IDSchemeEntity("T-IDS-1", false);
		em.persist(ids);
		
		ParticipantEntity pe = new ParticipantEntity();
		pe.setId(new EmbeddedIdentifier("T-PID-1"));
		
		pe.addAdditionalId(new EmbeddedIdentifier(ids, "ADDID-1"));
		pe.addAdditionalId(new EmbeddedIdentifier(ids, "AddId-1"));
		
		assertDoesNotThrow(() -> save(pe));
		
		ParticipantEntity found = reload(pe);
		
		assertEquals(1, found.getAdditionalIds().size());
	}	
	
	@Test
	void testRemoveAdditionalId() {
		ParticipantEntity pe = new ParticipantEntity();
		pe.setId(new EmbeddedIdentifier("T-PID-1"));
		pe.setName("My First Business");
		pe.setRegistrationCountry("XX");
		pe.setFirstRegistrationDate(LocalDate.now());
		pe.addWebsite(assertDoesNotThrow(() -> new URL("http://test.holodeck-smp.org")));
		
		EmbeddedContact contact = new EmbeddedContact("John Doe", "Director", "Management", "jdoe@first.business", "01234567890");
		pe.addContactInfo(contact);
		
		pe.addAdditionalId(new IdentifierImpl("ADDID-1"));
		pe.addAdditionalId(new IdentifierImpl("ADDID-2"));
		
		assertDoesNotThrow(() -> save(pe));
		
		ParticipantEntity found = reload(pe);
		
		assertEquals(2, found.getAdditionalIds().size());
		org.holodeckb2b.bdxr.common.datamodel.Identifier id2rm = found.getAdditionalIds().iterator().next();
		found.removeAdditionalId(id2rm);
		
		assertDoesNotThrow(() -> save(found));
		
		ParticipantEntity updated = reload(found);
		
		assertEquals(1, updated.getAdditionalIds().size());
		assertFalse(found.getAdditionalIds().contains(id2rm));
	}
}