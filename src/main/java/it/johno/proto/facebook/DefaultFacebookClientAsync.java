package it.johno.proto.facebook;

import com.restfb.BinaryAttachment;
import com.restfb.DefaultFacebookClient;
import com.restfb.DefaultWebRequestor;
import com.restfb.JsonMapper;
import com.restfb.Parameter;
import com.restfb.Version;
import com.restfb.exception.FacebookNetworkException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.restfb.WebRequestor.Response;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

/**
 * @author Johno Crawford (johno.crawford@gmail.com)
 */
class DefaultFacebookClientAsync extends DefaultFacebookClient implements FacebookClientAsync {

    private final WebRequestorAsync webRequestorAsync;

    public DefaultFacebookClientAsync(String accessToken, WebRequestorAsync webRequestorAsync,
                                      JsonMapper jsonMapper, Version apiVersion) {
        super(accessToken, new DefaultWebRequestor(), jsonMapper, apiVersion);
        this.webRequestorAsync = webRequestorAsync;
    }

    @Override
    public <T> CompletionStage<T> fetchObjectAsync(String object, Class<T> objectType, Parameter... parameters) {
        try {
            verifyParameterPresence("object", object);
            verifyParameterPresence("objectType", objectType);
        } catch (RuntimeException e) {
            CompletableFuture<T> cf = new CompletableFuture<>();
            cf.completeExceptionally(e);
            return cf;
        }
        return makeRequestAsync(object, parameters).thenApply(s -> jsonMapper.toJavaObject(s, objectType));
    }

    protected CompletionStage<String> makeRequestAsync(String endpoint, Parameter... parameters) {
        return makeRequestAsync(endpoint, false, false, null, parameters);
    }

    protected CompletionStage<String> makeRequestAsync(String endpoint, final boolean executeAsPost, final boolean executeAsDelete,
                                                       final List<BinaryAttachment> binaryAttachments, Parameter... parameters) {
        try {
            verifyParameterLegality(parameters);
        } catch (RuntimeException e) {
            CompletableFuture<String> cf = new CompletableFuture<>();
            cf.completeExceptionally(e);
            return cf;
        }

        if (executeAsDelete && isHttpDeleteFallback()) {
            parameters = parametersWithAdditionalParameter(Parameter.with(METHOD_PARAM_NAME, "delete"), parameters);
        }

        if (!endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }

        final String fullEndpoint =
                createEndpointForApiCall(endpoint, binaryAttachments != null && !binaryAttachments.isEmpty());
        final String parameterString = toParameterString(parameters);

        return makeRequestAndProcessResponseAsync(new RequestorAsync() {
            /**
             * @see RequestorAsync#makeRequestAsync()
             */
            @Override
            public CompletionStage<Response> makeRequestAsync() {
                if (executeAsDelete && !isHttpDeleteFallback()) {
                    return webRequestorAsync.executeDeleteAsync(fullEndpoint + "?" + parameterString);
                } else {
                    return executeAsPost
                            ? webRequestorAsync.executePostAsync(fullEndpoint, parameterString,
                            binaryAttachments == null ? null
                                    : binaryAttachments.toArray(new BinaryAttachment[binaryAttachments.size()]))
                            : webRequestorAsync.executeGetAsync(fullEndpoint + "?" + parameterString);
                }
            }
        });
    }

    protected interface RequestorAsync {
        CompletionStage<Response> makeRequestAsync();
    }

    protected CompletionStage<String> makeRequestAndProcessResponseAsync(RequestorAsync requestor) {
        // Perform a GET or POST to the API endpoint
        return requestor.makeRequestAsync().handle((response, throwable) -> {
            if (throwable != null) {
                CompletableFuture<String> cf = new CompletableFuture<>();
                cf.completeExceptionally(throwable);
                return cf;
            }

            CompletableFuture<String> cf = new CompletableFuture<>();
            try {
                // If we get any HTTP response code other than a 200 OK or 400 Bad Request
                // or 401 Not Authorized or 403 Forbidden or 404 Not Found or 500 Internal
                // Server Error or 302 Not Modified
                // throw an exception.
                if (HTTP_OK != response.getStatusCode() && HTTP_BAD_REQUEST != response.getStatusCode()
                        && HTTP_UNAUTHORIZED != response.getStatusCode() && HTTP_NOT_FOUND != response.getStatusCode()
                        && HTTP_INTERNAL_ERROR != response.getStatusCode() && HTTP_FORBIDDEN != response.getStatusCode()
                        && HTTP_NOT_MODIFIED != response.getStatusCode()) {
                    throw new FacebookNetworkException("Facebook request failed", response.getStatusCode());
                }

                String json = response.getBody();

                // If the response contained an error code, throw an exception.
                throwFacebookResponseStatusExceptionIfNecessary(json, response.getStatusCode());

                // If there was no response error information and this was a 500 or 401
                // error, something weird happened on Facebook's end. Bail.
                if (HTTP_INTERNAL_ERROR == response.getStatusCode() || HTTP_UNAUTHORIZED == response.getStatusCode()) {
                    throw new FacebookNetworkException("Facebook request failed", response.getStatusCode());
                }
                cf.complete(json);
            } catch (Exception e) {
                cf.completeExceptionally(e);
            }
            return cf;
        }).thenCompose(o -> o);
    }

}
