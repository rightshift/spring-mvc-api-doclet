package biz.rightshift.commons.doclet;

import java.util.ArrayList;
import java.util.List;

/**
 * Detailing a Rest Api Call.
 */
public class RestApiDetails {

    private String url = "";
    private String method = "unknown";
    private String produces = "";
    private String consumes = "";
    private List<RestApiParameter> pathVariables = new ArrayList<>();
    private List<RestApiParameter> requestParams = new ArrayList<>();
    private RestApiRequestBody requestBody;
    private String description;

    /**
     * @param url The URL.
     * @param method The method.
     * @param produces The output format.
     * @param consumes The input format.
     * @param pathVariables The path variables (after the base URL and before the ?).
     * @param requestParams The request parameters (after the ? part of the URL).
     * @param requestBody The request body.
     * @param description The request description.
     */
    public RestApiDetails(
            final String url,
            final String method,
            final String produces,
            final String consumes,
            final List<RestApiParameter> pathVariables,
            final List<RestApiParameter> requestParams,
            final RestApiRequestBody requestBody,
            final String description) {
        this.url = url;
        this.method = method;
        this.produces = produces;
        this.consumes = consumes;
        this.pathVariables = pathVariables;
        this.requestParams = requestParams;
        this.requestBody = requestBody;
        this.description = description;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return the method
     */
    public String getMethod() {
        return method;
    }

    /**
     * @return the produces
     */
    public String getProduces() {
        return produces;
    }

    /**
     * @return the consumes
     */
    public String getConsumes() {
        return consumes;
    }

    /**
     * @return the path variables
     */
    public List<RestApiParameter> getPathVariables() {
        return pathVariables;
    }

    /**
     * @return The request parameters.
     */
    public List<RestApiParameter> getRequestParams() {
        return requestParams;
    }

    /**
     * @return The request body.
     */
    public RestApiRequestBody getRequestBody() {
        return requestBody;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    @Override
    public final String toString() {
        return String.format(
                "RestApiDetails[url=%s, method=%s, produces=%s, consumes=%s]",
                getUrl(), getMethod(), getProduces(), getConsumes());
    }
}
