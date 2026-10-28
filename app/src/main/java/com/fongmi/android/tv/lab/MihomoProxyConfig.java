package com.fongmi.android.tv.lab;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.security.MessageDigest;
import java.util.Arrays;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** Read-only discovery. No mode/rule writes and no use of the App proxy for controller/probes. */
final class MihomoProxyConfig {
    private static final int TIMEOUT_MS = 600;
    private static final Set<String> KEYS = Set.of("mixed-port", "port", "socks-port", "allow-lan",
            "bind-address", "external-controller", "secret");
    private static final OkHttpClient CONTROL = new OkHttpClient.Builder().proxy(Proxy.NO_PROXY)
            .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .callTimeout(1200, TimeUnit.MILLISECONDS).followRedirects(false).retryOnConnectionFailure(false).build();

    record Launch(File config, String controller, String secret, byte[] initialConfig) {
        static Launch command(String command, File cwd) throws IOException {
            List<String> args = new ArrayList<>();
            StringBuilder word = new StringBuilder();
            char quote = 0;
            boolean present = false, escaped = false;
            for (char c : command.toCharArray()) {
                if (escaped) { word.append(c); escaped = false; present = true; }
                else if (c == '\\' && quote != '\'') { escaped = true; present = true; }
                else if ((c == '\'' || c == '"') && (quote == 0 || quote == c)) {
                    quote = quote == 0 ? c : 0;
                    present = true;
                } else if (quote == 0 && Character.isWhitespace(c)) {
                    if (present) { args.add(word.toString()); word.setLength(0); present = false; }
                } else {
                    if ((quote == 0 && ";|&<>".indexOf(c) >= 0)
                            || (quote != '\'' && "$`".indexOf(c) >= 0)) throw new IOException("shell expansion/wrapper launch unsupported");
                    word.append(c); present = true;
                }
            }
            if (escaped || quote != 0) throw new IOException("unterminated Mihomo launch argument");
            if (present) args.add(word.toString());
            return parse(args, cwd);
        }

        static Launch parse(List<String> args, File cwd) throws IOException {
            int i = !args.isEmpty() && args.get(0).equals("exec") ? 1 : 0;
            if (i >= args.size() || !new File(args.get(i++)).getName().equalsIgnoreCase("mihomo")) {
                throw new IOException("unsupported Mihomo launch command");
            }
            String file = null, home = null, controller = null, secret = null;
            while (i < args.size()) {
                String option = args.get(i++);
                String value;
                int equals = option.indexOf('=');
                if (equals > 0) {
                    value = option.substring(equals + 1);
                    option = option.substring(0, equals);
                } else {
                    if (i >= args.size()) throw new IOException("unsupported Mihomo launch option");
                    value = args.get(i++);
                }
                switch (option) {
                    case "-f", "--config" -> file = value;
                    case "-d", "--dir" -> home = value;
                    case "-ext-ctl", "--ext-ctl" -> controller = value;
                    case "-secret", "--secret" -> secret = value;
                    case "-ext-ui", "--ext-ui" -> { }
                    default -> throw new IOException("unsupported Mihomo launch option");
                }
            }
            if (file == null && home == null) throw new IOException("explicit Mihomo config path required");
            File config = file != null ? resolve(cwd, file) : new File(resolve(cwd, home), "config.yaml");
            return new Launch(config, controller, secret, fingerprint(config));
        }

        private static File resolve(File cwd, String path) {
            File value = new File(path);
            return value.isAbsolute() ? value : new File(cwd, path);
        }
    }

    record Ready(Proxy proxy, String source) { }

    static final class Detector {
        private final Launch launch;
        private Proxy verified;
        private String signature;
        private long verifiedAt;
        private Map<String, String> fileScalars;
        private long fileModified = -1, fileLength = -1;
        private byte[] fileHash;

        Detector(Launch launch) { this.launch = launch; }
        Ready detect() throws IOException { return MihomoProxyConfig.detect(launch, this); }
        void invalidate() { verified = null; signature = null; }

        private Map<String, String> readFile() throws IOException {
            File file = launch.config();
            if (!file.isFile()) throw new IOException("Mihomo config missing");
            if (fileScalars == null || fileModified != file.lastModified() || fileLength != file.length()) {
                fileScalars = scalars(file);
                fileHash = fingerprint(file);
                fileModified = file.lastModified();
                fileLength = file.length();
            }
            return fileScalars;
        }
    }

