package org.store.app.scheduling.cleanup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.store.app.service.CartService;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartCleanupJob {

    private final CartService cartService;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void run() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(10);
        int deletedCount = cartService.deleteOldAnonymousCarts(cutoffDate);
        log.info("Deleted {} anonymous carts older than 10 days", deletedCount);
    }
}
