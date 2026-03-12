package fr.emse;

import java.awt.Color;

import fr.emse.fayol.maqit.simulator.components.ColorInteractionRobot;
import fr.emse.fayol.maqit.simulator.components.Message;

public class BasicRobot extends ColorInteractionRobot {

	public BasicRobot(String name, int field, int debug, int[] pos, Color co, int rows, int columns) {
		super(name, field, pos, new int[] { co.getRed(), co.getGreen(), co.getBlue() });
	}

	@Override
	public void move(int nb) {
		for (int idx = 0; idx < nb; idx++) {
			if (freeForward()) {
				moveForward();
			} else {
				turnLeft();
			}
			Message message = new Message(getId(), "pos=" + getX() + "," + getY());
			sendMessage(message);
		}
	}

	@Override
	public void handleMessage(Message message) {
		System.out.println("Robot " + getId() + " received message from " + message.getEmitter()
			+ ": " + message.getContent());
	}
}
