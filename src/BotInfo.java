
public class BotInfo {
	
	private int botNum;
	private double centerX;
	private double centerY;
	
	public BotInfo(int _botNum) {
		botNum = _botNum;
		centerX = Double.MAX_VALUE;
		centerY = Double.MAX_VALUE;
	}
	
	public void update(double newCenterX, double newCenterY) {
		centerX = newCenterX;
		centerY = newCenterY;
	}

	/**
	 * @return the botNum
	 */
	public int getBotNum() {
		return botNum;
	}

	/**
	 * @return the centerX
	 */
	public double getCenterX() {
		return centerX;
	}

	/**
	 * @return the centerY
	 */
	public double getCenterY() {
		return centerY;
	}
	
	

}
