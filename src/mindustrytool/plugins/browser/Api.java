package mindustrytool.plugins.browser;

import arc.func.*;

public class Api {
    public static void downloadSchematic(String id, ConsT<byte[], Exception> c) {
        download(Config.API_URL + "schematics/" + id + "/data", c);
    }

    public static void downloadMap(String id, ConsT<byte[], Exception> c) {
        download(Config.API_URL + "maps/" + id + "/data", c);
    }

    private static void download(String url, ConsT<byte[], Exception> c) {
        AuthHttp.get(url)
                .error(e -> {
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    String str = e.toString();
                    boolean is401 = msg.contains("401") || str.contains("401") || msg.contains("UNAUTHORIZED")
                            || str.contains("UNAUTHORIZED");

                    if (is401) {
                        arc.util.Log.info(
                                "Access Unauthorized (401) caught in error handler, invalidating token and retrying as guest...");
                        BrowserAuthService.invalidateToken();
                        // Retry as Guest
                        GuestHttp.get(url)
                                .error(e2 -> {
                                    arc.util.Log.err("Guest Retry Failed: " + url, e2);
                                    try {
                                        c.get(null);
                                    } catch (Exception ex) {
                                        arc.util.Log.err(ex);
                                    }
                                })
                                .submit(r2 -> arc.Core.app.post(() -> {
                                    try {
                                        if (r2.getStatus() == arc.util.Http.HttpStatus.OK) {
                                            c.get(r2.getResult());
                                        } else {
                                            arc.util.Log.info("Guest retry returned status: " + r2.getStatus());
                                            arc.util.Log.err("Guest Retry Failed with Status: " + r2.getStatus());
                                            c.get(null);
                                        }
                                    } catch (Exception ex) {
                                        arc.util.Log.err(ex);
                                    }
                                }));
                    } else {
                        arc.util.Log.err(url, e);
                        try {
                            c.get(null);
                        } catch (Exception ex) {
                            arc.util.Log.err(ex);
                        }
                    }
                })
                .submit(r -> {
                    arc.util.Log.info("Api.download: " + url + " Status: " + r.getStatus());
                    if (r.getStatus() == arc.util.Http.HttpStatus.UNAUTHORIZED) {
                        arc.util.Log.info("Access Unauthorized (401), invalidating token and retrying as guest...");
                        BrowserAuthService.invalidateToken();
                        // Retry as Guest
                        GuestHttp.get(url).submit(r2 -> arc.Core.app.post(() -> {
                            try {
                                if (r2.getStatus() == arc.util.Http.HttpStatus.OK) {
                                    c.get(r2.getResult());
                                } else {
                                    arc.util.Log.info("Guest retry returned status: " + r2.getStatus());
                                    arc.util.Log.err("Guest Retry Failed with Status: " + r2.getStatus());
                                    c.get(null);
                                }
                            } catch (Exception ex) {
                                arc.util.Log.err(ex);
                            }
                        }));
                    } else if (r.getStatus() == arc.util.Http.HttpStatus.OK) {
                        byte[] result = r.getResult();
                        arc.Core.app.post(() -> {
                            try {
                                if (result.length == 0) {
                                    String debug = "Status: " + r.getStatus() + "\nCL: " + r.getHeader("Content-Length")
                                            + "\nHeaders: " + r.getHeaders();
                                    arc.util.Log.err("Zero Bytes Debug: " + debug);
                                }
                                c.get(result);
                            } catch (Exception ex) {
                                arc.util.Log.err(ex);
                            }
                        });
                    } else {
                        arc.util.Log.err("Api.download failed: " + r.getStatus() + " for " + url);
                        arc.Core.app.post(() -> {
                            try {
                                c.get(null);
                            } catch (Exception ex) {
                                arc.util.Log.err(ex);
                            }
                        });
                    }
                });
    }

    public static void findSchematicById(String id, Cons<SchematicDetailData> c) {
        ApiRequest.get(Config.API_URL + "schematics/" + id, SchematicDetailData.class, c);
    }

    public static void findMapById(String id, Cons<MapDetailData> c) {
        ApiRequest.get(Config.API_URL + "maps/" + id, MapDetailData.class, c);
    }

    public static void findUserById(String id, Cons<UserData> c) {
        ApiRequest.get(Config.API_URL + "users/" + id, UserData.class, c);
    }

    public static PagingRequest<CommentData> getCommentsRequest(String type, String id) {
        // Construct URL assuming standard parameter naming
        String url = Config.API_URL + "comments?targetType=" + type + "&targetId=" + id;
        // PagingRequest automatically appends &page=...&size=...
        return new PagingRequest<>(CommentData.class, url);
    }

    public static void postComment(String type, String id, String content, Cons<String> callback) {
        String url = Config.API_URL + "comments";

        arc.struct.ObjectMap<String, String> map = new arc.struct.ObjectMap<>();
        map.put("targetType", type);
        map.put("targetId", id);
        map.put("content", content);

        // Ensure standard JSON format
        arc.util.serialization.Json jsonWriter = new arc.util.serialization.Json();
        jsonWriter.setOutputType(arc.util.serialization.JsonWriter.OutputType.json);
        String json = jsonWriter.toJson(map);

        AuthHttp.post(url, json)
                .error(e -> {
                    arc.util.Log.err("Failed to post comment", e);
                    callback.get("Network error: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
                })
                .submit(r -> {
                    if (r.getStatus() == arc.util.Http.HttpStatus.OK
                            || r.getStatus() == arc.util.Http.HttpStatus.CREATED) {
                        callback.get(null); // Success
                    } else {
                        callback.get("Server returned: " + r.getStatus().toString());
                    }
                });
    }

    public static void vote(String type, String id, Cons<String> callback) {
        // Assume /vote endpoint or similar
        String url = Config.API_URL + "vote";

        arc.struct.ObjectMap<String, String> map = new arc.struct.ObjectMap<>();
        map.put("targetType", type);
        map.put("targetId", id);

        arc.util.serialization.Json jsonWriter = new arc.util.serialization.Json();
        jsonWriter.setOutputType(arc.util.serialization.JsonWriter.OutputType.json);
        String json = jsonWriter.toJson(map);

        AuthHttp.post(url, json)
                .error(e -> callback.get("Network error: " + (e.getMessage() != null ? e.getMessage() : e.toString())))
                .submit(r -> {
                    if (r.getStatus() == arc.util.Http.HttpStatus.OK
                            || r.getStatus() == arc.util.Http.HttpStatus.CREATED) {
                        callback.get(null);
                    } else {
                        callback.get("Server returned: " + r.getStatus());
                    }
                });
    }
}
