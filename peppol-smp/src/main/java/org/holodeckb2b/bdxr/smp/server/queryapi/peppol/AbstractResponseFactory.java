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
package org.holodeckb2b.bdxr.smp.server.queryapi.peppol;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMResult;

import org.busdox.servicemetadata.publishing._1.ServiceGroupType;
import org.busdox.servicemetadata.publishing._1.ServiceMetadataType;
import org.busdox.servicemetadata.publishing._1.SignedServiceMetadataType;
import org.w3c.dom.Document;

import eu.peppol.schema.pd.businesscard._20180621.BusinessCardType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;

/**
 * Is the abstract base class for creating the XML response documents as specified in the PEPPOL SMP specification. It
 * supplies some utility functions to construct the XML documents.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 2.1.0 support for converting BusinessCardType JAXB object to DOM
 */
abstract class AbstractResponseFactory {
	private final static QName SignedServiceMetadata_QNAME = new QName("http://busdox.org/serviceMetadata/publishing/1.0/", "SignedServiceMetadata");
	private final static QName ServiceGroup_QNAME = new QName("http://busdox.org/serviceMetadata/publishing/1.0/", "ServiceGroup");
	private final static QName BusinessCard_QNAME = new QName("http://www.peppol.eu/schema/pd/businesscard/20180621/", "BusinessCard");

	protected static final DatatypeFactory DTF;
	protected static final JAXBContext JAXB_CTX;

	static {
		try {
			JAXB_CTX = JAXBContext.newInstance(SignedServiceMetadataType.class, ServiceGroupType.class, BusinessCardType.class);
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
	 * Converts the given date time to a <code>XMLGregorianCalendar</code> object that can be used as content of a 
	 * JAXB element of xsd:dateTime type.
	 *
	 * @param value		date time to convert
	 * @return {@link XMLGregorianCalendar} instance representing the given date time
	 */
	protected XMLGregorianCalendar createDateTimeContent(ZonedDateTime value) {
		if (value == null)
			return null;

		return DTF.newXMLGregorianCalendar(value.getYear(), value.getMonthValue(), value.getDayOfMonth(),
										   value.getHour(), value.getMinute(), 0, 0, value.getOffset().getTotalSeconds()/60);
	}

	/**
	 * Converts the given date to a <code>XMLGregorianCalendar</code> object that can be used as content of a 
	 * JAXB element of xsd:date type.
	 *
	 * @param value		date to convert
	 * @return {@link XMLGregorianCalendar} instance representing the given date
	 * @since 2.1.0
	 */
	protected XMLGregorianCalendar createDateContent(LocalDate value) {
		if (value == null)
			return null;
		
		return DTF.newXMLGregorianCalendarDate(value.getYear(), value.getMonthValue(), value.getDayOfMonth(), 
												DatatypeConstants.FIELD_UNDEFINED);
	}
	
	/**
	 * Converts the given JAXB representation of the response to the DOM model representation.
	 *
	 * @param <T>	the JAXB type of the data, must be either {@link ServiceMetadataType} or {@link ServiceGroupType}
	 * @param data	the response data
	 * @return		the DOM object representation for the response
	 * @throws InstantiationException	when the given data cannot be converted to the DOM representation
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected <T> Document jaxb2dom(T data) throws InstantiationException {
		JAXBElement e;
		if (data instanceof SignedServiceMetadataType)
			e = new JAXBElement(SignedServiceMetadata_QNAME, SignedServiceMetadataType.class, data);
		else if (data instanceof ServiceGroupType)
			e = new JAXBElement(ServiceGroup_QNAME, ServiceGroupType.class, data);
		else if (data instanceof BusinessCardType)
			e = new JAXBElement(BusinessCard_QNAME, BusinessCardType.class, data);
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
