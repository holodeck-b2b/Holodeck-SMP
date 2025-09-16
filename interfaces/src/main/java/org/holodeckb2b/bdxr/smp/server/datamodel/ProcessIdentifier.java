package org.holodeckb2b.bdxr.smp.server.datamodel;

/**
 * Defines the meta-data of the Process identifier used to identify a <i>Process</i>, maintained by Holodeck SMP. This 
 * is the same as the definition provided in the <a href="https://github.com/holodeck-b2b/bdxr-common">BDXR Common 
 * project</a> but with the addition of <code>setter</code> methods. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public interface ProcessIdentifier extends Identifier, org.holodeckb2b.bdxr.common.datamodel.ProcessIdentifier {
	
	/**
	 * Sets the indicator that this Process Identifier is the special "no process" identifier as defined in the SMP
	 * specifications.
	 */
	void setNoProcess();
}
