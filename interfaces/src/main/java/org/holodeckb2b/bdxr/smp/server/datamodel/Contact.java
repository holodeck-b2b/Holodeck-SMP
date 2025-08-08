/*
 * Copyright (C) 2025 The Holodeck B2B Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.bdxr.smp.server.datamodel;

/**
 * Represent a contact person of a <i>Participant</i>. The data model used in Holodeck SMP is uses the semantic model
 * of a <a href="https://docs.oasis-open.org/ubl/os-UBL-2.4/mod/summary/reports/UBL-BusinessCard-2.4.html#Table-Contact.Details">
 * business party's Contact person in UBL 2.4</a>, but only uses a restricted number of information elements.
 * <p>
 * NOTE: The contact information of Participant is mainly intended for publication in the network's directory. Which
 * information elements are actually published to the directory depends on the network's directory implementation. 
 * Therefore none of the elements are mandatory, but at least one should be supplied as a contact without any 
 * information doesn't make sense.  
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface Contact {

	/**
	 * Gets the name of this contact. As this name may be published in the network's directory, it is recommended that 
	 * this be used for a functional name and not a personal name.
	 *  
	 * @return name of the contact
	 */
	String getName();
	
	/**
	 * Sets the name of the contact.
	 * 
	 * @param name	name of the contact
	 */
	void setName(String name);
	
	/**
	 * Gets the job title or function of this contact.
	 * 
	 * @return job title of the contact
	 */
	String getJobTitle();
	
	/**
	 * Sets the job title or function of this contact.
	 * 
	 * @parameter title job title of the contact
	 */
	void setJobTitle(String title);
	
	/**
	 * Gets the department where this contact works or represents.
	 * 
	 * @return department where this contact works or represents.
	 */
	String getDepartment();
	
	/**
	 * Sets the department where this contact works or represents.
	 * 
	 * @parameter department department where this contact works or represents.
	 */
	void setDepartment(String department);
	
	/**
	 * Gets the telephone number of this contact
	 * 
	 * @return contact's telephone number
	 */
	String getTelephone();
	
	/**
	 * Sets the telephone number of this contact
	 * 
	 * @parameter tel contact's telephone number
	 */
	void setTelephone(String tel);
	
	/**
	 * Gets the email address of this contact
	 * 
	 * @return contact's email address
	 */
	String getEmailAddress();

	/**
	 * Sets the email address of this contact
	 * 
	 * @parameter email contact's email address
	 */
	void setEmailAddress(String email);

}
