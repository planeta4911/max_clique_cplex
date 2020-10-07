import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

import java.io.BufferedReader;
import java.io.FileReader;

public class Main {

    public static void main(String[] args) throws IloException {

        String name = "C250.9";
        Graph graph = new SingleGraph(name);

        try {
            BufferedReader reader = new BufferedReader(new FileReader("src/"+ name +".clq"));
            String s = reader.readLine();
            while (s!=null){
                if(s.startsWith("p")){
                    int nodeCnt = Integer.parseInt(s.split(" ")[2]);
                    for(int i = 1; i<nodeCnt+1; i++){
                        graph.addNode(String.valueOf(i));
                    }
                }
                else if(s.startsWith("e")){
                    String n1 = s.split(" ")[1];
                    String n2 = s.split(" ")[2];
                    graph.addEdge("Edge "+ n1 + " --- " + n2, n1, n2);
                }

                s = reader.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        IloCplex model = new IloCplex();
        IloIntVar[] y = model.boolVarArray(graph.getNodeCount());
        for (int i = 0; i < graph.getNodeCount(); i++){
            for (int j = 0; j < graph.getNodeCount(); j++){
                if(i!=j && !graph.getNode(i).hasEdgeBetween(j)){
                    IloRange leRange = model.le(model.sum(y[i], y[j]), 1);
                    model.add(leRange);
                }
            }
        }
        model.add(y);

        IloObjective max = model.maximize();
        max.setExpr(model.sum(y));
        model.add(max);

        model.solve();
        model.writeSolution("src/"+ name +".xml");

        System.out.println("\nResult of " + name +" is " + model.getObjValue());
    }

}
