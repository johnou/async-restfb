package it.johno.proto.facebook;

import com.restfb.DefaultJsonMapper;
import com.restfb.Version;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author Johno Crawford (johno.crawford@gmail.com)
 */
public class FacebookClientFactory {

    private int connectAndReadTimeoutMillis = (int) TimeUnit.SECONDS.toMillis(6);

    private final AsyncHttpClient asyncHttpClient;

    public FacebookClientFactory() {
        asyncHttpClient = createHttpClient();
    }

    private AsyncHttpClient createHttpClient() {
        return new DefaultAsyncHttpClient(buildClientConfig());
    }

    private AsyncHttpClientConfig buildClientConfig() {
        return new DefaultAsyncHttpClientConfig.Builder()
                .setConnectTimeout(connectAndReadTimeoutMillis)
                .setReadTimeout(connectAndReadTimeoutMillis)
                //.setEventLoopGroup(eventLoopProvider.getWorkerGroup())
                //.setUseNativeTransport(eventLoopProvider.isNativeEpoll())
                .build();
    }

    public void shutdown() {
        try {
            asyncHttpClient.close();
        } catch (IOException ignore) {
        }
    }

    FacebookClientAsync createFacebookClient(String accessToken) {
        return new DefaultFacebookClientAsync(accessToken, new DefaultWebRequestorAsync(asyncHttpClient), new DefaultJsonMapper(), Version.VERSION_2_1);
    }
}
