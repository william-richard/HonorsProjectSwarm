import java.awt.Color;


public class DangerZone extends Zone {
	
	private static final long serialVersionUID = 1L;

	public DangerZone(int[] xPoints, int[] yPoints, int numPoints, int _zoneID) {
		super(xPoints, yPoints, numPoints, _zoneID, new Color(205,0,0)); //SET THE ZONE COLOR HERE
	}

}
