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
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DefaultDriver extends AbstractDriver {

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

    public DefaultDriver() {
        initialize();
        neuralNetwork = new NeuralNetwork(12, 8, 2);
        //neuralNetwork = neuralNetwork.loadGenome();
        focusFrame = new FocusFrame();
        focusFrame.requestFocus();

        try {
            pw = new PrintWriter(new File("test.csv"));
            pw.println("ACCELERATION,BRAKE,STEERING,SPEED,TRACK_POSITION,ANGLE_TO_TRACK_AXIS,TRACK_EDGE_0,TRACK_EDGE_1,TRACK_EDGE_2," +
                    "TRACK_EDGE_3,TRACK_EDGE_4,TRACK_EDGE_5,TRACK_EDGE_6,TRACK_EDGE_7,TRACK_EDGE_8,TRACK_EDGE_9,TRACK_EDGE_10," +
                    "TRACK_EDGE_11,TRACK_EDGE_12,TRACK_EDGE_13,TRACK_EDGE_14,TRACK_EDGE_15,TRACK_EDGE_16,TRACK_EDGE_17");
            /*reader = new BufferedReader(new FileReader("test.csv"));
            lines = new ArrayList<String>();
            String line = null;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }*/
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initialize() {
        this.enableExtras(new AutomatedClutch());
        this.enableExtras(new AutomatedGearbox());
        //this.enableExtras(new AutomatedRecovering());
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
        //return defaultControl(action, sensors);
        return keyboardControl(action, sensors);
    }

    @Override
    public Action controlQualification(SensorModel sensors) {
        Action action = new Action();
        //return defaultControl(action, sensors);
        return keyboardControl(action, sensors);
    }

    @Override
    public Action controlRace(SensorModel sensors) {
        Action action = new Action();
        //return defaultControl(action, sensors);
        return keyboardControl(action, sensors);
    }

    @Override
    public Action defaultControl(Action action, SensorModel sensors) {
        count += 1;
        System.out.println(count);
        if (action == null) {
            action = new Action();
        }
        if(lines.size() <= count) {
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

        /*System.out.println("--------------" + getDriverName() + "--------------");
        System.out.println("Steering: " + action.steering);
        System.out.println("Acceleration: " + action.accelerate);
        System.out.println("Brake: " + action.brake);
        System.out.println("Angle to track axis: "+ sensors.getAngleToTrackAxis());
        String s="[";
        double[] track_edge_sensors= sensors.getTrackEdgeSensors();
        for(int i=0; i<track_edge_sensors.length;++i) {
            s += " " + track_edge_sensors[i];
        }
        s+="]";
        System.out.println("Track edge sensors: "+ s);
        s="[";
        double[] focus_sensors= sensors.getFocusSensors();
        for(int i=0; i<focus_sensors.length;++i) {
            s += " " + focus_sensors[i];
        }
        s+="]";
        System.out.println("Focus sensor: "+ s);
        System.out.println("Track position: "+sensors.getTrackPosition());
        System.out.println("-----------------------------------------------");*/
        return action;
    }

    // used to get human data
    public Action keyboardControl(Action action, SensorModel sensors) {

        double steerSensitivity=0.01D;
        if (action == null) {
            action = new Action();
        }
        if(sensors.getGear()==-1)
        {
            action.gear=0;
            action.brake=0.0D;
            action.accelerate=1.0D;
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
                rightCumulSteering = -0.1D;
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
                leftCumulSteering = 0.1D;
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

         System.out.println("--------------" + getDriverName() + "--------------");
         System.out.println("Steering: " + action.steering);
         System.out.println("Acceleration: " + action.accelerate);
         System.out.println("Brake: " + action.brake);

        String s = "";
        s += action.accelerate + "," + action.brake + "," + action.steering + "," + sensors.getSpeed() + "," +
                sensors.getTrackPosition() + "," + sensors.getAngleToTrackAxis();
        double[] track_edge_sensors= sensors.getTrackEdgeSensors();
        for(int i=0; i<track_edge_sensors.length;++i) {
            s += "," + track_edge_sensors[i];
        }
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