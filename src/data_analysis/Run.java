package data_analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class Run {

	private double[][] data;
	private final int numBots;
	private final int numSurs;

	private Run(int numTimesteps, int numCols, int _numSurs, int _numBots) {
		data = new double[numTimesteps][numCols];
		numBots = _numBots;
		numSurs = _numSurs;
	}

	public int getNumRows() {
		return data.length;
	}

	public int getNumCols() {
		return data[0].length;
	}

	public double getValue(int r, int c) {
		try {
			return data[r][c];
		} catch(ArrayIndexOutOfBoundsException e) {
			System.out.println("Got out of bounds exception");
			return -1;
		}
	}


	/**
	 * @return the numBots
	 */
	public int getNumBots() {
		return numBots;
	}

	/**
	 * @return the numSurs
	 */
	public int getNumSurs() {
		return numSurs;
	}

	public void addRow(String row) {
		//split the string into columns
		String[] splitRow = row.split("\t");

		//store this row, converting to values
		int rowNum = Integer.parseInt(splitRow[0]);

		for(int i = 0; i < splitRow.length; i++) {
			try {
				data[rowNum][i] = Double.parseDouble(splitRow[i]);
			} catch (NumberFormatException e) {
				//can't read this number
				//put nan
				data[rowNum][i] = Double.NaN;
			}
		}
	}

	public static Run readFromFile(File file, int numSurs, int numBots) {
		//read the last line of the file, to get number of timesteps and number of columns
		Run run = null;
		try {
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			long maxLineLength = 100;
			long position = raf.length() - maxLineLength;
			raf.seek(position);
			String lastLine = null;
			String s;
			while((s = raf.readLine()) != null) {
				lastLine = s;
			}
			raf.seek(0);
			raf.close();

			//lastLine should now be the complete last line of the file
			//read the relevant data, and make our Run
			String[] splitLastLine = lastLine.split("\t");
			//			System.out.println(file.getAbsolutePath());
			//			System.out.println("'" + lastLine + "'");
			//if we get to timestep 100, there are actually 101 timesteps, because we need to count 0
			int numTimesteps = Integer.parseInt(splitLastLine[0]) + 1;

			run = new Run(numTimesteps, splitLastLine.length, numSurs, numBots);

			//read each line in order, and add them to the Run
			BufferedReader read = new BufferedReader(new FileReader(file));
			String curLine;
			while( (curLine = read.readLine()) != null) {
				run.addRow(curLine);
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return run;
	}

	public static double getMax(List<Run> runs, int row, int col) {
		double maxVal = runs.get(0).getValue(row, col);
		for(Run run : runs) {
			double curVal = run.getValue(row, col);
			if(curVal > maxVal) 
				maxVal = curVal;
		}

		return maxVal;
	}


	public static double getMin(List<Run> runs, int row, int col) {
		double minVal = runs.get(0).getValue(row, col);
		for(Run run : runs) {
			double curVal = run.getValue(row, col);
			if(curVal < minVal) 
				minVal = curVal;
		}

		return minVal;
	}

	public static double getAvg(List<Run> runs, int row, int col) {
		double avgVal = 0.0;
		for(Run run : runs) {
			avgVal += run.getValue(row, col);
		}
		avgVal /= runs.size();

		return avgVal;
	}

	public static double getStdDev(List<Run> runs, int row, int col) {
		double average = getAvg(runs, row, col);

		double squaredDiffSum = 0.0;
		for(Run run : runs) {
			squaredDiffSum += Math.pow(run.getValue(row, col) - average, 2);
		}

		double variance = squaredDiffSum / runs.size();

		return Math.sqrt(variance);
	}

	/**
	 * Get the average value of all values in the specified column for rows between start row and end row
	 * @param runs
	 * @param startRow
	 * @param endRow
	 * @param col
	 * @return
	 */
	public static double getAvg(List<Run> runs, int startRow, int endRow, int col) {
		//basically, get the average for all rows in the range
		//and average them together
		double avgSum = 0.0;
		for(Run run : runs) {
			for(int i = startRow; i <= endRow; i++) {
				avgSum += run.getValue(i, col);
			}
		}
		return avgSum / (runs.size()*(endRow - startRow));
	}


}
