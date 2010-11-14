package simulation;

public class Message {
	
	Bot sender;
	String message;
	
	public Message(Bot _sender, String _message) {
		sender = _sender;;
		message = _message;
	}

	/**
	 * @return the senderID
	 */
	public Bot getSender() {
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
		return sender + "\t'" + message + "'";
	}
}
