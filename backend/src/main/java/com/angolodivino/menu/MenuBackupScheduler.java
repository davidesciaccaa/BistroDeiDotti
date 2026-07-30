package com.angolodivino.menu;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MenuBackupScheduler {

    private final MenuOverridesStore store;

    public MenuBackupScheduler(MenuOverridesStore store) {
        this.store = store;
    }

    /** Startup catch-up is handled by the store; this maintains snapshots while the app stays up. */
    @Scheduled(cron = "0 15 3 * * *", zone = "Europe/Rome")
    public void maintainBackups() {
        store.ensureBackups();
    }
}
