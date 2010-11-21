package simulation;

public class Message {
	
	BotInfo sender;
	String message;
	
	public Message(BotInfo _sender, String _message) {
		sender = _sender;;
		message = _message;
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
		return sender + "\t'" + message + "'";
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
}
