package fr.emse.warehouse;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class WarehouseUiApp {
    private WarehouseUiApp() {
    }

    public static void launch(String configPath) throws IOException {
        WarehouseConfig config = WarehouseConfig.fromIni(configPath);
        WarehouseSimulator simulator = new WarehouseSimulator(config);
        WarehousePanel panel = new WarehousePanel();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Warehouse Simulator UI");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setContentPane(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        Thread simulationThread = new Thread(() -> {
            try {
                simulator.run(new SimulationStepListener() {
                    @Override
                    public void onStep(SimulationSnapshot snapshot) {
                        panel.updateSnapshot(snapshot);
                    }

                    @Override
                    public void onCompleted(SimulationResult result) {
                        System.out.println("UI simulation completed.");
                    }
                });
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }, "warehouse-simulation-thread");
        simulationThread.setDaemon(false);
        simulationThread.start();
    }

    private static class WarehousePanel extends JPanel {
        private static final int PADDING = 20;
        private static final int LEGEND_WIDTH = 260;
        private static final int BASE_WIDTH = 1500;
        private static final int BASE_HEIGHT = 900;
        private volatile SimulationSnapshot snapshot;

        private WarehousePanel() {
            setPreferredSize(new Dimension(BASE_WIDTH, BASE_HEIGHT));
            setBackground(new Color(238, 238, 238));
        }

        private void updateSnapshot(SimulationSnapshot snapshot) {
            this.snapshot = snapshot;
            SwingUtilities.invokeLater(this::repaint);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            SimulationSnapshot current = snapshot;
            if (current == null) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int drawWidth = getWidth() - LEGEND_WIDTH - (PADDING * 2);
            int drawHeight = getHeight() - (PADDING * 2);
            int cellSize = Math.max(8, Math.min(drawWidth / current.getColumns(), drawHeight / current.getRows()));
            int mapWidth = current.getColumns() * cellSize;
            int mapHeight = current.getRows() * cellSize;
            int originX = PADDING;
            int originY = PADDING;

            g2.setColor(new Color(252, 252, 252));
            g2.fillRect(originX, originY, mapWidth, mapHeight);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRect(originX, originY, mapWidth, mapHeight);

            drawGrid(g2, current, originX, originY, cellSize, mapWidth, mapHeight);
            drawZones(g2, current.getEntries(), originX, originY, cellSize, new Color(255, 245, 157));
            drawZones(g2, current.getExits(), originX, originY, cellSize, new Color(255, 205, 210));
            drawZones(g2, current.getIntermediates(), originX, originY, cellSize, new Color(179, 229, 252));
            drawRecharge(g2, current.getRecharge(), originX, originY, cellSize);
            drawObstacles(g2, current.getFixedObstacles(), originX, originY, cellSize);
            drawHumans(g2, current.getHumans(), originX, originY, cellSize);
            drawPallets(g2, current.getPallets(), originX, originY, cellSize);
            drawRobots(g2, current.getRobots(), originX, originY, cellSize);
            drawLegend(g2, current, originX + mapWidth + 20, originY);

            g2.dispose();
        }

        private void drawGrid(Graphics2D g2, SimulationSnapshot s, int ox, int oy, int cell, int w, int h) {
            g2.setColor(new Color(225, 225, 225));
            for (int x = 1; x < s.getColumns(); x++) {
                int lineX = ox + x * cell;
                g2.drawLine(lineX, oy, lineX, oy + h);
            }
            for (int y = 1; y < s.getRows(); y++) {
                int lineY = oy + y * cell;
                g2.drawLine(ox, lineY, ox + w, lineY);
            }
        }

        private void drawZones(Graphics2D g2, List<SimulationSnapshot.ZoneView> zones, int ox, int oy, int cell, Color fill) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            for (SimulationSnapshot.ZoneView zone : zones) {
                int x = ox + zone.getPosition().x * cell;
                int y = oy + zone.getPosition().y * cell;
                g2.setColor(fill);
                g2.fillRect(x + 1, y + 1, cell - 1, cell - 1);
                g2.setColor(Color.BLACK);
                g2.drawRect(x, y, cell, cell);
                g2.drawString(zone.getId(), x + 2, y + Math.max(12, cell - 2));
                if (zone.getType() == ZoneType.INTERMEDIATE) {
                    g2.drawString(zone.getOccupancy() + "/" + zone.getCapacity(), x + 2, y + 12);
                }
            }
        }

        private void drawRecharge(Graphics2D g2, SimulationSnapshot.ZoneView recharge, int ox, int oy, int cell) {
            int x = ox + recharge.getPosition().x * cell;
            int y = oy + recharge.getPosition().y * cell;
            g2.setColor(new Color(200, 230, 201));
            g2.fillRect(x + 1, y + 1, cell - 1, cell - 1);
            g2.setColor(new Color(46, 125, 50));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(x, y, cell, cell);
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(Color.BLACK);
            g2.drawString(recharge.getId() + " " + recharge.getOccupancy() + "/" + recharge.getCapacity(), x + 2, y + 12);
        }

        private void drawObstacles(Graphics2D g2, List<Position> obstacles, int ox, int oy, int cell) {
            g2.setColor(Color.BLACK);
            for (Position pos : obstacles) {
                g2.fillRect(ox + pos.x * cell + 1, oy + pos.y * cell + 1, cell - 2, cell - 2);
            }
        }

        private void drawHumans(Graphics2D g2, List<Position> humans, int ox, int oy, int cell) {
            g2.setColor(new Color(255, 167, 38));
            for (Position pos : humans) {
                g2.fillOval(ox + pos.x * cell + 2, oy + pos.y * cell + 2, Math.max(6, cell - 4), Math.max(6, cell - 4));
            }
        }

        private void drawPallets(Graphics2D g2, List<SimulationSnapshot.PalletView> pallets, int ox, int oy, int cell) {
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            for (SimulationSnapshot.PalletView pallet : pallets) {
                Position pos = pallet.getPosition();
                int x = ox + pos.x * cell;
                int y = oy + pos.y * cell;
                if (pallet.getStatus() == PalletStatus.STORED_INTERMEDIATE) {
                    g2.setColor(new Color(255, 241, 118));
                } else if (pallet.getStatus() == PalletStatus.CARRIED_BY_ROBOT) {
                    g2.setColor(new Color(255, 224, 130));
                } else {
                    g2.setColor(new Color(255, 213, 79));
                }
                int size = Math.max(6, cell / 2);
                int px = x + (cell - size) / 2;
                int py = y + (cell - size) / 2;
                g2.fillRect(px, py, size, size);
                g2.setColor(Color.BLACK);
                g2.drawRect(px, py, size, size);
                g2.drawString("P" + pallet.getId() + "->" + pallet.getDestinationExit(), x + 1, y + cell - 2);
            }
        }

        private void drawRobots(Graphics2D g2, List<SimulationSnapshot.RobotView> robots, int ox, int oy, int cell) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            for (SimulationSnapshot.RobotView robot : robots) {
                Position pos = robot.getPosition();
                int x = ox + pos.x * cell;
                int y = oy + pos.y * cell;
                g2.setColor(new Color(66, 165, 245));
                g2.fillOval(x + 1, y + 1, cell - 2, cell - 2);
                g2.setColor(Color.BLACK);
                g2.drawOval(x + 1, y + 1, cell - 2, cell - 2);
                if (robot.isCarrying()) {
                    g2.setColor(new Color(255, 235, 59));
                    g2.fillOval(x + cell / 3, y + cell / 3, Math.max(4, cell / 3), Math.max(4, cell / 3));
                }
                g2.setColor(Color.BLACK);
                g2.drawString("R" + robot.getId(), x + 2, y + Math.max(11, cell - 3));
            }
        }

        private void drawLegend(Graphics2D g2, SimulationSnapshot snapshot, int x, int y) {
            int line = y + 20;
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.drawString("Warehouse Live View", x, line);
            line += 26;
            g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g2.drawString("Step: " + snapshot.getStep(), x, line);
            line += 18;
            g2.drawString("Delivered: " + snapshot.getDelivered(), x, line);
            line += 18;
            g2.drawString("Robots: " + snapshot.getRobots().size(), x, line);
            line += 18;
            g2.drawString("Pallets in system: " + snapshot.getPallets().size(), x, line);
            line += 18;
            g2.drawString("Recharge occupancy: " + snapshot.getRecharge().getOccupancy() + "/" + snapshot.getRecharge().getCapacity(), x, line);
            line += 28;
            g2.drawString("Legend:", x, line);
            line += 18;
            g2.setColor(new Color(255, 245, 157));
            g2.fillRect(x, line - 12, 14, 14);
            g2.setColor(Color.BLACK);
            g2.drawString("Entry zones", x + 20, line);
            line += 18;
            g2.setColor(new Color(255, 205, 210));
            g2.fillRect(x, line - 12, 14, 14);
            g2.setColor(Color.BLACK);
            g2.drawString("Exit zones", x + 20, line);
            line += 18;
            g2.setColor(new Color(179, 229, 252));
            g2.fillRect(x, line - 12, 14, 14);
            g2.setColor(Color.BLACK);
            g2.drawString("Intermediate zones", x + 20, line);
            line += 18;
            g2.setColor(new Color(200, 230, 201));
            g2.fillRect(x, line - 12, 14, 14);
            g2.setColor(Color.BLACK);
            g2.drawString("Recharge zone", x + 20, line);
            line += 18;
            g2.setColor(new Color(255, 213, 79));
            g2.fillRect(x, line - 12, 14, 14);
            g2.setColor(Color.BLACK);
            g2.drawString("Pallet", x + 20, line);
            line += 18;
            g2.setColor(new Color(66, 165, 245));
            g2.fillOval(x, line - 12, 14, 14);
            g2.setColor(Color.BLACK);
            g2.drawString("Robot", x + 20, line);
            line += 18;
            g2.setColor(new Color(255, 167, 38));
            g2.fillOval(x, line - 12, 14, 14);
            g2.setColor(Color.BLACK);
            g2.drawString("Human obstacle", x + 20, line);
        }
    }
}
