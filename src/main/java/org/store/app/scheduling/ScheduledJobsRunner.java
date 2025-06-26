package org.store.app.scheduling;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.store.app.scheduling.cleanup.CartCleanupJob;
import org.store.app.scheduling.cleanup.WishlistCleanupJob;

@Component
@Profile({"dev"})
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobsRunner {


    @Value("${run.jobs:false}")
    private boolean runJobs;

    private final CartCleanupJob cartCleanupJob;
    private final WishlistCleanupJob wishlistCleanupJob;


    @PostConstruct
    public void runAllJobs() {
        if (!runJobs) {
            log.info("Job execution skipped (run.jobs=false)");
            return;
        }

        log.info("Running Scheduled Jobs on dev profile ... ");
        cartCleanupJob.run();
        wishlistCleanupJob.run();
        log.info("Scheduled Jobs completed");
    }
}
