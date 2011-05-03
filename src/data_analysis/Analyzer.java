package data_analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Analyzer {

	public static void main(String[] args) {
		//args should be the location of the data directory
		if(args.length != 1) {
			System.out.println("Incorrect number of arguments passed");
			System.exit(0);
		}

		//make sure the passed string is a valid directory
		File dataDir = new File(args[0]);

		if(! dataDir.isDirectory()) {
			System.out.println("Argument is not a directory path");
			System.exit(0);
		}

		//make sure the directory is called "data"
		if(! dataDir.getName().equals("data")) {
			System.out.println("Directory passed does not have the correct name");
			System.exit(0);
		}

		//now we can start the traversal
		NumberDirFileFilter numFileFilter = new NumberDirFileFilter();
		File[] surDirs = dataDir.listFiles(numFileFilter);

		for(File curSurDir : surDirs) {
			int curNumSur = Integer.parseInt(curSurDir.getName());
			//get their Bot sub directories
			File[] botDirs = curSurDir.listFiles(numFileFilter);
			
			for(File curBotDir : botDirs) {
				int curNumBots = Integer.parseInt(curBotDir.getName());
				
				//each sub directory here should be a Run
				//don't bother checking name validity - not worth it
				//TODO check for valid dir names
				File[] runDirs = curBotDir.listFiles(new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						return pathname.isDirectory();
					}
				});
				
				System.out.println("Staring to analyze sur = "+ curNumSur + " bots = " + curNumBots);
				
				List<Run> runs = new ArrayList<Run>();
				
				for(File curRunDir : runDirs) {
					runs.add(Run.readFromFile(new File(curRunDir,"data.txt"), curNumSur, curNumBots));
				}
				
				System.out.println("sur = " + curSurDir.getName() + "\tbot = " + curBotDir.getName());
				
				//we should now have all runs for the given number of bots and survivors
				//start creating our averages file
				try {		
					/* Note To Self: data column assignments
					 * 0 : timestep
					 * 1 : Start time in seconds since 1970
					 * 2 : % sur found
					 * 3 : Path quality metric
					 * 4 : Path Marking Metric
					 * 5 : Overall Metric
					 */

					/* Output column assignments
					 * 0 : timestep
					 * 1 : average value
					 * 2 : low value
					 * 3 : high value
					 */
					
					BufferedWriter surFoundOut = new BufferedWriter(new FileWriter(new File(curBotDir, "surFound.dat"), false));
					BufferedWriter pathQualOut = new BufferedWriter(new FileWriter(new File(curBotDir, "pathQal.dat"), false));
					BufferedWriter pathMarkOut = new BufferedWriter(new FileWriter(new File(curBotDir, "pathMark.dat"), false));
					BufferedWriter overallOut  = new BufferedWriter(new FileWriter(new File(curBotDir, "overall.dat"), false));
					
					for(int timestep = 0; timestep < runs.get(0).getNumRows(); timestep++) {
						surFoundOut.write(timestep + "\t" + Run.getAvg(runs, timestep, 2) + "\t" + Run.getMin(runs,timestep, 2) + "\t" + Run.getMax(runs, timestep, 2));
						surFoundOut.newLine();
						
						pathQualOut.write(timestep + "\t" + Run.getAvg(runs, timestep, 3) + "\t" + Run.getMin(runs,timestep, 3) + "\t" + Run.getMax(runs, timestep, 3));
						pathQualOut.newLine();
						
						pathMarkOut.write(timestep + "\t" + Run.getAvg(runs, timestep, 4) + "\t" + Run.getMin(runs,timestep, 4) + "\t" + Run.getMax(runs, timestep, 4));
						pathMarkOut.newLine();
						
						overallOut.write(timestep + "\t" + Run.getAvg(runs, timestep, 5) + "\t" + Run.getMin(runs,timestep, 5) + "\t" + Run.getMax(runs, timestep, 5));
						overallOut.newLine();
					}
					
					//close up the files
					surFoundOut.close();
					pathQualOut.close();
					pathMarkOut.close();
					overallOut.close();
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}


	private static class NumberDirFileFilter implements FileFilter {

		@Override
		public boolean accept(File pathname) {
			//we want directories
			if(! pathname.isDirectory()) return false;
			//we also want them to have just a number as their name
			try {
				Integer.parseInt(pathname.getName());
			} catch(NumberFormatException e) {
				return false;
			}
			return true;
		}

	}



}
