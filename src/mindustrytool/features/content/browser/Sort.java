package mindustrytool.features.content.browser;

public class Sort {
    private final String name;
    private final String value;

    public Sort(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }
    public String getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sort sort = (Sort) o;
        return name.equals(sort.name) && value.equals(sort.value);
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + value.hashCode();
    }
}
