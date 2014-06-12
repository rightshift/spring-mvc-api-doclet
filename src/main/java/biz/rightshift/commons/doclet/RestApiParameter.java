package biz.rightshift.commons.doclet;

/**
 * Rest Api Parameter.
 */
public class RestApiParameter {
    private String type;
    private String name;
    private String description;

    /**
     * Constructor injecting fields.
     * @param name The variable's name.
     * @param type The variable's type.
     * @param description The variable's description.
     */
    public RestApiParameter(final String name, final String type, final String description) {
        this.name = name;
        this.type = type;
        this.description = description;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
