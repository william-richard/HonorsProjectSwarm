import java.awt.Rectangle;
import java.util.List;


public class World extends Rectangle {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** VARIABLES */
	List<Zone> Zones; //The zones in the world - should be non-overlapping
	List<Bot> Bots; //List of the Bots, so we can do stuff with them
	List<Victim> Victims; //The Victims
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
