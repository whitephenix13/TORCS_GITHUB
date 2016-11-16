import org.encog.Encog;
import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.neural.data.NeuralDataSet;
import org.encog.neural.data.basic.BasicNeuralData;
import org.encog.neural.data.basic.BasicNeuralDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.Train;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import scr.SensorModel;

import java.io.*;
import java.util.*;

public class NeuralNetwork implements Serializable {

    private static final long serialVersionUID = -88L;

    private BufferedReader reader;
    private List<String> lines = null;
    List<String> result = null;
    String line = null;
    BasicNetwork network = null;

    NeuralNetwork(int inputs, int hidden, int outputs) {

        // prepare training data, removing the first line of every .csv files
        lines = new ArrayList<>();

        //prepareData("train_data/aalborg.csv");
        //prepareData("train_data/alpine-1.csv");
        //prepareData("train_data/f-speedway.csv");
        //prepareData("A_Speedway_34_52.csv");
        //prepareData("A_Speedway_34_52_2.csv");
        //prepareData("Corkscrew_01_26_01.csv");
        prepareData("Michigan_41_65.csv");

        double TORCS_INPUT[][] = new double[lines.size()][22];
        double TORCS_IDEAL[][] = new double[lines.size()][3];

        // prepare the input
        for(int i = 0; i < lines.size(); i++) {
            String data[] = lines.get(i).split(",");
            for(int j = 3; j < data.length; j ++) {
                TORCS_INPUT[i][j-3] = Double.parseDouble(data[j]);
            }
        }

        //prepare the output
        for(int i = 0; i < lines.size(); i++) {
            String data[] = lines.get(i).split(",");
            for(int j = 0; j < 3; j ++) {
                TORCS_IDEAL[i][j] = Double.parseDouble(data[j]);
            }
        }

        // intialize the training set
        NeuralDataSet trainingSet = new BasicNeuralDataSet(TORCS_INPUT, TORCS_IDEAL);

        // setup the network
        network = new BasicNetwork();
        network.addLayer(new BasicLayer(new ActivationSigmoid(), true, inputs));
        network.addLayer(new BasicLayer(new ActivationSigmoid(), true, hidden));
        network.addLayer(new BasicLayer(new ActivationSigmoid(), true, outputs));
        network.getStructure().finalizeStructure();
        network.reset();

        // train the network
        final Train train = new ResilientPropagation(network, trainingSet);
        int epoch = 1;
        do {
            train.iteration();
            System.out.println("Epoch #" + epoch + " Error:" + train.getError());
            epoch++;

        } while(train.getError() > 0.0001 && epoch < 5000);

        //printTrainingResult(trainingSet);

        Encog.getInstance().shutdown();
    }

    public double getOutput(SensorModel a) {
        return 0.5;
    }

    //Store the state of this neural network
    public void storeGenome() {
        ObjectOutputStream out = null;
        try {
            //create the memory folder manually
            out = new ObjectOutputStream(new FileOutputStream("memory/mydriver.mem"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.writeObject(this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load a neural network from memory
    public NeuralNetwork loadGenome() {

        // Read from disk using FileInputStream
        FileInputStream f_in = null;
        try {
            f_in = new FileInputStream("memory/mydriver.mem");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Read object using ObjectInputStream
        ObjectInputStream obj_in = null;
        try {
            obj_in = new ObjectInputStream(f_in);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Read an object
        try {
            if (obj_in != null) {
                return (NeuralNetwork) obj_in.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void prepareData(String filename) {
        // data preparation
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            reader.readLine(); // this will read the first line
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printTrainingResult(NeuralDataSet trainingSet) {
        System.out.println("Neural Network Results:");
        result = new ArrayList<String>();
        for (MLDataPair neuralDataSet : trainingSet) {
            MLData output = network.compute(neuralDataSet.getInput());
            result.add(output.getData(0) + "," + output.getData(1) + "," + output.getData(2));

            System.out.println("predicted=" + output.getData(0) + " " + output.getData(1) + " " + output.getData(2)
                    + ",expected=" + neuralDataSet.getIdeal().getData(0) + " " + neuralDataSet.getIdeal().getData(1)
                    + " " + neuralDataSet.getIdeal().getData(2));
        }
    }

}
