package simulation;

import java.util.ArrayList;

public class Message {

	private BotInfo sender;
	private String type;
	private String message;
	private ArrayList<Object> attachments;

	public final static String BOT_LOCATION_MESSAGE = 						"bloc";
	public final static String CLAIM_SURVIVOR_MESSAGE = 					"cs";
	public final static String FOUND_SURVIVOR_MESSAGE = 					"fs";
	public final static String CREATE_PATH_MESSAGE = 						"cp";
	public final static String STOP_ADDING_NEW_PATH_MARKERS = 				"sanpm";

	public Message(BotInfo _sender, String _type, String _message, ArrayList<Object> _attachments) {
		sender = _sender;;
		type = _type;
		message = _message;
		attachments = _attachments;
	}


	public Message(BotInfo _sender, String _type, String _message) {
		this(_sender, _type, _message, null);
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

	public Object getAttachment(int index) {
		if(index < 0 || index >= attachments.size()) {
			throw new IndexOutOfBoundsException("Trying to get attachment with invalid index");
		}
		return attachments.get(index);
	}

	@Override
	public String toString() {
		return sender + "\t" + type + "\t'" + message + "' " + attachments;
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
		return new Message(sender.getBotInfo(), 
				BOT_LOCATION_MESSAGE, sender.getID() + " " + World.getCurrentTimestep() + " " + sender.getCenterX() + " " + sender.getCenterY() + "\n");
	}

	public static Message constructFoundMessage(Bot sender, Survivor foundSurvivor, double surDamageAssessment) {
		return new Message(sender.getBotInfo(), 
				FOUND_SURVIVOR_MESSAGE, sender.getID() + " " + World.getCurrentTimestep() + " " + surDamageAssessment + " " + foundSurvivor.getCenterX() + " " + foundSurvivor.getCenterY() + "\n");
	}

	public static Message constructClaimMessage(Bot sender) {
		Survivor senderSurvivior = sender.getMySurvivor();
		if (senderSurvivior == null) {
			// can't do it - no survivor to claim
			return null;
		}
		return new Message(sender.getBotInfo(), 
				CLAIM_SURVIVOR_MESSAGE, sender.getID() + " " + World.getCurrentTimestep() + " " + senderSurvivior.getCenterX() + " " + senderSurvivior.getCenterY() + " " + sender.getMySurvivorClaimTime() + "\n");
	}

	public static Message constructCreatePathsMessage(Bot sender, SurvivorPath pathToUse) {

		ArrayList<Object> attachement = new ArrayList<Object>();
		//copy it with the constructor to make sure there aren't any pointer issues
		attachement.add(new SurvivorPath(pathToUse));

		return new Message(sender.getBotInfo(), CREATE_PATH_MESSAGE, sender.getID() + " " + World.getCurrentTimestep(), attachement);
	}

	public static Message constructStopAddNewPathMarkersMessage(Bot sender, SurvivorPath stopMarkingThis) {
		ArrayList<Object> attachment = new ArrayList<Object>();
		attachment.add(new SurvivorPath(stopMarkingThis));
		
		return new Message(sender.getBotInfo(), STOP_ADDING_NEW_PATH_MARKERS,  sender.getID() + " " + World.getCurrentTimestep(), attachment);
	}
}
