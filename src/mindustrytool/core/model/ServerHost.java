package mindustrytool.core.model;

import arc.util.Strings;

public class ServerHost {
    public String ip, name, error, last;
    public int port;
    public boolean wasValid;

    public boolean set(String ip) {
        if (ip.equals(last)) return wasValid;
        this.ip = error = null;
        this.port = 0;
        last = ip;
        if (ip.isEmpty()) {
            error = "@message.room.missing-host";
            return wasValid = false;
        }
        try {
            boolean isIpv6 = Strings.count(ip, ':') > 1;
            int idx;
            if (isIpv6 && (idx = ip.lastIndexOf("]:")) != -1 && idx != ip.length() - 1) {
                this.ip = ip.substring(1, idx);
                this.port = Integer.parseInt(ip.substring(idx + 2));
            } else if (!isIpv6 && (idx = ip.lastIndexOf(':')) != -1 && idx != ip.length() - 1) {
                this.ip = ip.substring(0, idx);
                this.port = Integer.parseInt(ip.substring(idx + 1));
            } else {
                error = "@message.room.missing-port";
                return wasValid = false;
            }
            if (port < 0 || port > 0xFFFF) throw new Exception();
            return wasValid = true;
        } catch (Exception e) {
            error = "@message.room.invalid-port";
            return wasValid = false;
        }
    }

    public String get() {
        return !wasValid ? "" : Strings.count(ip, ':') > 1 ? "[" + ip + "]:" + port : ip + ":" + port;
    }
}
