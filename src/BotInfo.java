import java.awt.geom.Point2D;
import java.util.List;


public class BotInfo {
	
	private int botNum;
	private Point2D location;
	
	public BotInfo(int _botNum) {
		botNum = _botNum;
		location = new Point2D.Double(Double.MAX_VALUE, Double.MAX_VALUE);
	}
	
	public BotInfo(int _botNum, double _x, double _y) {
		this(_botNum);
		location = new Point2D.Double(_x, _y);
	}
	
	public void update(double newCenterX, double newCenterY) {
		
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
	public Point2D getLocation() {
		return location;
	}
	
	public void merge(BotInfo newInfo) {
		if(newInfo.botNum != this.botNum) {
			System.out.println("BOT INFOS DON'T MATCH");
			return;
		}
		
		if(newInfo.getCenterX() < Double.MAX_VALUE) {
			location.setLocation(newInfo.getCenterX(), this.getCenterY());
		}
		
		if(newInfo.getCenterY() < Double.MAX_VALUE) {
			location.setLocation(this.getCenterX(), newInfo.getCenterY());
		}
		
	}

	public static void updateBotInfoInList(List<BotInfo> botInfos, int botNum, double newCenterX, double newCenterY) {
		BotInfo currInfo = botInfos.get(botNum);
		currInfo.update(newCenterX, newCenterY);
	}
	

}
