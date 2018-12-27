package peergos.server.social;
import java.util.*;
import java.util.function.*;
import java.util.logging.*;

import peergos.server.util.*;

import com.sun.net.httpserver.*;
import peergos.shared.cbor.*;
import peergos.shared.corenode.*;
import peergos.shared.crypto.asymmetric.*;
import peergos.shared.crypto.hash.*;
import peergos.shared.social.*;
import peergos.shared.util.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class HttpSocialNetworkServer  {
	private static final Logger LOG = Logging.LOG();

    private static final boolean LOGGING = true;

    public static final String SOCIAL_URL = "social/";

    public static class SocialHandler implements HttpHandler
    {
        private final SocialNetwork social;

        public SocialHandler(SocialNetwork social) {
            this.social = social;
        }

        public void handle(HttpExchange exchange) throws IOException
        {
            long t1 = System.currentTimeMillis();
            DataInputStream din = new DataInputStream(exchange.getRequestBody());

            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);

            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/"))
                path = path.substring(1);
            String[] subComponents = path.substring(SOCIAL_URL.length()).split("/");
            String method = subComponents[0];
            Map<String, List<String>> params = HttpUtil.parseQuery(exchange.getRequestURI().getQuery());
            Function<String, String> last = key -> params.get(key).get(params.get(key).size() - 1);
//            LOG.info("social method "+ method +" from path "+ path);

            PublicKeyHash owner = PublicKeyHash.fromString(last.apply("owner"));
            try {
                switch (method) {
                    case "followRequest":
                        byte[] encryptedCap = Serialize.readFully(din, 1024);
                        boolean followRequested = social.sendFollowRequest(owner, encryptedCap).get();
                        dout.writeBoolean(followRequested);
                        break;
                    case "getFollowRequests":
                        byte[] signedTime = ArrayOps.hexToBytes(last.apply("auth"));
                        byte[] res = social.getFollowRequests(owner, signedTime).get();
                        Serialize.serialize(res, dout);
                        break;
                    case "removeFollowRequest":
                        byte[] signedFollowRequest = Serialize.readFully(din, 4096);
                        boolean isRemoved = social.removeFollowRequest(owner, signedFollowRequest).get();
                        dout.writeBoolean(isRemoved);
                        break;
                    default:
                        throw new IOException("Unknown method "+ method);
                }

                dout.flush();
                dout.close();
                byte[] b = bout.toByteArray();
                exchange.sendResponseHeaders(200, b.length);
                exchange.getResponseBody().write(b);
            } catch (Exception e) {
                Throwable cause = e.getCause();
                if (cause != null)
                    exchange.getResponseHeaders().set("Trailer", cause.getMessage());
                else
                    exchange.getResponseHeaders().set("Trailer", e.getMessage());

                exchange.sendResponseHeaders(400, 0);
            } finally {
                exchange.close();
                long t2 = System.currentTimeMillis();
                if (LOGGING)
                    LOG.info("Social Network server handled " + method + " request in: " + (t2 - t1) + " mS");
            }

        }
    }
}
