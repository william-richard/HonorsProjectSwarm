package simulation;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class SimulationController extends JFrame implements PropertyChangeListener, ActionListener, ItemListener {

	private static final long serialVersionUID = 2089545078800743566L;

	// the world we're simulating
	private World world;

	// initial values for fields
	private int numBots = 200;
	private int numSurvivors = 5;
	private double timeBetweenTimestepsInSeconds = 0;
	private static final boolean DRAW_BOT_RADII_INIT_VALUE = false;
	private static final boolean CHOOSE_ZONE_DIR_INIT_VALUE = false;
	private static final boolean CHOOSE_SUR_DIR_INIT_VALUE = false;

	// buttons to control the simulation
	private JButton runSimulationButton;;
	private JButton stopSimulationButton;
	private JButton resetSimulationButton;
	private JButton runTestsButton;

	//check boxes to set values
	private JCheckBox drawBotRadiiCheckBox;
	private JCheckBox chooseZoneDirCheckBox;
	private JCheckBox chooseSurDirCheckBox;

	// feilds for variable entry
	private JFormattedTextField numBotsField;
	private JFormattedTextField numSurvivorsField;
	private JFormattedTextField timeBetweenTimestepsField; //TODO Make this into a slider

	// Formats to parse numbers in fields
	private NumberFormat numBotsFormat;
	private NumberFormat numSurvivorsFormat;
	private NumberFormat timeBetweenTimestepsFormat;

	// Label objects for fields and components
	private JLabel numBotsLabel;
	private JLabel numSurvivorsLabel;
	private JLabel timeBetweenTimestepsLabel;
	private JLabel drawBotRadiiLabel;
	private JLabel chooseZoneDirLabel;
	private JLabel chooseSurDirLabel;

	// Label strings
	private final String runSimulationString = "Run";
	private final String stopSimulationString = "Stop";
	private final String resetSimulationString = "Reset";

	private final String numBotsString = "Number of Bots: ";
	private final String numSurvivorsString = "Number of Survivors: ";
	private final String timeBetweenTimestepsString = "Time between timesteps (seconds) :";
	private final String drawBotRadiiString = "Draw bot radii: ";	
	private final String chooseZoneDirString = "Choose saved zones";
	private final String chooseSurDirString = "Choose saved survivor locations";
	private final String runTestsString = "Run tests";

	//TODO add a field that highlights a certain bot num

	public SimulationController() {
		super("Simulation Controller");

		// set up the window
		setResizable(false);

		setFocusable(true);

		setUpLables();

		// set everything up
		setUpFormats();
		setUpFields();
		setUpButtons();

		//lay everything out
		JPanel variablesPanel = layoutVariableComponentsAndLabels();
		JPanel buttonsPanel = layoutButtons();

		//put them into the overall layout
		add(variablesPanel, BorderLayout.NORTH);
		add(buttonsPanel, BorderLayout.SOUTH);
	}

	private void setUpLables() {
		numBotsLabel = new JLabel(numBotsString);
		numSurvivorsLabel = new JLabel(numSurvivorsString);
		timeBetweenTimestepsLabel = new JLabel(timeBetweenTimestepsString);
		drawBotRadiiLabel = new JLabel(drawBotRadiiString);
		chooseSurDirLabel = new JLabel(chooseSurDirString);
		chooseZoneDirLabel = new JLabel(chooseZoneDirString);
	}

	private void setUpFormats() {
		numBotsFormat = new DecimalFormat("###");
		numBotsFormat.setMaximumIntegerDigits(3);
		numBotsFormat.setMinimumIntegerDigits(1);

		numSurvivorsFormat = new DecimalFormat("###");
		numSurvivorsFormat.setMaximumIntegerDigits(3);
		numSurvivorsFormat.setMinimumIntegerDigits(1);

		timeBetweenTimestepsFormat = new DecimalFormat("##.#");
		timeBetweenTimestepsFormat.setMaximumIntegerDigits(2);
		timeBetweenTimestepsFormat.setMinimumIntegerDigits(0);
	}

	private void setUpFields() {
		int numFieldColumns = 3;

		numBotsField = new JFormattedTextField(numBotsFormat);
		numBotsField.setValue(new Integer(numBots));
		numBotsField.setColumns(numFieldColumns);
		numBotsField.addPropertyChangeListener("value", this);
		numBotsLabel.setLabelFor(numBotsField);

		numSurvivorsField = new JFormattedTextField(numSurvivorsFormat);
		numSurvivorsField.setValue(new Integer(numSurvivors));
		numSurvivorsField.setColumns(numFieldColumns);
		numSurvivorsField.addPropertyChangeListener("value", this);
		numSurvivorsLabel.setLabelFor(numSurvivorsField);

		timeBetweenTimestepsField = new JFormattedTextField(timeBetweenTimestepsFormat);
		timeBetweenTimestepsField.setValue(new Double(timeBetweenTimestepsInSeconds));
		timeBetweenTimestepsField.setColumns(numFieldColumns);
		timeBetweenTimestepsField.addPropertyChangeListener("value", this);
		timeBetweenTimestepsLabel.setLabelFor(timeBetweenTimestepsField);
	}	

	private void setUpButtons() {
		runSimulationButton = new JButton(runSimulationString);
		runSimulationButton.addActionListener(this);

		stopSimulationButton = new JButton(stopSimulationString);
		stopSimulationButton.addActionListener(this);

		resetSimulationButton = new JButton(resetSimulationString);
		resetSimulationButton.addActionListener(this);

		runTestsButton = new JButton(runTestsString);
		runTestsButton.addActionListener(this);

		drawBotRadiiCheckBox = new JCheckBox();
		drawBotRadiiCheckBox.setSelected(DRAW_BOT_RADII_INIT_VALUE);
		drawBotRadiiCheckBox.addItemListener(this);

		chooseSurDirCheckBox = new JCheckBox();
		chooseSurDirCheckBox.setSelected(CHOOSE_SUR_DIR_INIT_VALUE);
		chooseSurDirCheckBox.addItemListener(this);

		chooseZoneDirCheckBox = new JCheckBox();
		chooseZoneDirCheckBox.setSelected(CHOOSE_ZONE_DIR_INIT_VALUE);
		chooseZoneDirCheckBox.addItemListener(this);
	}


	private JPanel layoutVariableComponentsAndLabels() {
		JPanel panel = new JPanel(new GridLayout(0,2));
		panel.add(numBotsLabel);
		panel.add(numBotsField);

		panel.add(chooseSurDirLabel);
		panel.add(chooseSurDirCheckBox);

		panel.add(numSurvivorsLabel);
		panel.add(numSurvivorsField);

		panel.add(chooseZoneDirLabel);
		panel.add(chooseZoneDirCheckBox);

		panel.add(timeBetweenTimestepsLabel);
		panel.add(timeBetweenTimestepsField);

		panel.add(drawBotRadiiLabel);
		panel.add(drawBotRadiiCheckBox);

		return panel;
	}

	private JPanel layoutButtons() {
		JPanel panel = new JPanel(new GridLayout(1, 0));
		panel.add(resetSimulationButton);
		panel.add(stopSimulationButton);
		panel.add(runTestsButton);
		panel.add(runSimulationButton);

		return panel;
	}


	@Override
	public void propertyChange(PropertyChangeEvent e) {
		Object source = e.getSource();

		if(source == numBotsField) {
			numBots = ((Number)numBotsField.getValue()).intValue();
			if(numBots < 0) {
				numBots *= -1;
				numBotsField.setValue(new Integer(numBots));
			}
		}

		if(source == numSurvivorsField) {
			numSurvivors = ((Number)numSurvivorsField.getValue()).intValue();
			if(numSurvivors < 0) {
				numSurvivors *= -1;
				numSurvivorsField.setValue(new Integer(numSurvivors));
			}
		}

		if(source == timeBetweenTimestepsField) {
			timeBetweenTimestepsInSeconds = ((Number)timeBetweenTimestepsField.getValue()).doubleValue();
			if(timeBetweenTimestepsInSeconds < 0) {
				timeBetweenTimestepsInSeconds *= -1;
				timeBetweenTimestepsField.setValue(new Double(timeBetweenTimestepsInSeconds));
			}

			if(world != null) {
				world.setTimeBetweenTimesteps((long)(timeBetweenTimestepsInSeconds * 1000)); //convert from seconds to milliseconds
			}
		}
	}

	private void makeTheWorld(File zoneDir, File surDir) {
		if(world != null) {
			world.stopSimulation();
			world.dispose();
		}

		//if they want to, choose a zone directory or survivor directory
		if(zoneDir == null && chooseZoneDirCheckBox.isSelected()) {
			//choose the zone directory location, or press cancel if want to create randomly
			JFileChooser zoneDirChooser = new JFileChooser();
			zoneDirChooser.setCurrentDirectory(new File("."));
			zoneDirChooser.setDialogTitle("Choose a zone directory, or cancel for random creation");
			zoneDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			zoneDirChooser.setAcceptAllFileFilterUsed(false);

			if(zoneDirChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				zoneDir = zoneDirChooser.getSelectedFile();
			} 
		}

		if(surDir == null && chooseSurDirCheckBox.isSelected()) {
			JFileChooser surDirChooser = new JFileChooser();
			surDirChooser.setCurrentDirectory(new File("."));
			surDirChooser.setDialogTitle("Choose a survivor directory, or cancel to place them randomly");
			surDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			surDirChooser.setAcceptAllFileFilterUsed(false);

			if(surDirChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				surDir = surDirChooser.getSelectedFile();
			}
		}

		if(zoneDir != null) {
			if(surDir != null) {
				world = new World(numBots, surDir, (long)timeBetweenTimestepsInSeconds*1000, drawBotRadiiCheckBox.isSelected(), zoneDir);
			} else {
				world = new World(numBots, numSurvivors, (long)timeBetweenTimestepsInSeconds*1000, drawBotRadiiCheckBox.isSelected(), zoneDir);
			}
		} else {
			if(surDir != null) {
				world = new World(numBots, surDir, (long)timeBetweenTimestepsInSeconds*1000, drawBotRadiiCheckBox.isSelected());
			} else {
				world = new World(numBots, numSurvivors, (long)timeBetweenTimestepsInSeconds*1000, drawBotRadiiCheckBox.isSelected());
			}
		}

		world.setDrawBotRadii(drawBotRadiiCheckBox.isSelected());
		//		world.pack();
		world.setLocation(this.getX(), this.getY() + this.getHeight());
		world.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		if(source == runSimulationButton) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					if(world == null) {
						makeTheWorld(null, null);
					}

					if(! world.isGoing()) {
						//put the running in a seperate thread
						new Thread(new Runnable() {

							@Override
							public void run() {
								world.go();	
							}
						}).start();
					}
				}
			});
		}

		if(source == stopSimulationButton) {
			if(world != null) {
				world.stopSimulation();
			}
		}

		if(source == resetSimulationButton) {
			makeTheWorld(null, null);
		}

		if(source == runTestsButton) {
			//first test - see how long it takes to run lots of bots
			//ask for a specific zone and survivor combination
			File zoneDir = null, surDir = null;
			JFileChooser zoneDirChooser = new JFileChooser();
			zoneDirChooser.setCurrentDirectory(new File("."));
			zoneDirChooser.setDialogTitle("Choose a zone directory, or cancel for random creation");
			zoneDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			zoneDirChooser.setAcceptAllFileFilterUsed(false);

			if(zoneDirChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				zoneDir = zoneDirChooser.getSelectedFile();
			} 

			JFileChooser surDirChooser = new JFileChooser();
			surDirChooser.setCurrentDirectory(new File("."));
			surDirChooser.setDialogTitle("Choose a survivor directory, or cancel to place them randomly");
			surDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			surDirChooser.setAcceptAllFileFilterUsed(false);

			if(surDirChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				surDir = surDirChooser.getSelectedFile();
			}
			for(numBots = 100; numBots <= 800; numBots += 100) {
				//run each test 5 times, so that we get a good range of numbers
				for(int i = 0; i < 5; i++) {
					makeTheWorld(zoneDir, surDir);
					//go for 1000 timesteps - should be enough time to settle down
					//FIXME can't see what's going on
					world.go(1000);
					//this will make UI unresponsive but who cares - just stop from Eclipse
				}
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();

		if(world!= null && source == drawBotRadiiCheckBox) {
			world.setDrawBotRadii(e.getStateChange() == ItemEvent.SELECTED);
		}
	}

	public static void createAndShowGUI() {
		SimulationController simulationController = new SimulationController();
		simulationController.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		simulationController.pack();

		simulationController.runSimulationButton.requestFocusInWindow();
		simulationController.setLocation(800, 20);

		simulationController.setVisible(true);
	}

	public static void main(String[] args) {
		System.out.println("******\nPlease make sure I have lots of memory by adding the flag '-Xmx10G' to the java command! Thanks!\n*****");
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				createAndShowGUI();
			}
		});
	}

}
