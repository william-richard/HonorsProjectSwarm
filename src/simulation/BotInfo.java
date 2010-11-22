package simulation;
import java.awt.geom.Point2D;
import java.util.Scanner;


public class BotInfo {

	private int botNum;
	private Point2D location;
	private int zoneAssessment;

	public BotInfo(int _botNum) {
		botNum = _botNum;
		location = new Point2D.Double(java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE);
	}

	public BotInfo(int _botNum, double _x, double _y) {
		this(_botNum);
		location = new Point2D.Double(_x, _y);
	}

	public BotInfo(int _botNum, double _x, double _y, int _zoneAssessment) {
		this(_botNum, _x, _y);
		zoneAssessment = _zoneAssessment;
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
		//skip the :
		strScan.skip(":");
		//get the zone assessment
		int _zoneAssessment = strScan.nextInt();
		//all done store it 
		botNum = _botNum;
		location = new Point2D.Double(x,y);
		zoneAssessment = _zoneAssessment;

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

	public int getZoneAssessment() {
		return zoneAssessment;
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
		return "[ " + botNum + " : " + location.getX() + " " + location.getY() + " : " + zoneAssessment + " ]";
	}
}
