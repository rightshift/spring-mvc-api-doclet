package biz.rightshift.commons.doclet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Descriptions customizer for SpringWebServicesRestApiDoclet.
 */
public final class Description {

    private static volatile List<String> describableTypes = new ArrayList<>();

    private Description() {
        // Prevent Instantiation.
    }

    /**
     * Mutator for the list of Types to describe.
     * @param describeTypes  List of String of describable types.
     */
    public static void setDescribeTypes(final List<String> describeTypes) {
        if (Description.describableTypes != null) {
            Description.describableTypes = describeTypes;
        }
    }

     /**
     * Describes a type as a JSON string.
     *
     * @param type The type to describe.
     * @return A JSON representation of the type.
     */
    public static String describeType(final Type type) {
        ClassDoc classDoc = type.asClassDoc();

        Object typeDescription = describeType(classDoc);
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(typeDescription);
        } catch (JsonProcessingException e) {
            System.err.println("Failed to parse type " + e.getMessage());
        }
        return null;
    }

    /**
     * Utility function to describe a type.
     *
     * @param type The type to describe.
     * @return A representation of the type.
     */
    public static Object describeType(final ClassDoc type) {
        if (type == null) {
            return "";
        }
        if (shouldDescribe(type)) {
            if (type.isEnum()) {
                /* handle enums */
                return describeEnum(type.enumConstants());
            } else {
                /* otherwise its a type */
                Map<String, Object> description = new HashMap<>();
                for (MethodDoc method : type.methods()) {
                    Type returnType = method.returnType();
                    if (!"void".equals(returnType.typeName())) {
                        if (method.name().startsWith("get") || method.name().startsWith("is")) {
                            String name = asFieldName(method);
                            Object fieldDescription;
                            if (returnType.isPrimitive()) {
                                fieldDescription = returnType.typeName();
                            } else if (returnType.asParameterizedType() != null && isTypeOf(Collection.class, returnType)) {
                                Object typeParameterDesc =
                                    describeType(returnType.asParameterizedType().typeArguments()[0].asClassDoc());
                                fieldDescription = Arrays.asList(typeParameterDesc);
                            } else {
                                fieldDescription = describeType(returnType.asClassDoc());
                            }
                            description.put(name, fieldDescription);
                        }
                    }
                }
                return description;
            }
        } else {
            return type.simpleTypeName();
        }
    }

    /**
     * Indicates whether a type is to be described.
     * Typically, the type needs to not be a java String,
     * or one of the boxed/Un-boxed primitives. If no types
     * have been defined then just return true.
     *
     * @param type The type to check.
     * @return <code>true</code> if the type is worth describing.
     */
    private static boolean shouldDescribe(final ClassDoc type) {
        if (Description.describableTypes.isEmpty()) {
            return true;
        }

        for (String describeType : describableTypes) {
            if (type.qualifiedTypeName().startsWith(describeType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param type The type to check against.
     * @param candidate The type to check.
     * @return {@code true} if the provided class is of the given type.
     */
    private static boolean isTypeOf(final Class<?> type, final Type candidate) {
        if (candidate == null) {
            return false;
        }
        if (candidate.qualifiedTypeName().equals(type.getName())) {
            return true;
        }
        if (isTypeOf(type, candidate.asClassDoc().superclassType())) {
            return true;
        }
        for (Type interfaceType : candidate.asClassDoc().interfaceTypes()) {
            if (interfaceType.qualifiedTypeName().equals(type.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Describes an enum as a BNF set of options.
     *
     * @param enumConstants The enumConstants.
     * @return A BNF string of options.
     */
    private static String describeEnum(final FieldDoc[] enumConstants) {
        StringBuilder options = new StringBuilder();
        String sep = "";
        for (FieldDoc option : enumConstants) {
            options.append(sep).append(option.name());
            sep = "|";
        }
        return options.toString();
    }

    /**
     * Derives a field name for a method.
     *
     * This simply strips leading lower case letters and inverts
     * the remaining first letter.
     *
     * @param method The method to convert to a field name.
     * @return The surmised field name.
     */
    private static String asFieldName(final MethodDoc method) {
        int index = 0;
        char[] charArray = method.name().toCharArray();
        /* find the first upper case character */
        while (Character.isLowerCase(charArray[index])) {
            index++;
            if (index >= charArray.length) {
                return method.name();
            }
        }
        /* and keep the result of lower casing it */
        StringBuilder fieldName = new StringBuilder();
        fieldName.append(Character.toLowerCase(charArray[index++]));
        /* and then keep the rest */
        for ( ; index < charArray.length; index++) {
            fieldName.append(charArray[index]);
        }
        return fieldName.toString();
    }

}
