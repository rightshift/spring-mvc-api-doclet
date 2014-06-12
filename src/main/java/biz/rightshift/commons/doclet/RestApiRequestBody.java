package biz.rightshift.commons.doclet;

/**
 * Rest Api Request Body.
 */
public class RestApiRequestBody extends RestApiParameter {

    private String structure;

    /**
     * Constructor injecting fields.
     * @param name The variable's name.
     * @param type The variable's type.
     * @param description The description.
     * @param structure The request body's data structure.
     */
    public RestApiRequestBody(
            final String name, final String type, final String description, final String structure) {
        super(name, type, description);
        this.setStructure(structure);
    }

    /**
     * @return The data structure.
     */
    public String getStructure() {
        return structure;
    }

    /**
     * @param structure The data structure to set.
     */
    public void setStructure(final String structure) {
        this.structure = structure;
    }
}
