package mindustrytool.core.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Sort {
    private final String name;
    private final String value;
}
