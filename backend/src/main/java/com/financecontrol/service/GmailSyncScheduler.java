package com.financecontrol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GmailSyncScheduler {

    private final GmailService gmailService;

    public GmailSyncScheduler(@Lazy GmailService gmailService) {
        this.gmailService = gmailService;
    }

    @Scheduled(fixedDelay = 1800000)
    public void syncBcpEmails() {
        gmailService.syncBcpEmails();
    }
}