    private static byte[] fingerprint(File file) throws IOException {
        if (!file.isFile() || file.length() > 8 * 1024 * 1024) throw new IOException("Mihomo config missing or too large");
        try { return MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath())); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IOException("SHA-256 unavailable"); }
    }

    static Ready detect(Launch launch) throws IOException {
        return detect(launch, null);
    }

    private static Ready detect(Launch launch, Detector detector) throws IOException {
        Map<String, String> file = detector == null ? scalars(launch.config()) : detector.readFile();
        String control = launch.controller() != null ? launch.controller() : scalar(file.getOrDefault("external-controller", ""));
        String secret = launch.secret() != null ? launch.secret() : scalar(file.getOrDefault("secret", ""));
        JsonObject config;
        InetSocketAddress controller = null;
        String source;
        if (!control.isEmpty()) {
            controller = controller(control);
            config = liveConfig(controller, secret);
            source = "controller";
        } else {
            if (!Arrays.equals(launch.initialConfig(), detector == null ? fingerprint(launch.config()) : detector.fileHash)) {
                throw new IOException("config changed without live controller; restart core before binding");
            }
            // Conservative fallback for simple top-level YAML. Never guess aliases, listeners, or defaults.
            config = new JsonObject();
            for (String key : List.of("mixed-port", "port", "socks-port", "allow-lan", "bind-address")) {
                if (file.containsKey(key)) config.addProperty(key, scalar(file.get(key)));
            }
            source = "yaml+protocol";
        }
        List<Proxy> candidates = candidates(config, controller);
        String signature = source + config;
        for (Proxy candidate : candidates) {
            try {
                long now = System.nanoTime();
                boolean cached = detector != null && candidate.type() == Proxy.Type.HTTP
                        && candidate.equals(detector.verified) && signature.equals(detector.signature)
                        && now - detector.verifiedAt < TimeUnit.SECONDS.toNanos(30);
                if (cached) {
                    try (Socket socket = new Socket(Proxy.NO_PROXY)) { socket.connect(candidate.address(), TIMEOUT_MS); }
                } else {
                    handshake(candidate);
                    if (detector != null) {
                        detector.verified = candidate;
                        detector.signature = signature;
                        detector.verifiedAt = now;
                    }
                }
                return new Ready(candidate, source);
            } catch (IOException ignored) {
                if (detector != null) detector.invalidate();
                // A different configured listener/protocol may still be ready.
            }
        }
        throw new IOException("no usable configured loopback HTTP/SOCKS listener (including auth/protocol check)");
    }

    static InetSocketAddress controller(String value) throws IOException {
        try {
            URI uri = URI.create("http://" + value);
            if (uri.getRawUserInfo() != null || uri.getPort() < 1 || uri.getPort() > 65535
                    || (uri.getPath() != null && !uri.getPath().isEmpty()) || uri.getQuery() != null) throw new IllegalArgumentException();
            String host = uri.getHost();
            if (host == null) throw new IllegalArgumentException();
            host = host.replace("[", "").replace("]", "");
            if (host.equals("0.0.0.0") || host.equals("localhost")) host = "127.0.0.1";
            if (host.equals("::")) host = "::1";
            InetAddress address = literalLoopback(host);
            return new InetSocketAddress(address, uri.getPort());
        } catch (Exception e) {
            throw new IOException("controller must be a valid local loopback/wildcard listen address");
        }
    }

    private static JsonObject liveConfig(InetSocketAddress controller, String secret) throws IOException {
        String host = controller.getAddress().getHostAddress();
        if (host.contains(":")) host = "[" + host + "]";
        Request.Builder request = new Request.Builder().url("http://" + host + ":" + controller.getPort() + "/configs");
        if (!secret.isEmpty()) request.header("Authorization", "Bearer " + secret);
        try (Response response = CONTROL.newCall(request.build()).execute()) {
            if (!response.isSuccessful() || response.body() == null) throw new IOException("controller unavailable/unauthorized");
            String json = response.body().string();
            if (json.length() > 1024 * 1024) throw new IOException("controller response too large");
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("invalid controller configuration response");
        }
    }

    static List<Proxy> candidates(JsonObject config, InetSocketAddress controller) throws IOException {
        List<Proxy> result = new ArrayList<>();
        try {
            boolean lan = config.has("allow-lan") && Boolean.parseBoolean(config.get("allow-lan").getAsString());
            String bind = lan && config.has("bind-address") ? config.get("bind-address").getAsString() : "127.0.0.1";
            List<String> hosts = bind.equals("*") || bind.equals("0.0.0.0") ? List.of("127.0.0.1", "::1")
                    : bind.equals("::") ? List.of("::1", "127.0.0.1") : List.of(bind);
            for (String key : List.of("mixed-port", "port", "socks-port")) {
                if (!config.has(key)) continue;
                String text = config.get(key).getAsString();
                if (!text.matches("[0-9]{1,5}")) throw new IOException("invalid configured proxy port");
                int port = Integer.parseInt(text);
                if (port == 0) continue;
                if (port > 65535) throw new IOException("invalid configured proxy port");
                if (controller != null && port == controller.getPort()) continue;
                for (String host : hosts) {
                    InetSocketAddress address = new InetSocketAddress(literalLoopback(host), port);
                    Proxy.Type type = key.equals("socks-port") ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
                    Proxy proxy = new Proxy(type, address);
                    if (!result.contains(proxy)) result.add(proxy);
                }
            }
            return result;
        } catch (RuntimeException e) {
            throw new IOException("invalid proxy listener configuration");
        }
    }

    private static InetAddress literalLoopback(String host) throws IOException {
        host = host.replace("[", "").replace("]", "");
        if (host.equalsIgnoreCase("localhost")) host = "127.0.0.1";
        // Do not resolve arbitrary names or LAN addresses while discovering a local endpoint.
        if (!host.matches("127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")
                && !host.equals("::1") && !host.equals("0:0:0:0:0:0:0:1")) throw new IOException("listener is not loopback reachable");
        InetAddress address = InetAddress.getByName(host);
        if (!address.isLoopbackAddress()) throw new IOException("listener is not loopback reachable");
        return address;
    }

    static void handshake(Proxy proxy) throws IOException {
        try (Socket socket = new Socket(Proxy.NO_PROXY)) {
            socket.connect(proxy.address(), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);
            if (proxy.type() == Proxy.Type.SOCKS) {
                socket.getOutputStream().write(new byte[]{5, 1, 0});
                socket.getOutputStream().flush();
                if (socket.getInputStream().read() != 5 || socket.getInputStream().read() != 0) {
                    throw new IOException("SOCKS5 listener does not accept no-auth loopback clients");
                }
            } else {
                // Mihomo acknowledges CONNECT before outbound dialing. No Internet test or mode change.
                socket.getOutputStream().write("CONNECT 127.0.0.1:1 HTTP/1.1\r\nHost: 127.0.0.1:1\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                socket.getOutputStream().flush();
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                StringBuilder status = new StringBuilder();
                int c;
                while (status.length() < 128 && (c = reader.read()) != -1 && c != '\n') status.append((char) c);
                if (!status.toString().matches("HTTP/1\\.[01] 200(?: .*)?\\r?")) throw new IOException("HTTP proxy CONNECT/auth check failed");
            }
        }
    }

    private static Map<String, String> scalars(File file) throws IOException {
        if (!file.isFile() || file.length() > 8 * 1024 * 1024) throw new IOException("Mihomo config missing or too large");
        Map<String, String> result = new HashMap<>();
        for (String line : new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).replace("\uFEFF", "").split("\\r?\\n")) {
            if (line.isEmpty() || Character.isWhitespace(line.charAt(0))) continue;
            int colon = line.indexOf(':');
            if (colon < 0) continue;
            String key = line.substring(0, colon).trim().replace("\"", "").replace("'", "");
            if (!KEYS.contains(key)) continue;
            if (result.put(key, line.substring(colon + 1).trim()) != null) throw new IOException("duplicate Mihomo routing scalar");
        }
        return result;
    }

    private static String scalar(String raw) throws IOException {
        // Only scalar metadata is read here; complex YAML stays owned by Mihomo.
        try {
            if (raw.startsWith("\"")) {
                int end = 1;
                for (; end < raw.length(); end++) {
                    if (raw.charAt(end) == '\\') end++;
                    else if (raw.charAt(end) == '"') break;
                }
                return JsonParser.parseString(raw.substring(0, end + 1)).getAsString();
            }
            if (raw.startsWith("'")) {
                StringBuilder value = new StringBuilder();
                for (int i = 1; i < raw.length(); i++) {
                    char c = raw.charAt(i);
                    if (c != '\'') value.append(c);
                    else if (i + 1 < raw.length() && raw.charAt(i + 1) == '\'') { value.append('\''); i++; }
                    else return value.toString();
                }
                throw new IllegalArgumentException();
            }
            String value = raw.replaceFirst("\\s+#.*$", "").trim();
            if (value.matches("^[&*!|>\\[{].*")) throw new IllegalArgumentException();
            return value.equals("null") || value.equals("~") ? "" : value;
        } catch (RuntimeException e) {
            throw new IOException("unsupported YAML routing scalar; use explicit controller launch flags");
        }
    }
}
