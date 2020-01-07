package tutorialGeneration.MCTSRewardEvolution;

import java.io.BufferedWriter;

//for use on the NYU HPC server


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import tutorialGeneration.MCTSRewardEvolution.evaluator.ParentEvaluator;


public class ADPParentRunner {

	private static void resetAllFolders(String in, String out, String sig) {
		deleteDirectory(new File(in));
		deleteDirectory(new File(out));
		deleteDirectory(new File(sig));
		new File(in).mkdir();
		new File(out).mkdir();
		new File(sig).mkdir();
	}
	
	//clear everything from a specified directory
	private static void deleteDirectory(File directoryToBeDeleted) {
		File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectory(file);
			}
		}
		directoryToBeDeleted.delete();
	}
	
	//parse the parameters from the external file
	private static HashMap<String, String> readParameters(String filename) throws IOException {
		List<String> lines = Files.readAllLines(Paths.get("", filename));
		HashMap<String, String> parameters = new HashMap<String, String>();
		for(int i=0; i<lines.size(); i++) {
			if(lines.get(i).trim().length() == 0) {
				continue;
			}
			String[] parts = lines.get(i).split("=");
			parameters.put(parts[0].trim(), parts[1].trim());
		}
		return parameters;
	}
	
	//parse csv file
	public static HashMap<Integer, String[]> readGamesCSV(String csv) throws IOException{
		HashMap<Integer, String[]> gameSet = new HashMap<Integer, String[]>();
		List<String> lines = Files.readAllLines(Paths.get("", csv));
		for(int i=0;i<lines.size();i++) {
			//skip empty lines
			if(lines.get(i).trim().length() == 0)
				continue;
			
			//grab the game name from the end of the directory string
			String[] partStr = lines.get(i).split(",");
			String[] dir = partStr[1].split("/");
			String game_name = dir[dir.length-1].replace(".txt", "");
			
			//add the index of the csv and the string array of the game path and the game name
			gameSet.put(Integer.parseInt(partStr[0]), new String[]{game_name, partStr[1]});
		}
		
		return gameSet;
	}
	
	//export the map to a file
	public static void simpleExportMap(CMEMapElites m, String outFile) throws Exception{
		BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));
		bw.write(m.toString());
		bw.close();
	}
	
	//create a set of directories from a list of strings
	public static void createDirectories(String[] dir) {
		for(String d : dir) {
			File f = new File(d);
			if(!f.exists())
				f.mkdir();
		}
	}
	
	public static void main(String[] args)  throws IOException{
		////////////////		IMPORT PARAMETERS         //////////////
		
		//import the parameter values
		HashMap<String, String> parameters = null;
		try {
			parameters = readParameters("MCTSRewardEvolutionParameters.txt");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		//parse the parameters
		Random seed = new Random(Integer.parseInt(parameters.get("seed")));
		int gameIndex = Integer.parseInt(parameters.get("gameIndex"));
		int popSize = Integer.parseInt(parameters.get("populationSize"));
		double coinFlip = Double.parseDouble(parameters.get("coinFlip"));
		int exportFreq = Integer.parseInt(parameters.get("exportFreq"));
		
		//import the game list
		HashMap<Integer, String[]> gameList = readGamesCSV(parameters.get("gameListCSV"));
		
		/////////////		START OF TUTORIAL LEVEL GENERATION   	/////////////////
		//create all the directories beforehand
		createDirectories(new String[]{parameters.get("inputFolder"),
		                   parameters.get("outputFolder"),
		                   parameters.get("resultFolder")
							});
				
		
		//get the game name and game location
		String gameName = gameList.get(gameIndex)[0];
		String gameLoc = gameList.get(gameIndex)[1];
		
		//setup map elites and the first chromosomes
		CMEMapElites map = new CMEMapElites(gameName, gameLoc, seed, coinFlip, parameters);
		ParentEvaluator parent = new ParentEvaluator(parameters.get("inputFolder"), parameters.get("outputFolder"), parameters.get("signalFolder"));
		System.out.println("First Batch of Chromosomes");
		Chromosome[] chromosomes = map.randomChromosomes(popSize, parameters.get("generatorFolder") + "init_ph.txt");
		
		//set iteration count
		int iteration = 0;
		int maxIterations = -1;
		if(args.length > 0) {
			maxIterations = Integer.parseInt(args[0]);
		}
		
		//delete old folders 
		System.out.println("P: Resetting input/output folders...");
		resetAllFolders(parameters.get("inputFolder"), parameters.get("outputFolder"), parameters.get("signalFolder"));
		int childCount = 0;


			// check for children alive, TODO move this somewhere else probably

		//run forever, or until all the iterations have been completed
		while(true) {
			try {
				// send awake signal and wait 5 seconds for responses
				childCount = callOut(parent);
				System.out.println("\n\nITERATION #" + iteration + "/" + maxIterations);
				
				// 1p) export the chromosomes to the files for the children
				// 		in the form [age\n hasborder\n level]
				System.out.println("P: Writing in files for children...");
				String[] levelOut = new String[chromosomes.length];
				for(int i=0;i<chromosomes.length;i++) {
					levelOut[i] = chromosomes[i].toInputFile(chromosomes[i].getIndex(), 0);
				}
				
				// 2p-5p) wait for the children to return results from running the AI agent
				parent.writeChromosomes(levelOut, childCount);
				// delete block
				System.out.println("P: Waiting for children to finish...");
				while(!parent.checkChromosomes(childCount)) {
					Thread.sleep(500);
				}
				
				// 6p) read in the results of the child output chromosomes
				System.out.println("P: Reading children results...");
				String[] values = parent.assignChromosomes();
				for(int i=0; i<chromosomes.length; i++) {
					chromosomes[i].saveResults(values[i]);
				}
								
				System.out.println("P: Checking population for further assessment...");
				ArrayList<Tuple> furtherReview = map.checkForFurtherReview(chromosomes);
				parent.clearOutputFiles(chromosomes.length);
				if (furtherReview.size() > 0) {
					
					// 6.5p) do the further review
					// TODO change this to a list, to be more dynamic
					ArrayList<String> outTemp = new ArrayList<String>();
//					levelOut = new String[furtherReview.size()];

					int i = 0;
			        for (Tuple tup : furtherReview) { 
			            Chromosome chrome = tup.getChrome(); 
			            for(int j = 0; j < tup.getCount(); j++) {
			            	outTemp.add(chrome.toInputFile(chrome.getIndex(), j));
			            	i++;
			            }
			        } 
			        levelOut = outTemp.toArray(levelOut);
					
					callOut(parent);
					
					System.out.println("P: Assigning further assessment...");
					parent.writeChromosomes(levelOut, childCount);
					System.out.println("P: Waiting for children to finish...");
					while(!parent.checkChromosomes(childCount)) {
						Thread.sleep(500);
					}
					// TODO combine the chromosome runs with the same index into one merged chromosome again

					values = parent.assignChromosomes();
					chromosomes = mergeChromes(values, chromosomes);
					
				}
				
				
				// 7p) assign the chromosomes to the MAP
				System.out.println("P: Assigning chromosomes to the MAP...");
				map.assignChromosomes(chromosomes);
				
				
				// 8p) write map results and info to the results folder (done every x iterations)
				if(iteration % exportFreq == 0 || iteration == 0) {
					System.out.println("P: Writing results...");
					File f = new File(parameters.get("resultFolder") + iteration + "/");
					f.mkdir();
					map.deepExport(parameters.get("resultFolder") + iteration + "/");
//					deleteDirectory(new File(parameters.get("resultFolder") + (iteration - exportFreq) + "/"));
				}
				
				//if completed all iterations, then finish
				if(maxIterations > 0 && iteration >= maxIterations) {
					break;
				}
				
				// 9p) otherwise generate a new batch of chromosomes
				System.out.println("P: Generating next batch...");
				chromosomes = map.makeNextGeneration(popSize);
				iteration += 1;
				
				// 10p) repeat back to (1p)
				
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public static int callOut(ParentEvaluator parent) {
		System.out.println("P: Calling out into the void...");
		parent.sendAwakeRequest();
		int childCount = 0;
		do {
			try {
				Thread.sleep(5000);
			} catch(InterruptedException e1) {
				e1.printStackTrace();

			}
			childCount = parent.checkChildrenAlive();

		}while (childCount < 1);
		// make blocks and start messages
		childCount += parent.checkChildrenAlive();
		parent.removeAwakeSignal();
		System.out.println("Children alive: " + childCount);
		
		return childCount;
	}
	
	public static Chromosome[] mergeChromes(String[] values, Chromosome[] oldChromosomes) {
		Chromosome[] newChromosomes = new Chromosome[oldChromosomes.length];
		
		for(String val : values) {
			String[] valA = val.split("\n");
			oldChromosomes[Integer.parseInt(valA[5])].addFitness(Double.parseDouble(valA[2]));
		}
		return newChromosomes;
	}
}
