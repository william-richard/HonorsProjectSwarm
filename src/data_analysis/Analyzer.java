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
			
			//create the avgOverall file
			File avgOverall = new File(curSurDir, "avgOverall.dat");
			//delete it so we're sure we're starting with a clean slate
			avgOverall.delete();
			
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
					
					BufferedWriter surFoundRunOut = new BufferedWriter(new FileWriter(new File(curBotDir, "surFound.dat"), false));
					BufferedWriter pathQualRunOut = new BufferedWriter(new FileWriter(new File(curBotDir, "pathQal.dat"), false));
					BufferedWriter pathMarkRunOut = new BufferedWriter(new FileWriter(new File(curBotDir, "pathMark.dat"), false));
					BufferedWriter overallRunOut  = new BufferedWriter(new FileWriter(new File(curBotDir, "overall.dat"), false));
					
					
					for(int timestep = 0; timestep < runs.get(0).getNumRows(); timestep++) {
						surFoundRunOut.write(timestep + "\t" + Run.getAvg(runs, timestep, 2) + "\t" + Run.getMin(runs,timestep, 2) + "\t" + Run.getMax(runs, timestep, 2));
						surFoundRunOut.newLine();
						
						pathQualRunOut.write(timestep + "\t" + Run.getAvg(runs, timestep, 3) + "\t" + Run.getMin(runs,timestep, 3) + "\t" + Run.getMax(runs, timestep, 3));
						pathQualRunOut.newLine();
						
						pathMarkRunOut.write(timestep + "\t" + Run.getAvg(runs, timestep, 4) + "\t" + Run.getMin(runs,timestep, 4) + "\t" + Run.getMax(runs, timestep, 4));
						pathMarkRunOut.newLine();
						
						overallRunOut.write(timestep + "\t" + Run.getAvg(runs, timestep, 5) + "\t" + Run.getMin(runs,timestep, 5) + "\t" + Run.getMax(runs, timestep, 5));
						overallRunOut.newLine();
					}
					
					//close up the files
					surFoundRunOut.close();
					pathQualRunOut.close();
					pathMarkRunOut.close();
					overallRunOut.close();
					
					//now write to the average overall file
					//make sure to append to the end
					BufferedWriter avgOverallSurOut = new BufferedWriter(new FileWriter(avgOverall, true));
					//write our number of bots and the avg Overall metric value for all of this bot number's runs
					avgOverallSurOut.write(curNumBots + "\t" + Run.getAvg(runs, 200, 1800, 5));
					avgOverallSurOut.newLine();
					avgOverallSurOut.close();
					
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
