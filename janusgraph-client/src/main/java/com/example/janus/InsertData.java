package com.example.janus;

import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;

public class InsertData {
  public static void main(String[] args) throws Exception {
    // 1) We will use this to connect to Gremlin Server
    Cluster cluster = Cluster.build()
      .addContactPoint("node1.sreekant.nova-pg0.utah.cloudlab.us") 
      .port(8182)
      .create();
    Client client = cluster.connect();

    // 2) This will be our One‐shot traversal: add two components + edge, then commit
    String gremlin = ""
      + "import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.*;\n"
      + "g.addV('component').property('componentId','C1').property('name','FrontEnd').as('c1')"
      + ".addV('component').property('componentId','C2').property('name','BackEnd').as('c2')"
      + ".addE('dependsOn').from('c1').to('c2').iterate();"
      + "graph.tx().commit();";

    client.submit(gremlin).all().get();
    System.out.println("Data inserted successfully.");

    client.close();
    cluster.close();
  }
}
