package automenta.knowtention;

import automenta.knowtention.EventEmitter.EventObserver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.diff.JsonDiff;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;

/**
 * Holds working data in the form of a JSON symbol tree
 * @see https://github.com/fge/json-patch
 */
public class Channel extends EventEmitter implements Serializable, Iterable<JsonNode> {
    
    private ObjectNode prev;
    protected ObjectNode root;
    private boolean inTransaction;
    public final String id;
    
    public abstract static class ChannelChange implements EventObserver {  

        @Override  public void event(Class event, Object[] args) {
            event((Channel)args[0], (JsonPatch)args[1]);
        }
        
        abstract public void event(Channel c, JsonPatch p);
        
    }
    


    public Channel(String id) {
        super();
        this.id = id;
        this.root = JsonNodeFactory.instance.objectNode();
        this.root.put("id", id);
    }
    
    public Channel(ObjectNode node) {
        super();
        this.root = node;
        this.id = node.get("id").asText();
    }

    @Override
    public String toString() {
        return root.toString();
    }

    synchronized void applyPatch(JsonPatch patch) throws JsonPatchException {
        set((ObjectNode) patch.apply(root) );
    }
    
    synchronized public Channel set(final ObjectNode newRoot) {
        
        tx(new JTransaction() {
            @Override public ObjectNode run() {
                return newRoot;
            }            
        });

        return this;
    }
    
    public Channel set(String jsonContent) {
        try {
            
            set( Core.json.readValue(jsonContent, ObjectNode.class) );
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return this;
    }

    public ObjectNode get() {
        return root;
    }
    
    
    public ObjectNode getSnapshot() {
        return get().deepCopy();
    }
    
    /** add vertex */
    public boolean addVertex(ObjectNode vertex) {
        if (vertex == null) return false;
        
        if (!get().has("nodes")) {
            get().put("nodes", Core.newJson.arrayNode());
        }
        
        ArrayNode node = (ArrayNode) get().get("nodes");
        
        if (!vertex.has("id")) {
            vertex.put("id", Core.uuid());
        }
        
        removeVertex(vertex.get("id").asText());
        
        node.add(vertex);
        
        commit();
        return true;
    }
    
    /** wont be necessary if vertex is changed to objectnode, not array */
    @Deprecated protected void removeVertex(String id) {
        ArrayNode node = (ArrayNode) get().get("nodes");
        for (int i = 0; i < node.size(); i++) {
            JsonNode v = node.get(i);
            if (v.get("id").asText().equals(id)) {
                node.remove(i);
                break;
            }
        }
    }    
    
    public void addEdge(ObjectNode edge) {
        
    }
    
    /* patch may be null */
    protected void emitChange(JsonNode patch) {
        
        //core.emit(ChannelChange.class, this, patch);
        emit(ChannelChange.class, this, patch);
    }
    
    public interface JTransaction {
        public ObjectNode run();
    }
    
    /** begin transaction */
    public synchronized void tx(JTransaction r) {
        
        inTransaction = true;
        
        //save snapshot
        if (get()!=null)
            prev = getSnapshot();
        else
            prev = null;
        
        commit( r.run() );
        
        inTransaction = false;
        
    }
    
    public ObjectNode commit() {
        return commit(root);
    }
    
    /** end transaction */
    public synchronized ObjectNode commit(ObjectNode next) {
        if (inTransaction)
            return prev;

        if (next!=null) {
            prev = root;
            root = next;
        }
        
        root.put("id",id);
        
        JsonNode patch = null;
                
        if (prev!=null) {            
            patch = JsonDiff.asJson(prev, root);
            prev = null;
        }
        
        //dont emit if patch is empty
        if (patch!=null && patch.size() == 0)
            return root;
        
        emitChange(patch);
        
        return root;
    }

    @Override
    public Iterator<JsonNode> iterator() {
        return get().iterator();
    }
    
    
}
