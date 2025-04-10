package org.holodeckb2b.bdxr.smp.server.svc.peppol;

import java.security.cert.CertificateException;
import java.time.LocalDate;

import org.holodeckb2b.bdxr.smp.server.db.CertificateUpdate;
import org.holodeckb2b.bdxr.smp.server.db.SMLRegistration;
import org.holodeckb2b.bdxr.smp.server.svc.SMPCertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CertUpdateWorker {

	private static boolean	checkedOnStart = false; 
	
	@Autowired
	protected SMLClient smlClient;
	
	@Autowired
	protected SMPCertificateService certSvc;
	
	@EventListener(ApplicationReadyEvent.class)	
	public void checkOnStart() {
		if (!checkedOnStart) {
			checkForPendingUpdate();
			checkedOnStart = true;
		}
	}
	
	@Scheduled(cron = "@daily")
	public void checkForPendingUpdate() {
		if (!smlClient.isSMPRegistered())
			return;
		
		log.trace("Check if there is a pending update of the SMP Certificate");
		CertificateUpdate pendingUpd;
		try {
			SMLRegistration reg = smlClient.getSMLRegistration();
			pendingUpd = reg.getPendingCertUpdate();
		} catch (CertificateException invalidConf) {
			log.error("Could not retrieve SML configuration");
			return;
		}
		if (pendingUpd == null || pendingUpd.getActivation().isAfter(LocalDate.now())) {
			log.trace("There is no pending certificate update or its activation date is after today");
			return;
		}
		log.debug("The new SMP certificate must be activated");
		try {
			certSvc.setKeyPair(pendingUpd.getKeyPair());
			log.debug("Clean pending update");
			smlClient.clearPendingUpdate();
			log.info("SMP certificate updated");
		} catch (CertificateException updFailure) {
			log.error("Update of the SMP certificate failed! Error: {}", updFailure.getMessage());			
		}
	}
}
