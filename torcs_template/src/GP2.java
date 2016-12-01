//import java.io.BufferedReader;
//import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.io.IOException;
//import java.lang.reflect.Array;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Random;
//
//import static cern.clhep.Units.s;
//import static hep.aida.bin.BinFunctions1D.max;
//
///**
// * Created by daniel on 18-11-2016.
// */
//public class GP2{
//
//    public ArrayList<double[]> population = new ArrayList<>();
//    int populationSize = 100;
//    int numCrossover = 20;
//    double mutationStrength = 0.1;
//    int survivors = 20;
//
//    public GP2(double[] initialSetting) {
//        population.add(initialSetting);
//
//        for(int i = 0; i<populationSize-1; i++){
//            population.add(mutation(initialSetting));
//        }
//    }
//
//    public void nextGeneration(double[] lapTimes){
//        int mutations = populationSize - numCrossover - survivors;;
//
//        // keep fittest species
////        Arrays.sort(lapTimes);
////        TODO map
//        // insert new mutations
//        Random r = new Random();
//        for(int i = 0; i<mutations; i++){
//            int randomSpecies = r.nextInt(population.size());
//            population.add(mutation(population.get(randomSpecies)));
//        }
//
//        // insert crossovers
//        for(int i = 0; i<numCrossover; i++){
//            int randomSpecies1 = r.nextInt(population.size());
//            int randomSpecies2 = r.nextInt(population.size());
//            while(randomSpecies2==randomSpecies1){
//                randomSpecies2 = r.nextInt(population.size());
//            }
//            double[] species1 = population.get(randomSpecies1);
//            double[] species2 = population.get(randomSpecies2);
//            population.add(crossOver(species1, species2));
//        }
//    }
//
//
//    private double[] mutation(double[] fenotype) {
//        Random r = new Random();
//        double rangeMin = 1 - mutationStrength;
//        double rangeMax = 1 + mutationStrength;
//        double randomValue;
//
//        double[] newFenotype = fenotype.clone();
//
//        for(int i=0;i<fenotype.length;i++){
//            randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble();
//            newFenotype[i] = fenotype[i] * randomValue;
//        }
//        return newFenotype;
//    }
//
//    private double[] crossOver(double[] fenotype1, double[] fenotype2) {
//        Random r = new Random();
//
//        // deep copy
//        double[] newFenotype = fenotype1.clone();
//        int crossPoint=1;
//        if(fenotype1.length > 2)
//            crossPoint = r.nextInt(fenotype1.length-2)+1;
//
//        for(int i=0;i<crossPoint;i++){
//            newFenotype[i] = fenotype2[i];
//        }
//        return newFenotype;
//    }
//}
