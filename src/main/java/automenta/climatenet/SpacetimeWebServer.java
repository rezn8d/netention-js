/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import automenta.climatenet.elastic.ElasticSpacetime;
import static automenta.climatenet.ImportKML.json;
import automenta.climatenet.p2p.TomPeer;
import automenta.knowtention.Channel;
import static automenta.knowtention.Core.newJson;
import automenta.knowtention.WebSocketCore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import static io.undertow.Handlers.resource;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.util.Headers;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author me
 */
public class SpacetimeWebServer extends PathHandler {
    public static final Logger logger = LoggerFactory.getLogger(SpacetimeWebServer.class);
    
    final ElasticSpacetime db;
    final String clientPath = "./src/web";
    private List<String> paths = new ArrayList();

    public class SourceIndex extends Channel {

        public SourceIndex() {
            super("source/index");
        }

        @Override
        public synchronized ObjectNode commit() {
            
            return super.commit( json(db.rootLayers()) );
        }

        
    }
    
    /** wraps a channel as an HTTP handler which returns a snapshot (text) JSON representation */
    public static class ChannelSnapshot implements HttpHandler {
        private final Channel channel;
    
        public ChannelSnapshot(Channel c) {
            super();
            this.channel = c;
        }

        @Override
        public void handleRequest(HttpServerExchange hse) throws Exception { 
            send(hse, channel.commit());                
        }
                
    }
    
    
    public SpacetimeWebServer(ElasticSpacetime db, int port) throws Exception {
        this(db, "localhost", port);
    }

    public SpacetimeWebServer(final ElasticSpacetime db, String host, int port) throws Exception {
        this.db = db;

        Undertow server = Undertow.builder()                
                .addHttpListener(port, host)
                .setIoThreads(8)
                .setHandler(this)
                .build();

        //https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/sessionhandling/SessionServer.java
        //addPrefixPath("/ws", websocket(new WebSocketConnector(core)).addExtension(new PerMessageDeflateHandshake()))
        
        addPrefixPath("/", resource(
                new FileResourceManager(new File(clientPath), 100)).
                setDirectoryListingEnabled(false));

        SourceIndex sourceIndex = new SourceIndex();
        
        addPrefixPath("/socket", new WebSocketCore(
                new SourceIndex()
        ).handler());
        
        addPrefixPath("/layer/index", new ChannelSnapshot(sourceIndex));
        
        addPrefixPath("/layer/meta", new HttpHandler() {

            @Override
            public void handleRequest(HttpServerExchange ex) throws Exception {
                Map<String, Deque<String>> reqParams = ex.getQueryParameters();

                //Deque<String> deque = reqParams.get("attrName");
                //Deque<String> dequeVal = reqParams.get("value");
                Deque<String> idArray = reqParams.get("id");

                ArrayNode a = json.readValue(idArray.getFirst(), ArrayNode.class);

                String[] ids = new String[a.size()];
                int j = 0;
                for (JsonNode x : a) {
                    ids[j++] = x.textValue();
                }
                QueryBuilder qb = QueryBuilders.termsQuery("_id", ids);

                SearchResponse response = db.search(qb, 0, 60);

                sendLayers(response, ex);

            }

        });

        addPrefixPath("/geoCircle", new HttpHandler() {

            @Override
            public void handleRequest(final HttpServerExchange ex) throws Exception {
                Map<String, Deque<String>> reqParams = ex.getQueryParameters();

                //   Deque<String> deque = reqParams.get("attrName");
                //Deque<String> dequeVal = reqParams.get("value");
                Deque<String> lats = reqParams.get("lat");
                Deque<String> lons = reqParams.get("lon");
                Deque<String> rads = reqParams.get("radiusM");

                if (lats != null && lons != null && rads != null) {
                    //System.out.println(lats.getFirst() + "  "+ lons.getFirst() + " "+ rads.getFirst());
                    double lat = Double.parseDouble(lats.getFirst());
                    double lon = Double.parseDouble(lons.getFirst());
                    double rad = Double.parseDouble(rads.getFirst());

                    SearchHits result = db.search(lat, lon, rad, 60);

                    XContentBuilder d = jsonBuilder().startObject();

                    for (SearchHit h : result) {

                        Map<String, Object> s = h.getSource();

                        //System.out.println(h.getId() + " " + s);
                        d.startObject(h.getId())
                                .field("path", s.get("path"))
                                .field("name", s.get("name"))
                                .field("description", s.get("description"))
                                .field("geom", s.get("geom"));
                        d.endObject();

                    }
                    d.endObject();

                    send(ex, d);

                }
                ex.getResponseSender().send("");

            }

        });
        
        
        server.start();
        logger.info("Started web server @ " + host + ":" + port + "\n  " + paths);
        
    }

    @Override
    public synchronized PathHandler addPrefixPath(String path, HttpHandler handler) {
        paths.add(path);
        return super.addPrefixPath(path, handler);
    }
    

    public static void main(String[] args) throws Exception {
        int webPort = 9090;
        int p2pPort = 9091;
        String peerID = UUID.randomUUID().toString();
        
        SpacetimeWebServer s = new SpacetimeWebServer(
                ElasticSpacetime.server("cv", false),
                webPort);

        TomPeer peer = new TomPeer(
                new PeerBuilderDHT(new PeerBuilder(Number160.createHash(peerID)).ports(p2pPort).start()).start());
        peer.add(s.db);

    }

    
    public static void send(HttpServerExchange ex, JsonNode d) {
        ex.startBlocking();

        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");

        try {
            json.writeValue(ex.getOutputStream(), d);
        } catch (IOException ex1) {
            logger.warn(ex1.toString());
        }
        
        ex.getResponseSender().close();
    }
    
    public static void send(HttpServerExchange ex, XContentBuilder d) {
        ex.startBlocking();

        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");

        ex.getResponseSender().send(d.bytes().toChannelBuffer().toByteBuffer());
        ex.getResponseSender().close();
    }

    public static boolean sendLayers(SearchResponse response, HttpServerExchange ex) throws IOException {
        SearchHits result = response.getHits();
        if (result.totalHits() == 0) {
            ex.getResponseSender().send("");
            return true;
        }
        XContentBuilder d = jsonBuilder().startObject();
        for (SearchHit h : result) {

            Map<String, Object> s = h.getSource();

            d.startObject(h.getId())
                    .field("name", s.get("name"));
            Object desc = s.get("description");
            if (desc != null) {
                d.field("description", s.get("description"));
            }
            d.endObject();

        }
        d.endObject();
        send(ex, d);
        return false;
    }

    public static ObjectNode json(SearchResponse response) {
        SearchHits result = response.getHits();
        ObjectNode o = newJson.objectNode();
        if (result.totalHits() == 0) {            
            return o;
        }        
        for (SearchHit h : result) {

            Map<String, Object> s = h.getSource();

            ObjectNode p = newJson.objectNode();
            o.put(h.getId(), p);
            
            p.put("name", s.get("name").toString());
            
            Object desc = s.get("description");
            if (desc != null)
                p.put("description", s.get("description").toString());
            
        }        
        
        return o;
        
    }
    
}
