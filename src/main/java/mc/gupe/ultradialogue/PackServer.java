package mc.gupe.ultradialogue;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/** Mini-servidor web que sirve el resource pack a los jugadores, sin hostearlo en ningun lado. */
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
