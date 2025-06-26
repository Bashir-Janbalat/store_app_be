package org.store.app.scheduling.cleanup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.store.app.service.WishlistService;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class WishlistCleanupJob {

    private final WishlistService wishlistService;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void run() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(10);
        int deletedCount = wishlistService.deleteOldAnonymousWishlists(cutoffDate);
        log.info("Deleted {} anonymous wishlists older than 10 days", deletedCount);
    }
}
