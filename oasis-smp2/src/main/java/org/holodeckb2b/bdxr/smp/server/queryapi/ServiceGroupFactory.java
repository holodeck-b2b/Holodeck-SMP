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
package org.holodeckb2b.bdxr.smp.server.queryapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessGroup;
import org.holodeckb2b.bdxr.smp.datamodel.ProcessInfo;
import org.holodeckb2b.bdxr.smp.datamodel.util.Comparator;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataBinding;
import org.holodeckb2b.bdxr.smp.server.datamodel.ServiceMetadataTemplate;
import org.oasis_open.docs.bdxr.ns.smp._2.aggregatecomponents.ProcessType;
import org.oasis_open.docs.bdxr.ns.smp._2.aggregatecomponents.ServiceReferenceType;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.IDType;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.ParticipantIDType;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.RoleIDType;
import org.oasis_open.docs.bdxr.ns.smp._2.servicegroup.ServiceGroupType;
import org.w3c.dom.Document;

/**
 * Is a factory for <code>ServiceGroup</code> XML documents as specified by the OASIS SMP V2 Specification.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Slf4j
public class ServiceGroupFactory extends AbstractResponseFactory {

	/**
	 * Creates a XML Document with <code>ServiceGroup</code> root element as defined by the OASIS SMP2 Specification
	 * using the metadata from the given ServiceMetadata Bindings.
	 *
	 * @param smb	the collection of Servicemetadata Bindings to use
	 * @return	new XML Document containing the <code>ServiceGroup</code>
	 */
	Document newResponse(List<? extends ServiceMetadataBinding> smb) throws InstantiationException {
		ServiceGroupType sg = new ServiceGroupType();
		sg.setSMPVersionID(SMP_VERSION_ID);
		sg.setParticipantID(convertID(smb.get(0).getParticipantId(), ParticipantIDType.class));

		for(ServiceMetadataBinding b : smb)
			sg.getServiceReference().add(createServiceReference(b.getTemplate()));

		return jaxb2dom(sg);
	}

	private ServiceReferenceType createServiceReference(ServiceMetadataTemplate t) throws InstantiationException {
		ServiceReferenceType r = new ServiceReferenceType();
		r.setID(convertID(t.getServiceId(), IDType.class));
		Collection<? extends ProcessGroup> pg = t.getProcessMetadata();
		Collection<ProcessInfo> procs = new ArrayList<>();
		// Only add unique Process element, i.e. that represent the same Process and collection of Roles
		pg.forEach(g -> procs.addAll(g.getProcessInfo().parallelStream()
													   .filter(p1 -> procs.parallelStream()
																.noneMatch(p2 -> Comparator.equalProcessInfos(p1, p2)))
													   .toList()));
		for(ProcessInfo p : procs)
			r.getProcess().add(createProcess(p));
		return r;
	}

	private ProcessType createProcess(ProcessInfo pi) throws InstantiationException {
		ProcessType p = new ProcessType();
		IDType pid;
		if (pi.getProcessId().isNoProcess()) {
			pid = new IDType();
			pid.setValue("bdx:noprocess");
		} else
			pid = convertID(pi.getProcessId(), IDType.class);
		p.setID(pid);
		for(Identifier r : pi.getRoles())
			p.getRoleID().add(convertID(r, RoleIDType.class));
		return p;
	}
}
