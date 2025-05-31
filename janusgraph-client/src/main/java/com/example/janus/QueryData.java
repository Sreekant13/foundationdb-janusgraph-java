package com.example.janus;

import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.Result;

import java.util.List;

public class QueryData {
  public static void main(String[] args) throws Exception {
    Cluster cluster = Cluster.build()
      .addContactPoint("node1.sreekant.nova-pg0.utah.cloudlab.us")
      .port(8182)
      .create();
    Client client = cluster.connect();

    // We will fetch component names
    List<Result> names = client.submit("g.V().hasLabel('component').values('name')").all().get();
    System.out.println("Components in graph:");
    names.forEach(r -> System.out.println("  " + r.getString()));

    // We will fetch what C1 depends on
    List<Result> deps = client.submit(
      "g.V().has('component','componentId','C1').out('dependsOn').valueMap()")
      .all().get();
    System.out.println("\nC1 depends on:");
    deps.forEach(r -> System.out.println("  " + r.getObject()));

    client.close();
    cluster.close();
  }
}
