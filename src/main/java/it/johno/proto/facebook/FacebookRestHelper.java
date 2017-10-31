package it.johno.proto.facebook;

import com.restfb.Parameter;
import com.restfb.exception.FacebookException;
import com.restfb.exception.FacebookNetworkException;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.types.User;
import it.johno.proto.util.ExecutorUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

/**
 * @author Johno Crawford (johno.crawford@gmail.com)
 */
public class FacebookRestHelper {

    private static final Logger logger = LogManager.getLogger(FacebookRestHelper.class);

    private static final Integer FACEBOOK_LOGGED_OUT_ERROR_CODE = 190;

    private FacebookClientFactory facebookClientFactory = new FacebookClientFactory();

    private ForkJoinPool executionPool = ExecutorUtils.newScalingThreadPool("RestWorker-", 12);

    public void shutdown() {
        facebookClientFactory.shutdown();
        ExecutorUtils.shutdown(executionPool);
    }

    public CompletableFuture<User> fetchUser(String accessToken) {
        FacebookClientAsync facebookClient = facebookClientFactory.createFacebookClient(accessToken);
        return facebookClient.fetchObjectAsync("me", User.class, Parameter.with("fields", "id,name,age_range")).exceptionally(ex -> {
            if (ex instanceof FacebookNetworkException) {
                logger.error("Unexpected network error getting Facebook related user data from the access token", ex);
            } else if (ex instanceof FacebookOAuthException) {
                if (FACEBOOK_LOGGED_OUT_ERROR_CODE.equals(((FacebookOAuthException) ex).getErrorCode())) {
                    logger.warn("Facebook login failed because of expired token: " + ((FacebookOAuthException) ex).getErrorMessage());
                } else {
                    logger.warn("Unable to login Facebook: " + ((FacebookOAuthException) ex).getErrorMessage(), ex);
                }
            } else if (ex instanceof FacebookException) {
                logger.warn("Unable to get Facebook related user data from the access token: " + ex.getMessage(), ex);
            }
            return null;
        }).toCompletableFuture().whenCompleteAsync((result, throwable) -> {
            // place holder, just to ensure the completion happens in execution thread (async-http-client threads should not be used for
            // blocking IO as they would interrupt the Netty event loop)
        }, executionPool);
    }
}
