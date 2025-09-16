package org.holodeckb2b.bdxr.smp.server.datamodel;

/**
 * Defines the meta-data of an identifier used in meta-data registrations, e.g. <i>Service</i>, maintained by 
 * Holodeck SMP. This is the same as the definition provided in the <a 
 * href="https://github.com/holodeck-b2b/bdxr-common">BDXR Common project</a> but with the addition of <code>setter
 * </code> methods. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface Identifier extends org.holodeckb2b.bdxr.common.datamodel.Identifier {

	/**
	 * Sets the identifier scheme of the identifier.
	 * 
	 * @param scheme	the identifier scheme
	 */
	void setScheme(IDScheme scheme);
	
	/**
	 * Sets the value of the identifier.
	 * 
	 * @param value		the identifier value
	 */
	void setValue(String value);
}
