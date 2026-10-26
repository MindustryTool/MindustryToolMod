package solim.overlay;

public interface DockTarget {

    String id();

    float x();

    float y();

    float width();

    float height();

    boolean active();

    default boolean supports(DockSide side) {
        return side != null && side != DockSide.INSIDE;
    }
}
