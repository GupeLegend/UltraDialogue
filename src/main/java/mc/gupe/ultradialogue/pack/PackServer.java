package mc.gupe.ultradialogue.pack;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/** Small web server that serves the resource pack to players, without hosting it anywhere. */
public final class PackServer {

    private HttpServer server;

    public void start(int port, String path, byte[] data) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(path, exchange -> {
            try {
                exchange.getResponseHeaders().set("Content-Type", "application/zip");
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(data);
                }
            } catch (Throwable t) {
                exchange.close();
            }
        });
        server.setExecutor(null);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }
}
