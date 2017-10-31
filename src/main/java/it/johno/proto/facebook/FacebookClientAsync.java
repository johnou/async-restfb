package it.johno.proto.facebook;

import com.restfb.Parameter;

import java.util.concurrent.CompletionStage;

/**
 * @author Johno Crawford (johno.crawford@gmail.com)
 */
public interface FacebookClientAsync {
    <T> CompletionStage<T> fetchObjectAsync(String object, Class<T> objectType, Parameter... parameters);
}
