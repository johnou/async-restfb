package it.johno.proto.facebook;

import com.restfb.BinaryAttachment;
import org.asynchttpclient.AsyncHttpClient;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;

import static com.restfb.WebRequestor.Response;

/**
 * @author Johno Crawford (johno.crawford@gmail.com)
 */
class DefaultWebRequestorAsync implements WebRequestorAsync {

    private final AsyncHttpClient asyncHttpClient;

    DefaultWebRequestorAsync(AsyncHttpClient asyncHttpClient) {
        this.asyncHttpClient = asyncHttpClient;
    }

    @Override
    public CompletionStage<Response> executeGetAsync(String url) {
        return asyncHttpClient.prepareGet(url).execute().toCompletableFuture().thenApply(response ->
                new Response(response.getStatusCode(), response.getResponseBody(StandardCharsets.UTF_8)));
    }

    @Override
    public CompletionStage<Response> executePostAsync(String url, String parameters, BinaryAttachment... binaryAttachments) {
        if (binaryAttachments != null) {
            throw new RuntimeException("Binary attachments not supported!");
        }

        return asyncHttpClient.preparePost(url).setBody(parameters.getBytes(StandardCharsets.UTF_8)).execute().toCompletableFuture().thenApply(response ->
                new Response(response.getStatusCode(), response.getResponseBody(StandardCharsets.UTF_8)));
    }

    @Override
    public CompletionStage<Response> executeDeleteAsync(String url) {
        return asyncHttpClient.prepareDelete(url).execute().toCompletableFuture().thenApply(response ->
                new Response(response.getStatusCode(), response.getResponseBody(StandardCharsets.UTF_8)));
    }
}
