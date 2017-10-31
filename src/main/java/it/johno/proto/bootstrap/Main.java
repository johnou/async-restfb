package it.johno.proto.bootstrap;

import it.johno.proto.facebook.FacebookRestHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CountDownLatch;

/**
 * @author Johno Crawford (johno.crawford@gmail.com)
 */
public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        FacebookRestHelper facebookRestHelper = new FacebookRestHelper();
        try {
            CountDownLatch latch = new CountDownLatch(3);
            for (int i = 0; i < 3; i++) {
                facebookRestHelper.fetchUser("your access token").whenComplete((user, throwable) -> {
                    logger.info("Fetch user returned {}", user);
                    latch.countDown();
                });
            }
            latch.await();
        } finally {
            facebookRestHelper.shutdown();
        }
    }
}
