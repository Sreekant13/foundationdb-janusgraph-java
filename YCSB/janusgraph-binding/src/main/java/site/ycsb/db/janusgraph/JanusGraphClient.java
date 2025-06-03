package site.ycsb.db.janusgraph;

import site.ycsb.DB;
import site.ycsb.DBException;
import site.ycsb.ByteIterator;
import site.ycsb.Status;
import site.ycsb.StringByteIterator;

import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * JanusGraphClient implements the YCSB DB interface for JanusGraph via a Gremlin Server.
 * Each YCSB table becomes a JanusGraph vertex label, with "id" as the primary key
 * and other fields as vertex properties.
 */
public class JanusGraphClient extends DB {

  private Client gremlinClient;
  private GraphTraversalSource g;

  @Override
  public void init() throws DBException {
    String gremlinHost = getProperties().getProperty("janusgraph.host", "127.0.0.1");
    String gremlinPort = getProperties().getProperty("janusgraph.port", "8182");
    try {
      Cluster cluster = Cluster.build()
          .addContactPoint(gremlinHost)
          .port(Integer.parseInt(gremlinPort))
          .create();
      gremlinClient = cluster.connect();
      g = AnonymousTraversalSource.traversal()
          .withRemote(DriverRemoteConnection.using(gremlinClient));
    } catch (Exception e) {
      throw new DBException("Could not connect to JanusGraph", e);
    }
  }

  @Override
  public void cleanup() throws DBException {
    try {
      if (g != null) {
        g.close();
      }
      if (gremlinClient != null) {
        gremlinClient.close();
      }
    } catch (Exception e) {
      throw new DBException("Error closing JanusGraph connection", e);
    }
  }

  @Override
  public Status read(
      String table,
      String key,
      Set<String> fields,
      Map<String, ByteIterator> result) {
    try {
      Vertex v = g.V().hasLabel(table).has("id", key).next();
      if (v == null) {
        return Status.NOT_FOUND;
      }
      if (fields == null || fields.isEmpty()) {
        v.properties().forEachRemaining(prop -> {
            result.put(
                prop.key(),
                new StringByteIterator(prop.value().toString())
            );
          });
      } else {
        for (String f : fields) {
          Object val = v.property(f).orElse(null);
          if (val != null) {
            result.put(f, new StringByteIterator(val.toString()));
          }
        }
      }
      return Status.OK;
    } catch (Exception e) {
      return Status.ERROR;
    }
  }

  @Override
  public Status insert(
      String table,
      String key,
      Map<String, ByteIterator> values) {
    try {
      Vertex v = g.addV(table).property("id", key).next();
      for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
        v.property(entry.getKey(), entry.getValue().toString());
      }
      return Status.OK;
    } catch (Exception e) {
      return Status.ERROR;
    }
  }

  @Override
  public Status update(
      String table,
      String key,
      Map<String, ByteIterator> values) {
    try {
      Vertex v = g.V().hasLabel(table).has("id", key).next();
      if (v == null) {
        return Status.NOT_FOUND;
      }
      for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
        v.property(entry.getKey(), entry.getValue().toString());
      }
      return Status.OK;
    } catch (Exception e) {
      return Status.ERROR;
    }
  }

  @Override
  public Status delete(
      String table,
      String key) {
    try {
      g.V().hasLabel(table).has("id", key).drop().iterate();
      return Status.OK;
    } catch (Exception e) {
      return Status.ERROR;
    }
  }

  @Override
  public Status scan(
      String table,
      String startKey,
      int recordCount,
      Set<String> fields,
      Vector<HashMap<String, ByteIterator>> result) {
    // Scan is not implemented for this binding.
    return Status.NOT_IMPLEMENTED;
  }
}
