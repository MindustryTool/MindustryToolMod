package solim.modifier;

public interface BlurCapability extends ElementCapability {

    void backdropBlur(float radius);

    void contentBlur(float radius);

    float backdropBlur();

    float contentBlur();
}
