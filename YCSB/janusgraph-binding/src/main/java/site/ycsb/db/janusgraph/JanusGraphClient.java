package site.ycsb.db.janusgraph;

import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;

import site.ycsb.*;

import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.HashMap;

/**
 * JanusGraphClient is a YCSB binding to JanusGraph over Gremlin Server.
 * It implements basic CRUD operations using TinkerPop's traversal API.
 */
public class JanusGraphClient extends DB {

  private GraphTraversalSource g;
  private Cluster cluster;

  @Override
  public void init() throws DBException {
    try {
      String confPath = getProperties().getProperty("janusgraph.remote.config", "conf/remote-objects.yaml");
      cluster = Cluster.open(confPath);
      g = AnonymousTraversalSource.traversal().withRemote(DriverRemoteConnection.using(cluster));
    } catch (Exception e) {
      throw new DBException("Failed to initialize JanusGraph connection", e);
    }
  }

  @Override
  public void cleanup() throws DBException {
    try {
      if (g != null) {
        g.close();
      }
      if (cluster != null) {
        cluster.close();
      }
    } catch (Exception e) {
      throw new DBException("Error closing connection", e);
    }
  }

  @Override
  public Status insert(String table, String key, Map<String, ByteIterator> values) {
    try {
      Vertex v = g.addV(table).property("userid", Integer.parseInt(key)).next();
      for (Map.Entry<String, ByteIterator> e : values.entrySet()) {
        v.property(e.getKey(), e.getValue().toString());
      }
      return Status.OK;
    } catch (Exception e) {
      System.err.println("Insert failed for key " + key + ": " + e.getMessage());
      return Status.ERROR;
    }
  }

  @Override
  public Status read(String table, String key, Set<String> fields, Map<String, ByteIterator> result) {
    try {
      Vertex v = g.V().hasLabel(table).has("userid", Integer.parseInt(key)).next();
      if (v == null) {
        return Status.NOT_FOUND;
      }

      v.properties().forEachRemaining(p -> {
          if (fields == null || fields.contains(p.key())) {
            result.put(p.key(), new StringByteIterator(p.value().toString()));
          }
        });
      return Status.OK;
    } catch (Exception e) {
      System.err.println("Read failed for key " + key + ": " + e.getMessage());
      return Status.ERROR;
    }
  }

  @Override
  public Status update(String table, String key, Map<String, ByteIterator> values) {
    try {
      Vertex v = g.V().hasLabel(table).has("userid", Integer.parseInt(key)).next();
      if (v == null) {
        return Status.NOT_FOUND;
      }

      for (Map.Entry<String, ByteIterator> e : values.entrySet()) {
        v.property(e.getKey(), e.getValue().toString());
      }
      return Status.OK;
    } catch (Exception e) {
      System.err.println("Update failed for key " + key + ": " + e.getMessage());
      return Status.ERROR;
    }
  }

  @Override
  public Status delete(String table, String key) {
    try {
      g.V().hasLabel(table).has("userid", Integer.parseInt(key)).drop().iterate();
      return Status.OK;
    } catch (Exception e) {
      System.err.println("Delete failed for key " + key + ": " + e.getMessage());
      return Status.ERROR;
    }
  }

  @Override
  public Status scan(String table, String startKey, int recordCount,
                     Set<String> fields, Vector<HashMap<String, ByteIterator>> result) {
    System.err.println("Scan not implemented for JanusGraph.");
    return Status.NOT_IMPLEMENTED;
  }
}

