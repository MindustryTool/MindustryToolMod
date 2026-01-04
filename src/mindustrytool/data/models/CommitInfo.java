package mindustrytool.data.models;

import arc.util.serialization.Jval;

/**
 * Data class representing a GitHub Commit.
 */
public class CommitInfo {
    public final String message;
    public final String authorName;
    public final String date;
    public final String htmlUrl;
    public final String sha;

    public final String avatarUrl;

    public final String[] parents;

    public CommitInfo(Jval json) {
        this.sha = json.getString("sha", "");
        this.htmlUrl = json.getString("html_url", "");

        Jval authorUser = json.get("author");
        if (authorUser != null) {
            this.avatarUrl = authorUser.getString("avatar_url", "");
        } else {
            this.avatarUrl = "";
        }

        // Parse parents
        Jval parentsJson = json.get("parents");
        if (parentsJson != null && parentsJson.isArray()) {
            this.parents = new String[parentsJson.asArray().size];
            for (int i = 0; i < parentsJson.asArray().size; i++) {
                this.parents[i] = parentsJson.asArray().get(i).getString("sha", "");
            }
        } else {
            this.parents = new String[0];
        }

        Jval commit = json.get("commit");
        if (commit != null) {
            this.message = commit.getString("message", "").split("\n")[0];

            Jval author = commit.get("author");
            if (author != null) {
                this.authorName = author.getString("name", "Unknown");
                this.date = author.getString("date", "");
            } else {
                this.authorName = "Unknown";
                this.date = "";
            }
        } else {
            this.message = "No description";
            this.authorName = "Unknown";
            this.date = "";
        }
    }
}
