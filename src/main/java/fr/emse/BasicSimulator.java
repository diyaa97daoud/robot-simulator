package fr.emse;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.emse.fayol.maqit.simulator.SimFactory;
import fr.emse.fayol.maqit.simulator.components.ColorObstacle;
import fr.emse.fayol.maqit.simulator.components.Message;
import fr.emse.fayol.maqit.simulator.components.SituatedComponent;
import fr.emse.fayol.maqit.simulator.configuration.IniFile;
import fr.emse.fayol.maqit.simulator.configuration.SimProperties;
import fr.emse.fayol.maqit.simulator.display.GraphicalWindow;
import fr.emse.fayol.maqit.simulator.environment.Cell;
import fr.emse.fayol.maqit.simulator.environment.ColorGridEnvironment;
import fr.emse.fayol.maqit.simulator.environment.ColorSimpleCell;
import fr.emse.fayol.maqit.simulator.environment.GridEnvironment; 


public class BasicSimulator extends SimFactory<GridEnvironment, SituatedComponent> {

    private static final Path LOG_FILE = Path.of("simulation.log");
    private static BufferedWriter logWriter;

    GraphicalWindow gwindow;
    public static void main(String [] args) throws Exception {
        initializeLogger();
        IniFile ifile = new IniFile("configuration.ini");
        SimProperties sp = new SimProperties(ifile);

        sp.simulationParams();
        sp.displayParams();

        try {
            BasicSimulator sim = new BasicSimulator(sp); 
            sim.createEnvironment();
            for (int idx = 0; idx < sp.nbrobot; idx++) {
                sim.createRobot();
            }
            if (sp.display == 1) { 
                sim.initializeGW(); 
                for (int idx = 0; idx < sp.nbobstacle; idx++) {
                    sim.createObstacle();
                    sim.refreshGW();
                }

                sim.createObstacle();
                sim.refreshGW();
            }
            sim.schedule();
        } finally {
            closeLogger();
        }
    } 

    public BasicSimulator(SimProperties sp) {
        super(sp);
    }

    public void initializeGW() {
        this.gwindow = new GraphicalWindow((ColorSimpleCell[][])(this.environment.getGrid()), this.sp.display_x, this.sp.display_y, this.sp.display_width, this.sp.display_height, this.sp.display_title);
        this.gwindow.init();
    }

    public void refreshGW() {
        gwindow.refresh();
    }

    @Override
    public void createEnvironment() {
        this.environment = new ColorGridEnvironment(this.sp.seed);
    }

    @Override
    public void createObstacle() {
        int[] freePlace = this.environment.getPlace();
        print("Obstacle created at " + freePlace[0] + "," + freePlace[1]);
        int[] obstacleColor = new int[] {
            sp.colorobstacle.getRed(),
            sp.colorobstacle.getGreen(),
            sp.colorobstacle.getBlue()
        };
        ColorObstacle obstacle = new ColorObstacle(freePlace, obstacleColor); 
        this.addNewComponent(obstacle);
        
     } 

    @Override
    public void createRobot() {
        int[] freePlace = this.environment.getPlace();
        print("Robot created at " + freePlace[0] + "," + freePlace[1]);
        BasicRobot robot = new BasicRobot(
            "basic-robot",
            this.sp.field,
            this.sp.debug,
            freePlace,
            new Color(
                this.sp.colorrobot.getRed(),
                this.sp.colorrobot.getGreen(),
                this.sp.colorrobot.getBlue()
            ),
            this.sp.rows,
            this.sp.columns
        );
        this.addNewComponent(robot);
    }

    @Override
    public void createGoal() { } 

    @Override
    public void addNewComponent(SituatedComponent j) { 
        this.environment.setCellContent(j.getX(), j.getY(), j);
    } 

    @Override
    public void updateEnvironment(int[] oldPos, int[] newPos, int robotId) {
        this.environment.removeCellContent(oldPos[0], oldPos[1]);

        List<?> robots = this.environment.getRobot();
        for (Object component : robots) {
            if (component instanceof SituatedComponent situatedComponent && situatedComponent.getId() == robotId) {
                this.environment.setCellContent(newPos[0], newPos[1], situatedComponent);
                break;
            }
        }
    } 
    
    @Override
    public void schedule() {
        for (int i = 0; i < this.sp.step; i++) {
            print("Step: " + i);

            Map<Integer, BasicRobot> robotsById = new LinkedHashMap<>();
            for (Object component : this.environment.getRobot()) {
                if (component instanceof BasicRobot robot) {
                    robotsById.put(robot.getId(), robot);
                }
            }
            List<BasicRobot> robots = new ArrayList<>(robotsById.values());

            for (BasicRobot receiverRobot : robots) {
                for (BasicRobot senderRobot : robots) {
                    for (Object rawMessage : senderRobot.popSentMessages()) {
                        if (!(rawMessage instanceof Message message)) {
                            continue;
                        }
                        if (receiverRobot.getId() == senderRobot.getId()) {
                            continue;
                        }
                        if (message.getReceiver() == 0 || message.getReceiver() == receiverRobot.getId()) {
                            receiverRobot.receiveMessage(message);
                            String messageType = message.getReceiver() == 0 ? "broadcast" : "direct";
                            print("Step " + i + " | message " + messageType + " from "
                                + senderRobot.getId() + " to " + receiverRobot.getId() + ": "
                                + message.getContent());
                        }
                    }
                }
            }

            for (BasicRobot robot : robots) {
                robot.readMessages();

                int[] oldPos = robot.getLocation().clone();
                Cell[][] perception = this.environment.getNeighbor(robot.getX(), robot.getY(), robot.getField());
                robot.updatePerception(perception);
                robot.move(1);
                int[] newPos = robot.getLocation().clone();
                updateEnvironment(oldPos, newPos, robot.getId());
                print("Step " + i + " | " + robot.getName() + " (id=" + robot.getId() + ") moved from "
                    + oldPos[0] + "," + oldPos[1] + " to " + newPos[0] + "," + newPos[1]);
            }

            if (this.sp.display == 1 && this.gwindow != null) {
                refreshGW();
            }

            try {
                Thread.sleep(this.sp.waittime);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                System.out.println(ex);
                return;
            }
        }
    }
    
    private static synchronized void print(String s) { 
        System.out.println(s);
        if (logWriter == null) {
            return;
        }
        try {
            logWriter.write(s);
            logWriter.newLine();
            logWriter.flush();
        } catch (IOException ex) {
            System.out.println("Failed to write log file: " + ex.getMessage());
        }
    }

    private static void initializeLogger() throws IOException {
        Files.deleteIfExists(LOG_FILE);
        logWriter = Files.newBufferedWriter(
            LOG_FILE,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }

    private static void closeLogger() {
        if (logWriter == null) {
            return;
        }
        try {
            logWriter.close();
        } catch (IOException ex) {
            System.out.println("Failed to close log file: " + ex.getMessage());
        } finally {
            logWriter = null;
        }
    }
}