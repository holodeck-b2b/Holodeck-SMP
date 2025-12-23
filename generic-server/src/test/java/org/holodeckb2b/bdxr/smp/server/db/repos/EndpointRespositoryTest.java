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
package org.holodeckb2b.bdxr.smp.server.db.repos;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.Random;

import org.holodeckb2b.bdxr.smp.server.CommonServerConfig;
import org.holodeckb2b.bdxr.smp.server.db.entities.EmbeddedIdentifier;
import org.holodeckb2b.bdxr.smp.server.db.entities.EndpointEntity;
import org.holodeckb2b.bdxr.smp.server.db.entities.TransportProfileEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ContextConfiguration(classes = { CommonServerConfig.class })
public class EndpointRespositoryTest {

	@Autowired
	TransportProfileRepository	tpRepo;
	
	@Autowired
	EndpointRepository epRepo;	
	
	@Test
	void testCountAndFindByProfile() {
		TransportProfileEntity tp1 = new TransportProfileEntity();
		tp1.setId(new EmbeddedIdentifier("test-transport-profile-" + System.nanoTime()));
		tpRepo.save(tp1);
		TransportProfileEntity tp2 = new TransportProfileEntity();
		tp2.setId(new EmbeddedIdentifier("test-transport-profile-" + System.nanoTime()));
		tpRepo.save(tp2);
		
		int ctp1 = 0, ctp2 = 0;
		Random random = new Random();
		for(int i = 0; i < 100; i++) {
			EndpointEntity ep = new EndpointEntity();
			ep.setEndpointURL(assertDoesNotThrow(() -> new URL("http://test.holodeck-smp.org")));
			if (random.nextBoolean()) {
				ep.setTransportProfile(tp1);
				ctp1++;
			} else {
				ep.setTransportProfile(tp2);
				ctp2++;
			}
			epRepo.save(ep);
		}
			
		assertEquals(ctp1, assertDoesNotThrow(() -> epRepo.countByTransportProfile(tp1)));
		assertEquals(ctp1, assertDoesNotThrow(() -> epRepo.findByTransportProfile(tp1)).size());
		assertEquals(ctp2, assertDoesNotThrow(() -> epRepo.countByTransportProfile(tp2)));
		assertEquals(ctp2, assertDoesNotThrow(() -> epRepo.findByTransportProfile(tp2)).size());
	}
}
