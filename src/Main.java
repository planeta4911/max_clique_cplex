import ilog.concert.*;
import ilog.cplex.IloCplex;
import org.apache.commons.math.util.MathUtils;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class Main {

    public static IloCplex model;
    private static int bestSolution = 0;
    private static double [] bestVars;
    public static IloNumVar[] vars;
    public static double eps = 10e-6;
    private static double launchTime;
    private static double stopTime;

    public static void main(String[] args) throws IloException {

        String name = "C125.9";
        Graph graph = new SingleGraph(name);


        try {
            BufferedReader reader = new BufferedReader(new FileReader("src/resources/"+ name +".clq"));
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

        model = new IloCplex();
        String[] names = new String[graph.getNodeCount()];
        for (int i = 0; i < names.length; i++) {
            names[i] = String.format("x_%d", i);
        }
        IloNumVar[] y = model.numVarArray(graph.getNodeCount(), 0L, 1L, names);

        int maxDegree = 0;
        for (int i = 0; i < graph.getNodeCount(); i++) {
            if (graph.getNode(i).getDegree() > maxDegree) {
                maxDegree = graph.getNode(i).getDegree();
            }
        }

        for (int i = 0; i < graph.getNodeCount(); i++){
            for (int j = 0; j < graph.getNodeCount(); j++){
                if(i!=j && !graph.getNode(i).hasEdgeBetween(j)){
                    IloRange leRange = model.le(model.sum(y[i], y[j]), 1);
                    model.add(leRange);
                }
            }
        }

        model.add(model.le(model.sum(y), maxDegree));
        model.add(y);

        IloObjective max = model.maximize();
        max.setExpr(model.sum(y));
        model.add(max);

        vars = y;

        launchTime = System.currentTimeMillis();
        stopTime = launchTime + (60 * 60 * 1000);

        try {
            branchAndBound();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("\n" + name);
            System.out.println("===============================");
            System.out.println("Time elapsed: " + (System.currentTimeMillis() - launchTime) / 1000F);
            System.out.println("Result is " + bestSolution);
            System.out.println("Indexes of nodes for best solution:");
            ArrayList<Integer> bestVarsInd = new ArrayList<>();
            if (bestVars != null) {
                for (int i = 0; i < bestVars.length; i++) {
                    if (bestVars[i] == 1) {
                        bestVarsInd.add(i + 1);
                    }
                }
            }
            for(int var : bestVarsInd){
                System.out.print(var + " ");
            }
        }
    }

    public static void branchAndBound() throws IloException, InterruptedException {

        if (System.currentTimeMillis() >= stopTime) {
            throw new InterruptedException();
        }

        model.solve();
        double newObj = model.getObjValue();
        double[] newVars = model.getValues(vars);

        if (isWorse(newObj)) {
            return;
        }

        if (isInteger(newVars)) {
            bestSolution = (int) Math.round(newObj);
            bestVars = newVars;
            return;
        }
        int ind = branching(newVars);


        IloRange ub = addBound(ind, newVars, true);
        branchAndBound();
        model.remove(ub);
        IloRange lb = addBound(ind, newVars, false);
        branchAndBound();
        model.remove(lb);
    }

    public static int branching(double[] newVars) {
        for (int i=0; i<newVars.length; i++) {
            if (MathUtils.compareTo(Math.round(newVars[i]), newVars[i], eps) != 0) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isInteger(double[] s) {
        for (double var : s) {
            long rounded = Math.round(var);
            if (MathUtils.compareTo(rounded, var, eps) != 0) {
                return false;
            }
        }
        return true;
    }

    public static boolean isWorse(double s) {
        if (Math.abs(s - Math.round(s)) < eps) {
            return MathUtils.compareTo(Math.round(s), bestSolution, eps) <= 0;
        }
        return MathUtils.compareTo(Math.floor(s), bestSolution, eps) <= 0;
    }


    public static IloRange addBound(int ind, double[] newVars, boolean isUpper) throws IloException {
        IloRange constr;
        if (isUpper) {
            double ceil = Math.ceil(newVars[ind]);
            constr = model.range(ceil, vars[ind], ceil);

        } else {
            double floor = Math.floor(newVars[ind]);
            constr = model.range(floor, vars[ind], floor);
        }
        model.add(constr);
        return constr;
    }

}
