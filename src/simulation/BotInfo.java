package simulation;
import java.awt.geom.Point2D;
import java.util.Scanner;


public class BotInfo {

	private int botNum;
	private Point2D location;
	private int mode;
	private double zoneMultiplier;
	
	public BotInfo(int _botNum) {
		botNum = _botNum;
		location = new Point2D.Double(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE);
		mode = Bot.WAITING_FOR_ACTIVATION;
		zoneMultiplier = 0.0;
	}
	
	public BotInfo(int _botNum, Point2D _loc, int _mode, double _zoneMultiplier) {
		botNum = _botNum;
		location = _loc;
		mode = _mode;
		zoneMultiplier = _zoneMultiplier;
	}

	public BotInfo(int _botNum, double _x, double _y, int _mode, double _zoneMultiplier) {
		this(_botNum, new Point2D.Double(_x, _y), _mode, _zoneMultiplier);
	}

	public BotInfo(String str) {
		//convert the passed string into a BotInfo
		Scanner strScan = new Scanner(str);
		//tell the scanner to ignore the starting [
		strScan.skip("[");
		//get the bot num
		int _botNum = strScan.nextInt();
		//skip the :
		strScan.skip(":");
		//get the x and y
		double x = strScan.nextDouble();
		double y = strScan.nextDouble();
		//all done store it 
		botNum = _botNum;
		location = new Point2D.Double(x,y);

	}

	public BotInfo(BotInfo i) {
		//just to be 100% sure it copies when using the constructor
		this(i.getBotID(), i.getCenterX(), i.getCenterY(), i.getMode(), i.getZoneMultiplier());
	}
	
	/**
	 * @return the botNum
	 */
	public int getBotID() {
		return botNum;
	}

	/**
	 * @return the centerX
	 */
	public double getCenterX() {
		return location.getX();
	}

	/**
	 * @return the centerY
	 */
	public double getCenterY() {
		return location.getY();
	}

	/**
	 * @return the location
	 */
	public Point2D getCenterLocation() {
		return location;
	}

	public int getMode() {
		return mode;
	}
	
	
	/**
	 * @return the zoneMultiplier
	 */
	public double getZoneMultiplier() {
		return zoneMultiplier;
	}

	/**
	 * @return the pathMarker
	 */
	public boolean isPathMarker() {
		return mode == Bot.PATH_MARKER;
	}
	
	public boolean isExplorer() {
		return this.isNormalExplorer() || this.isDangerExplorer();
	}
	
	public boolean isNormalExplorer() {
		return mode == Bot.EXPLORER;
	}
	
	public boolean isDangerExplorer() {
		return mode == Bot.DANGEROUS_EXPLORER;
	}
	
	public boolean isWaitingForActivation() {
		return mode == Bot.WAITING_FOR_ACTIVATION;
	}

	public void merge(BotInfo newInfo) {
		if(newInfo.botNum != this.botNum) {
			System.out.println("BOT INFOS DON'T MATCH");
			return;
		}

		if(newInfo.getCenterX() < java.lang.Double.MAX_VALUE) {
			location.setLocation(newInfo.getCenterX(), this.getCenterY());
		}

		if(newInfo.getCenterY() < java.lang.Double.MAX_VALUE) {
			location.setLocation(this.getCenterX(), newInfo.getCenterY());
		}

	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + botNum;
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
		if (!(obj instanceof BotInfo))
			return false;
		BotInfo other = (BotInfo) obj;
		if (botNum != other.botNum)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[ " + botNum + " : " + location.getX() + " " + location.getY() + " ]";
	}
}
