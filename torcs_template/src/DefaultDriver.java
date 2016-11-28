import cicontest.algorithm.abstracts.AbstractDriver;
import cicontest.algorithm.abstracts.DriversUtils;
import cicontest.torcs.controller.extras.ABS;
import cicontest.torcs.controller.extras.AutomatedClutch;
import cicontest.torcs.controller.extras.AutomatedGearbox;
import cicontest.torcs.controller.extras.AutomatedRecovering;
import cicontest.torcs.genome.IGenome;
import scr.Action;
import scr.SensorModel;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DefaultDriver extends AbstractDriver {

    private TRACK_NAME trackName;
    // test
    private NeuralNetwork neuralNetwork;

    // arrows pressed and released
    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean rightPressed = false;
    private boolean leftPressed = false;
    private boolean upReleased = false;
    private boolean downReleased = false;
    private boolean rightReleased = false;
    private boolean leftReleased = false;

    private boolean currentSteeringRight = false;
    private boolean currentSteeringLeft = false;

    private double rightCumulSteering=0.0D;
    private double leftCumulSteering=0.0D;
    FocusFrame focusFrame;

    private PrintWriter pw;
    private BufferedReader reader;

    List<String> lines = null;
    private int count;

    // change this value to choose whether to simulate or control the car
    private boolean simulate = true;

    // change this value to simulate test on trained neural net
    private boolean testNeural = false;
    private boolean trainNeural = false;
    private boolean saveNeural = false;
    private boolean furthestSensor = true;
    private double[] previous_outputs={0.0D,0.0D,0.0D};

    // for GP
//    private boolean GPtest = true;
//    private int speciesCounter = 0;
//    GP gp = new GP();
//    private ArrayList<ArrayList<String>> population = gp.evolve();
//    private int current_iter = 0;
//    private int iterations = 5;
//    private double[] lapTimes = new double[99];



    // change this value to determine how many hidden layers and the size of it
    private int[] layersConfig = {50, 50, 25, 25};

    //Use to know if this is the first time we enter the controller:
    boolean raceStarted = false;
    double startTime;

    public DefaultDriver() {
        initialize();
        if(trainNeural)
        {
            neuralNetwork = new NeuralNetwork(25, layersConfig, 3);
            //String[] trainingSetNames = {trackName.A_SPEEDWAY,trackName.CORKSCREW,trackName.E_TRACK2};
            String[] trainingSetNames = {trackName.A_SPEEDWAY, trackName.MICHIGAN, trackName.GC_TRACK2, trackName.FORZA,
                    trackName.E_ROAD, trackName.STREET1, trackName.CORKSCREW, trackName.E_TRACK6, trackName.E_TRACK2};            //String[] trainingSetNames = {"train_data/f-speedway.csv","train_data/aalborg.csv","train_data/alpine-1.csv"};
            //neuralNetwork.Train(trainingSetNames);
            //,"Corkscrew_01_26_01.csv","Michigan_41_65.csv","GC_track2_59_74.csv"
            neuralNetwork.Train(trainingSetNames);
            if(saveNeural)
                neuralNetwork.storeGenome();
        }
        else
        {
            neuralNetwork=new NeuralNetwork(true);
        }
        if(!simulate && !testNeural) {
            focusFrame = new FocusFrame();
            focusFrame.requestFocus();
        }

        try {
            if(!simulate && !testNeural) {
                pw = new PrintWriter(new File("test"+".csv"));
                pw.println("ACCELERATION,BRAKE,STEERING,SPEED,TRACK_POSITION,ANGLE_TO_TRACK_AXIS,TRACK_EDGE_0,TRACK_EDGE_1,TRACK_EDGE_2," +
                        "TRACK_EDGE_3,TRACK_EDGE_4,TRACK_EDGE_5,TRACK_EDGE_6,TRACK_EDGE_7,TRACK_EDGE_8,TRACK_EDGE_9,TRACK_EDGE_10," +
                        "TRACK_EDGE_11,TRACK_EDGE_12,TRACK_EDGE_13,TRACK_EDGE_14,TRACK_EDGE_15,TRACK_EDGE_16,TRACK_EDGE_17,TRACK_EDGE_18");
            } else if(simulate && !testNeural) {
                reader = new BufferedReader(new FileReader(trackName.A_SPEEDWAY+".csv"));
                lines = new ArrayList<String>();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initialize() {
        this.enableExtras(new AutomatedClutch());
        this.enableExtras(new AutomatedGearbox());
        this.enableExtras(new AutomatedRecovering());
        this.enableExtras(new ABS());
    }

    @Override
    public void loadGenome(IGenome genome) {
        if (genome instanceof DefaultDriverGenome) {
            DefaultDriverGenome myGenome = (DefaultDriverGenome) genome;
        } else {
            System.err.println("Invalid Genome assigned");
        }
    }

    @Override
    public double getAcceleration(SensorModel sensors) {
        double[] sensorArray = new double[4];
        double output = neuralNetwork.getOutput(sensors);
        return 1;
    }

    @Override
    public double getSteering(SensorModel sensors) {
        Double output = neuralNetwork.getOutput(sensors);
        return 0.5;
    }

    @Override
    public String getDriverName() {
        return "Example Controller";
    }

    @Override
    public Action controlWarmUp(SensorModel sensors) {
        Action action = new Action();
//        if(GPtest){
//            return GPControl(action, sensors);
//        }
        if(testNeural) {
            return neuralControl(action, sensors);
        }
        else if(furthestSensor) {
            return furthestSensorControl(action, sensors);
        } else if(!simulate) {
            return keyboardControl(action, sensors);
        }
        return defaultControl(action, sensors);
    }

    @Override
    public Action controlQualification(SensorModel sensors) {
        Action action = new Action();
        if(testNeural) {
            return neuralControl(action, sensors);
        } else if(furthestSensor) {
            return furthestSensorControl(action, sensors);
        }else if(!simulate) {
            return keyboardControl(action, sensors);
        }
        return defaultControl(action, sensors);
    }

    @Override
    public Action controlRace(SensorModel sensors) {
        Action action = new Action();
        if(testNeural) {
            return neuralControl(action, sensors);
        } else if(furthestSensor) {
            return furthestSensorControl(action, sensors);
        } else if(!simulate) {
            return keyboardControl(action, sensors);
        }
        return defaultControl(action, sensors);
    }

    // used to test trained
    int i=0;
    public Action neuralControl(Action action, SensorModel sensors) {
        if (action == null) {
            action = new Action();
        }

        double[] predicted_outputs = neuralNetwork.predict(sensors,previous_outputs);

        action.accelerate = predicted_outputs[0];
        action.brake = predicted_outputs[1];
        action.steering = predicted_outputs[2];
        System.out.println("predicted= Acc " + predicted_outputs[0] + " Brake " + predicted_outputs[1] + " Steering " + predicted_outputs[2]);
        //if(i>25)
            //action.restartRace=true;
        i++;
        return action;
    }

    // used to simulate the data
    @Override
    public Action defaultControl(Action action, SensorModel sensors) {
        if(!raceStarted)
        {
            raceStarted=true;
            startTime=System.currentTimeMillis();
        }
        double time_elapsed=(System.currentTimeMillis()- startTime);
        if(sensors.getLaps()==1 || time_elapsed>300000)//5 min = 5 * 60 * 1000 ms = 300 000
        {
            System.out.println("Track is finished in "+ NeuralNetwork.convertTime(time_elapsed,true));
            raceStarted=false;
            action.restartRace=true;
        }


        count += 1;
        if (action == null) {
            action = new Action();
        }

        if(lines.size() <= count){
            action.steering = DriversUtils.alignToTrackAxis(sensors, 0.5);
            if (sensors.getSpeed() > 60.0D) {
                action.accelerate = 0.0D;
                action.brake = 0.0D;
            }

            if (sensors.getSpeed() > 70.0D) {
                action.accelerate = 0.0D;
                action.brake = -1.0D;
            }

            if (sensors.getSpeed() <= 60.0D) {
                action.accelerate = (80.0D - sensors.getSpeed()) / 80.0D;
                action.brake = 0.0D;
            }

            if (sensors.getSpeed() < 30.0D) {
                action.accelerate = 1.0D;
                action.brake = 0.0D;
            }
        } else {
            String act = lines.get(count);
            String[] acts = act.split(",");

            action.steering = Double.parseDouble(acts[2]);
            action.brake = Double.parseDouble(acts[1]);
            action.accelerate = Double.parseDouble(acts[0]);
        }

        return action;
    }

    // used to simulate the data
    public Action furthestSensorControl(Action action, SensorModel sensors) {

        if (action == null) {
            action = new Action();
        }

        if(true){
            double furthest = 0;
            int edgesIndex = 0;
            double[] edges = sensors.getTrackEdgeSensors();
            for(int i =0;i<edges.length; i++){
                if(edges[i] > furthest){
                    furthest = edges[i];
                    edgesIndex = i;
                }
            }
            action.steering = ((90.0 - (double)edgesIndex * 10.0)/180) * 3.14;
//            System.out.println(action.steering);
//            action.steering = DriversUtils.alignToTrackAxis(sensors, 0.5);

//            double[] opponents = sensors.getOpponentSensors();


            if (sensors.getSpeed() > 60.0D) {
                action.accelerate = 0.0D;
                action.brake = 0.0D;
            }

            if (sensors.getSpeed() > 70.0D) {
                action.accelerate = 0.0D;
                action.brake = -1.0D;
            }

            if (sensors.getSpeed() <= 50.0D) {
                action.accelerate = (80.0D - sensors.getSpeed()) / 80.0D;
                action.brake = 0.0D;
            }

            if (sensors.getSpeed() < 400.0D) {
                action.accelerate = 1.0D;
                action.brake = 0.0D;
            }

            if(edges[9] < 0.5*sensors.getSpeed() && sensors.getSpeed() > 40){
                action.accelerate = 0.0D;
                action.brake = (20.0D)/((double)edges[9]);
//                System.out.println(action.brake);
            }
//            if(furthest < 2){
//                System.out.println("back");
//                action.gear = 0;
//                action.brake = -1.0;
//                action.accelerate = 0.0;
//            }
        }

        return action;
    }

//    // used to simulate the data
//    public Action GPControl(Action action, SensorModel sensors) {
//
//        if(sensors.getLaps() > 0){
//            lapTimes[speciesCounter] = sensors.getBestLapTime();
//            if(speciesCounter < population.size()-1) {
//                action.restartRace = true;
//                speciesCounter++;
//            }
//            else if(current_iter < iterations){
//                population = gp.nextGeneration(population, lapTimes);
//            }
//            else{
////
//            }
//        }
//
//        count += 1;
//        if (action == null) {
//            action = new Action();
//        }
//
//        if(lines.size() <= count){
//            action.steering = DriversUtils.alignToTrackAxis(sensors, 0.5);
//            if (sensors.getSpeed() > 60.0D) {
//                action.accelerate = 0.0D;
//                action.brake = 0.0D;
//            }
//
//            if (sensors.getSpeed() > 70.0D) {
//                action.accelerate = 0.0D;
//                action.brake = -1.0D;
//            }
//
//            if (sensors.getSpeed() <= 60.0D) {
//                action.accelerate = (80.0D - sensors.getSpeed()) / 80.0D;
//                action.brake = 0.0D;
//            }
//
//            if (sensors.getSpeed() < 30.0D) {
//                action.accelerate = 1.0D;
//                action.brake = 0.0D;
//            }
//        } else {
//            String act = lines.get(count);
//            String[] acts = act.split(",");
//
//            action.steering = Double.parseDouble(acts[2]);
//            action.brake = Double.parseDouble(acts[1]);
//            action.accelerate = Double.parseDouble(acts[0]);
//        }
//
//        return action;
//    }
//
//    public void setupGP(){




//        int[] times = new int[population.size()];

//        for(int i=0; i<5; i++){
            // test the whole population
//            for(int k=0; k<population.size();k++){
                // test single species
//            }
//            // print best time
//            int smallest = times[0];
//            for(int j=1; j< times.length; j++){
//                if(times[j] < smallest){
//                    smallest = times[j];
//                }
//            }
//
//            // send the 20 best species to:
//            population = gp.nextGeneration(population);
//        }
//    }

    // used to get human data
    public Action keyboardControl(Action action, SensorModel sensors) {

        double steerSensitivity=0.01D;
        if (action == null) {
            action = new Action();
        }

        if(upPressed) action.accelerate = 1.0D;
        if(upReleased) {
            action.accelerate = 0.0D;
            upReleased = false;
        }
        if(downPressed) {
            action.accelerate = 0.0D;
            action.brake = 1.0D;
        }
        if(downReleased) {
            action.brake = 0.0D;
            downReleased = false;
        }
        if(rightPressed) {
            if(rightCumulSteering>=0) {
                rightCumulSteering = -0.3D;
            }
            else if(rightCumulSteering>(steerSensitivity-1))
                rightCumulSteering-=steerSensitivity;
            else
                rightCumulSteering = -1.0D;
            action.steering = rightCumulSteering;
        }
        if(rightReleased) {
            action.steering = 0.0D;
            rightReleased = false;
            rightCumulSteering=0.0D;
        }
        if(leftPressed)
        {
            if(leftCumulSteering<=0)
                leftCumulSteering = 0.3D;
            else if(leftCumulSteering<(1-steerSensitivity))
                leftCumulSteering += steerSensitivity;
            else
                leftCumulSteering = 1.0D;
            action.steering = leftCumulSteering;

        }
        if(leftReleased) {
            action.steering = 0.0D;
            leftReleased = false;
            leftCumulSteering=0.0D;
        }

        String s = "";
        s += action.accelerate + "," + action.brake + "," + action.steering + "," + sensors.getSpeed() + "," +
                sensors.getTrackPosition() + "," + sensors.getAngleToTrackAxis();
        double[] track_edge_sensors= sensors.getTrackEdgeSensors();
        for(int i=0; i<track_edge_sensors.length;++i) {
            s += "," + track_edge_sensors[i];
        }
        System.out.println("s: "+ s);
        pw.println(s);

        return action;
    }

    public class KeyboardListener implements KeyListener {

        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {

            if(e.getKeyCode() == KeyEvent.VK_UP) {
                upPressed = true;
                upReleased = false;
            }

            if(e.getKeyCode() == KeyEvent.VK_DOWN) {
                downPressed = true;
                downReleased = false;
            }

            if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
                rightPressed = true;
                rightReleased = false;
                currentSteeringRight = true;
                currentSteeringLeft = false;
            }

            if(e.getKeyCode() == KeyEvent.VK_LEFT) {
                leftPressed = true;
                leftReleased = false;
                currentSteeringRight = false;
                currentSteeringLeft = true;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {

            if(e.getKeyCode() == KeyEvent.VK_UP) {
                upReleased = true;
                upPressed = false;
            }

            if(e.getKeyCode() == KeyEvent.VK_DOWN) {
                downReleased = true;
                downPressed = false;
            }

            if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
                if(currentSteeringRight)
                    rightReleased = true;
                rightPressed = false;
                currentSteeringRight = false;
            }

            if(e.getKeyCode() == KeyEvent.VK_LEFT) {
                if(currentSteeringLeft)
                    leftReleased = true;
                leftPressed = false;
                currentSteeringLeft = false;
            }
        }
    }

    public class FocusPanel extends JPanel {
        public FocusPanel() {
            super();
        }
    }

    public class FocusFrame extends JFrame {
        public FocusFrame() {
            this.setFocusable(true);
            this.setSize(400, 500);
            this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.setVisible(true);
            this.setTitle("Focus to Control");
            this.setContentPane(new FocusPanel());
            this.addKeyListener(new KeyboardListener());
        }
    }
}
