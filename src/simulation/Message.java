package simulation;

import java.awt.geom.PathIterator;

public class Message {
	
	BotInfo sender;
	String type;
	String message;
	
	public final static String BOT_LOCATION_MESSAGE = 						"bloc";
	public final static String CLAIM_SURVIVOR_MESSAGE = 					"cs";
	public final static String FOUND_SURVIVOR_MESSAGE = 					"fs";
	public final static String CREATE_PATH_MESSAGE = 						"cp";
	
	public Message(BotInfo _sender, String _type, String _message) {
		sender = _sender;;
		type = _type;
		message = _message;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the senderID
	 */
	public BotInfo getSender() {
		return sender;
	}

	/**
	 * @return the message
	 */
	public String getText() {
		return message;
	}
	
	@Override
	public String toString() {
		return sender + "\t" + type + "\t'" + message + "'";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((sender == null) ? 0 : sender.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Message))
			return false;
		Message other = (Message) obj;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (sender == null) {
			if (other.sender != null)
				return false;
		} else if (!sender.equals(other.sender))
			return false;
		return true;
	}
	
	public static Message constructLocationMessage(Bot sender) {
		return new Message(sender.getThisBotInfo(), 
				BOT_LOCATION_MESSAGE, sender.getID() + " " + World.getCurrentTimestep() + " " + sender.getCenterX() + " " + sender.getCenterY() + "\n");
	}

	public static Message constructFoundMessage(Bot sender, Survivor foundSurvivor, double surDamageAssessment) {
		return new Message(sender.getThisBotInfo(), 
				FOUND_SURVIVOR_MESSAGE, sender.getID() + " " + World.getCurrentTimestep() + " " + surDamageAssessment + " " + foundSurvivor.getCenterX() + " " + foundSurvivor.getCenterY() + "\n");
	}

	public static Message constructClaimMessage(Bot sender) {
		Survivor senderSurvivior = sender.getMySurvivor();
		if (senderSurvivior == null) {
			// can't do it - no survivor to claim
			return null;
		}
		return new Message(sender.getThisBotInfo(), 
				CLAIM_SURVIVOR_MESSAGE, sender.getID() + " " + World.getCurrentTimestep() + " " + senderSurvivior.getCenterX() + " " + senderSurvivior.getCenterY() + " " + sender.getMySurvivorClaimTime() + "\n");
	}

	public static Message constructCreatePathsMessage(Bot sender, SurvivorPath pathToUse) {
		//make a string representing the path
		//include the damage of the survivor, and the points in the path
		//start with the survivor
		String messageBody = "";

		Survivor pathSurvivor = pathToUse.getSur();

		messageBody += pathSurvivor.getCenterX() + " " + pathSurvivor.getCenterY() + " " + pathSurvivor.getDamage() + "\t";

		//now add the points in the path
		PathIterator pathit = pathToUse.getPathIterator(null);

		double[] curCoord = new double[6];
		while(! pathit.isDone()){
			//get the coordinates of the current point
			int segType = pathit.currentSegment(curCoord);
			if(segType == PathIterator.SEG_CLOSE || segType == PathIterator.SEG_CUBICTO || segType == PathIterator.SEG_QUADTO) {
				throw new IllegalArgumentException("Got a path that has incorrect form");
			}
			//add the current point to the string
			messageBody += " " + curCoord[0] + " " + curCoord[1] + " ";
			pathit.next();
		}

		return new Message(sender.getThisBotInfo(), CREATE_PATH_MESSAGE, messageBody);
	}
}
