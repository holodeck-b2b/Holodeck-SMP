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
package org.holodeckb2b.bdxr.smp.server.queryapi.oasisv2;

import java.lang.reflect.Method;
import java.time.ZonedDateTime;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMResult;

import org.holodeckb2b.bdxr.smp.datamodel.IDScheme;
import org.holodeckb2b.bdxr.smp.datamodel.Identifier;
import org.holodeckb2b.commons.util.Utils;
import org.oasis_open.docs.bdxr.ns.smp._2.basiccomponents.SMPVersionIDType;
import org.oasis_open.docs.bdxr.ns.smp._2.servicegroup.ServiceGroupType;
import org.oasis_open.docs.bdxr.ns.smp._2.servicemetadata.ServiceMetadataType;
import org.oasis_open.docs.bdxr.ns.smp._2.unqualifieddatatypes.IdentifierType;
import org.w3c.dom.Document;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;

/**
 * Is the abstract base class for creating the XML response documents as specified in the OASIS SMP v2 specification. It
 * supplies some utility functions to construct the XML documents.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
abstract class AbstractResponseFactory {
	protected static final SMPVersionIDType SMP_VERSION_ID;

	private final static QName ServiceMetadata_QNAME = new QName("http://docs.oasis-open.org/bdxr/ns/SMP/2/ServiceMetadata", "ServiceMetadata");
	private final static QName ServiceGroup_QNAME = new QName("http://docs.oasis-open.org/bdxr/ns/SMP/2/ServiceGroup", "ServiceGroup");

	protected static final DatatypeFactory DTF;
	protected static final JAXBContext JAXB_CTX;

	static {
		SMP_VERSION_ID = new SMPVersionIDType();
		SMP_VERSION_ID.setValue("2.0");
		try {
			JAXB_CTX = JAXBContext.newInstance(ServiceMetadataType.class, ServiceGroupType.class);
		} catch (JAXBException ex) {
			throw new RuntimeException("Failed to initialise JAXBContext", ex);
		}
		try {
			DTF = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException ex) {
			throw new RuntimeException("Cannot initialize DatatypeFactory instance!", ex);
		}
	}

	/**
	 * Converts the given Identifier from object to JAXB representation.
	 *
	 * @param id	identifier to convert
	 * @param cls	the JAXB type to convert to, must be a descendant of {@link IdentifierType}
	 * @return		the JAXB object representation of the identifier
	 * @throws InstantiationException if the JAXB object cannot be created
	 */
	protected <T extends IdentifierType> T convertID(Identifier id, Class<T> cls) throws InstantiationException {
		IdentifierType xmlID;
		try {
			xmlID = cls.getDeclaredConstructor().newInstance();
		} catch (Exception ex) {
			throw new InstantiationException("Could not create instance of " + cls.getName());
		}
		IDScheme s = id.getScheme();
		if (s != null)
			xmlID.setSchemeID(s.getSchemeId());
		xmlID.setValue(id.getValue());
		return (T) xmlID;
	}

	/**
	 * Creates a new JAXB content object of the given type and sets its value to the specified text.
	 *
	 * @param value	the text to set as content
	 * @param cls	the JAXB content type class
	 * @throws InstantiationException when the object could not be created
	 */
	protected <T> T createTextContent(String value, Class<T> cls) throws InstantiationException {
		if (Utils.isNullOrEmpty(value))
			return null;
		try {
			T o = cls.getDeclaredConstructor().newInstance();
			Method m = cls.getMethod("setValue", String.class);
			m.invoke(o, value);
			return o;
		} catch (Exception ex) {
			throw new InstantiationException("Could not create text content of type " + cls.getName());
		}
	}

	/**
	 * Creates a new JAXB content object of the given type and sets its value to the specified date.
	 *
	 * @param value	datetime to take the date from
	 * @param cls	the JAXB content type class
	 * @throws InstantiationException when the object could not be created
	 */
	protected <T> T createDateContent(ZonedDateTime value, Class<T> cls) throws InstantiationException {
		if (value == null)
			return null;
		
		XMLGregorianCalendar d = DTF.newXMLGregorianCalendarDate(value.getYear(), value.getMonthValue(),
																 value.getDayOfMonth(),
																 value.getOffset().getTotalSeconds()/60);
		try {
			T o = cls.getDeclaredConstructor().newInstance();
			Method m = cls.getMethod("setValue", XMLGregorianCalendar.class);
			m.invoke(o, d);
			return o;
		} catch (Exception ex) {
			throw new InstantiationException("Could not create date content of type " + cls.getName());
		}
	}

	/**
	 * Converts the given JAXB representation of the response to the DOM model representation.
	 *
	 * @param <T>	the JAXB type of the data, must be either {@link ServiceMetadataType} or {@link ServiceGroupType}
	 * @param data	the response data
	 * @return		the DOM object representation for the response
	 * @throws InstantiationException	when the given data cannot be converted to the DOM representation
	 */
	protected <T> Document jaxb2dom(T data) throws InstantiationException {
		JAXBElement e;
		if (data instanceof ServiceMetadataType)
			e = new JAXBElement(ServiceMetadata_QNAME, ServiceMetadataType.class, data);
		else if (data instanceof ServiceGroupType)
			e = new JAXBElement(ServiceGroup_QNAME, ServiceGroupType.class, data);
		else
			throw new IllegalArgumentException("Unsupported data type " + data.getClass().getName());

		try {
			DOMResult res = new DOMResult();
			JAXB_CTX.createMarshaller().marshal(e, res);
			return (Document) res.getNode();
		} catch (JAXBException ex) {
			throw new InstantiationError("Could not convert from JAXB to DOM representation");
		}
	}

}
