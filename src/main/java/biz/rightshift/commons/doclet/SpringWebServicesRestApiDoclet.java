package biz.rightshift.commons.doclet;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationValue;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.RootDoc;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

/**
 * Custom REST API Doclet.
 */
public final class SpringWebServicesRestApiDoclet {

    private static final String DEFAULT_OUTPUT_FILE = "index.html";
    private static final String DEFAULT_TEMPLATE_NAME = "rest_api_template.vm";
    private static final String REST_KEY = "restCalls";
    private static final String ANNOTATION_CONTROLLER = "Controller";
    private static final String ANNOTATION_REQUEST_PARAM = "RequestMapping";
    private static final String ANNOTATION_PARAM_PATH_VARIABLE = "PathVariable";
    private static final String ANNOTATION_PARAM_REQUEST_PARAM = "RequestParam";
    private static final String ANNOTATION_PARAM_REQUEST_BODY = "RequestBody";
    private static final String ENCODING = "UTF-8";
    private static final String HEAD_TITLE = "headTitle";
    private static final String DEFAULT_HEADING = "JavaDoc API";

    private static final Pattern JAVADOC_TAG_PATTERN = Pattern.compile("\\{@\\w+\\b(.*?)\\}");

    private static final String OPTION_PARAM_HEADING = "-heading";
    private static final String OPTION_PARAM_TYPES = "-types";
    private static final String OPTION_PARAM_OUTPUT = "-output";
    private static final String OPTION_PARAM_TEMPLATE = "-template";

