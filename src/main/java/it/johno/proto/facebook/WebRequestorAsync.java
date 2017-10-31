package it.johno.proto.facebook;

import com.restfb.BinaryAttachment;

import java.util.concurrent.CompletionStage;

import static com.restfb.WebRequestor.*;

/**
 * @author Johno Crawford (johno.crawford@gmail.com)
 */
public interface WebRequestorAsync {
    CompletionStage<Response> executeGetAsync(String url);
    CompletionStage<Response> executePostAsync(String url, String parameters, BinaryAttachment... binaryAttachments);
    CompletionStage<Response> executeDeleteAsync(String url);
}