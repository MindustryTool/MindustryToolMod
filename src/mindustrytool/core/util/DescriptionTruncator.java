package mindustrytool.core.util;

public class DescriptionTruncator {
    public static String truncate(String desc, int maxNewlines) {
        int count = 0;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < desc.length(); i++) {
            char c = desc.charAt(i);
            if (c == '\n') { if (++count < maxNewlines) result.append(c); }
            else result.append(c);
        }
        return result.toString();
    }
}
