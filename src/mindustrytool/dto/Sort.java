package mindustrytool.dto;

public class Sort {
    private final String name;
    private final String value;

    public Sort(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

}
