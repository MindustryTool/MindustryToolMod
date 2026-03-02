package mindustrytool.dto;

import java.util.List;

public class TaskResponse {
    public List<TaskData> data;
    public Meta meta;

    public static class Meta {
        public int total;
        public int page;
        public int limit;
        public int totalPages;
    }
}