    /**
     * Starting point.
     *
     * @param root The <code>RootDoc</code>.
     * @return <code>true</code> if the doclet succeeded.
     */
    public static boolean start(final RootDoc root) {

        String headingOption = getOptionValue(root.options(), OPTION_PARAM_HEADING);
        String heading = headingOption == null ? DEFAULT_HEADING : headingOption;

        String templateOption = getOptionValue(root.options(), OPTION_PARAM_TEMPLATE);
        String templateName = templateOption == null ? DEFAULT_TEMPLATE_NAME : templateOption;

        String outputFileOption = getOptionValue(root.options(), OPTION_PARAM_OUTPUT);
        String outputFile = outputFileOption == null ? DEFAULT_OUTPUT_FILE : outputFileOption;

        String optionsTypes = getOptionValue(root.options(), OPTION_PARAM_TYPES);
        if (optionsTypes != null) {
            Description.setDescribeTypes(Arrays.asList(optionsTypes.split(",")));
        }

        List<RestApiDetails> restCalls = new ArrayList<>();
        for (ClassDoc classDoc : root.classes()) {
            if (isController(classDoc)) {
                restCalls.addAll(getRestCallsFor(classDoc));
            }
        }

        Velocity.setProperty("resource.loader", "class");
        Velocity.setProperty(
                "class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init();

        VelocityContext context = new VelocityContext();
        context.put(REST_KEY, restCalls);
        context.put(HEAD_TITLE, heading);
        try (Writer output = new OutputStreamWriter(new FileOutputStream(outputFile), ENCODING)) {
            Template template = Velocity.getTemplate(templateName, ENCODING);
            template.merge(context, output);
        } catch (Exception ex) {
            root.printError("Failed to write HTML: " + ex.getMessage());
            return false;
        }

        return true;
    }

    private static String getOptionValue(final String[][] options, final String option) {
        String optionValue = null;
        for (String[] opt : options) {
            if (opt[0].equals(option)) {
                optionValue = opt[1];
            }
        }
        return optionValue;
    }

    /**
     * Returns an options length. Any doclet that uses custom options
     * must define a method with this signature.
     *
     * @param option The option name.
     * @return the number of tokens in the option.
     */
    public static int optionLength(final String option) {
        if (option.equals(OPTION_PARAM_HEADING)
                || option.equals(OPTION_PARAM_TYPES)
                || option.equals(OPTION_PARAM_OUTPUT)
                || option.equals(OPTION_PARAM_TEMPLATE)) {
            return 2;
        }
        return 0;
    }

    /**
     * Process class for rest end points.
     * @param classDoc The <code>ClassDoc</code>.
     * @return the list of <code>RestApiDetails</code>
     */
    private static List<RestApiDetails> getRestCallsFor(final ClassDoc classDoc) {
        List<RestApiDetails> restCalls = new ArrayList<>();

        for (MethodDoc method : classDoc.methods()) {
            if (isEndpoint(method)) {
                List<RestApiParameter> pathVariables =
                    getVariables(method, ANNOTATION_PARAM_PATH_VARIABLE);

                List<RestApiParameter> requestParams =
                    getVariables(method, ANNOTATION_PARAM_REQUEST_PARAM);

                RestApiRequestBody body = getRequestBody(method);

                String url = getContextFor(method);

                AnnotationDesc requestParam = getAnnotation(method, ANNOTATION_REQUEST_PARAM);

                String httpVerb = getAnnotationValue(requestParam, "method");
                int lastIndex = httpVerb.lastIndexOf('.');
                if (lastIndex >= 0 && lastIndex < httpVerb.length()) {
                    httpVerb = httpVerb.substring(lastIndex + 1);
                }

                String consumes = getAnnotationValue(requestParam, "consumes");
                String produces = getAnnotationValue(requestParam, "produces");

                RestApiDetails endpoint = new RestApiDetails(
                        url,
                        httpVerb,
                        consumes,
                        produces,
                        pathVariables,
                        requestParams,
                        body,
                        sanitizeComment(method.commentText()));

                restCalls.add(endpoint);
            }
        }
        return restCalls;
    }

    /**
     * Gets the variables for an endpoint.
     *
     * @param method The method.
     * @param annotationType The annotation type denoting the type of variable.
     * @return The path variables.
     */
    private static List<RestApiParameter> getVariables(
            final MethodDoc method, final String annotationType) {
        List<RestApiParameter> variables = new ArrayList<>();
        for (Parameter parameter : method.parameters()) {
            AnnotationDesc annotation = getAnnotation(parameter, annotationType);
            if (annotation != null) {
                String annotationValue = getAnnotationValue(annotation, "value");
                String parameterName = annotationValue.isEmpty() ? parameter.name() : annotationValue;
                RestApiParameter apiParameter = new RestApiParameter(
                        parameterName,
                        parameter.typeName(),
                        getParamTag(method, parameter.name()));
                variables.add(apiParameter);
            }
        }
        return variables;
    }

    /**
     * Gets the param javadoc description.
     *
     * @param method The method.
     * @param name The parameter name.
     * @return The parameter description, or {@code null} if
     *          there is no tag.
     */
    private static String getParamTag(final MethodDoc method, final String name) {
        String description = "";
        for (ParamTag tag : method.paramTags()) {
            if (name.equals(tag.parameterName())) {
                description = sanitizeComment(tag.parameterComment());
                break;
            }
        }
        return description;
    }

    /**
     * Sanitizes a javadoc comment by removing all javadoc markup.
     *
     * @param comment The javadoc comment.
     * @return The sanitized string.
     */
    private static String sanitizeComment(final String comment) {
        Matcher matcher = JAVADOC_TAG_PATTERN.matcher(comment);
        String sanitized = comment;
        while (matcher.find() && matcher.groupCount() > 0) {
            sanitized = sanitized.replace(matcher.group(0), matcher.group(1).trim());
        }
        return sanitized;
    }

    /**
     * Gets the context for an endpoint.
     *
     * This defined by the @RequestMapping value on the class
     * and/or method.
     *
     * @param method The endpoint.
     * @return The context.
     */
    private static String getContextFor(final MethodDoc method) {
        String classContext = "";
        String methodContext = "";

        AnnotationDesc classRequestMapping = getAnnotation(
                method.containingClass(), ANNOTATION_REQUEST_PARAM);
        if (classRequestMapping == null) {
            classRequestMapping = getAnnotation(method.containingClass(), ANNOTATION_CONTROLLER);
        }
        if (classRequestMapping != null) {
            classContext = getAnnotationValue(classRequestMapping, "value");
        }

        AnnotationDesc methodRequestMapping = getAnnotation(method, ANNOTATION_REQUEST_PARAM);
        if (methodRequestMapping != null) {
            methodContext = getAnnotationValue(methodRequestMapping, "value");
        }

        return classContext + methodContext;
    }

    /**
     * Determines whether or not a class is a Spring MVC controller.
     *
     * @param classDoc The class.
     * @return {@code true} if the class is a Controller.
     */
    private static boolean isController(final ClassDoc classDoc) {
        return isAnnotatedWith(classDoc, ANNOTATION_CONTROLLER);
    }

    /**
     * Determines whether or not a method is an endpoint.
     * An endpoint is annotated with {@code {@literal @}RequestMapping}.
     *
     * @param methodDoc The method
     * @return {@code true} if the class is an endpoint.
     */
    private static boolean isEndpoint(final MethodDoc methodDoc) {
        return isAnnotatedWith(methodDoc, ANNOTATION_REQUEST_PARAM);
    }

    /**
     * Generic function to look through the annotations for a
     * {@code ProgramElementDoc} to indicate if a particular
     * annotation exists.
     *
     * @param programElement The element to inspect.
     * @param annotationName The annotation name.
     * @return {@code true} if the annotation exists.
     */
    private static boolean isAnnotatedWith(
            final ProgramElementDoc programElement, final String annotationName) {
        return getAnnotation(programElement, annotationName) != null;
    }

    /**
     * Generic function to look through the annotations for a
     * {@code Parameter} to indicate if a particular
     * annotation exists.
     *
     * @param parameter The element to inspect.
     * @param annotationName The annotation name.
     * @return {@code true} if the annotation exists.
     */
    private static boolean isAnnotatedWith(
            final Parameter parameter, final String annotationName) {
        return getAnnotation(parameter, annotationName) != null;
    }

    /**
     * Generic function to look through the annotations for a
     * {@code ProgramElementDoc} for a particular annotation.
     *
     * @param programElement The element to inspect.
     * @param annotationName The annotation name.
     * @return {@code AnnotationDesc} if the annotation exists,
     *          or {@code null} if it doesn't.
     */
    private static AnnotationDesc getAnnotation(
            final ProgramElementDoc programElement, final String annotationName) {
        for (AnnotationDesc annotation : programElement.annotations()) {
            if (annotation.annotationType().name().equals(annotationName)) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * Generic function to look through the annotations for a
     * {@code Parameter} for a particular annotation.
     *
     * @param parameter The element to inspect.
     * @param annotationName The annotation name.
     * @return {@code AnnotationDesc} if the annotation exists,
     *          or {@code null} if it doesn't.
     */
    private static AnnotationDesc getAnnotation(
            final Parameter parameter, final String annotationName) {
        for (AnnotationDesc annotation : parameter.annotations()) {
            if (annotation.annotationType().name().equals(annotationName)) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * Gets the String value for a annotation parameter.
     *
     * @param annotation The annotation.
     * @param param The parameter name.
     * @return The associated value, or an empty string if not found.
     */
    private static String getAnnotationValue(
            final AnnotationDesc annotation, final String param) {
        for (AnnotationDesc.ElementValuePair pair : annotation.elementValues()) {
            if (param.equals(pair.element().name())) {
                AnnotationValue value = pair.value();
                if (value != null) {
                    return pair.value().toString().replaceAll("\"", "");
                }
            }
        }
        return "";
    }

    /**
     * Gets the request body description, if available, for a method.
     *
     * @param method The method.
     * @return The method body description, or {@code null} if this
     *          endpoint does not require a body.
     */
    private static RestApiRequestBody getRequestBody(final MethodDoc method) {
        RestApiRequestBody bodyDescription = null;

        for (Parameter parameter : method.parameters()) {
            if (isAnnotatedWith(parameter, ANNOTATION_PARAM_REQUEST_BODY)) {
                /* this parameter is a @RequestBody */
                String typeDescription = Description.describeType(
                        parameter.type()
                );
                bodyDescription = new RestApiRequestBody(
                        parameter.name(),
                        parameter.type().toString(),
                        getParamTag(method, parameter.name()),
                        typeDescription);
            }
        }
        return bodyDescription;
    }


    /**
     * @return The language version this doclet works with.
     */
    public static LanguageVersion languageVersion() {
        return LanguageVersion.JAVA_1_5;
    }

    /**
     * Don't instantiate directly.
     */
    private SpringWebServicesRestApiDoclet() {
    }
}
