package tutorialGeneration.MCTSRewardEvolution.evaluator;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ChildEvaluator {
	private int _id;
	private int _size;
	private String _inputFolder;
	private String _outputFolder;
	private String _signalFolder;

	public ChildEvaluator(int id, int size, String inputFolder, String outputFolder, String signalFolder) {
		this._id = id;					//unique id of the child 
		this._size = size;				//how many levels this  child will handle
		this._inputFolder = inputFolder;
		this._outputFolder = outputFolder;
		this._signalFolder = signalFolder;
	}

	//check if all the input files for this child have been created yet
	public boolean checkChromosomes() {
		File file = new File(this._signalFolder + (this._id) + ".request");
		if(!file.exists()) {
			return false;
		}		
		return true;
	}
	
	public boolean checkStartRequest() {
		File file = new File(this._signalFolder + "0.request");
		if(!file.exists()) {
			return false;
		}
		return true;
	}

	//read in the string input chromosomes produced by the parents
	public String[] readChromosomes() throws IOException {
		// look thru all files and find the ones that have the right id
		File dir = new File(this._inputFolder);
		List<File> toDo = new ArrayList<File>();
		for(File f : dir.listFiles()) {
			String id = f.getName().split("_")[0];
			if (Integer.parseInt(id) == this._id) {
				toDo.add(f);
			}	
		}
		String[] result = new String[toDo.size()];
		for(int i=0; i<this._size; i++) {
			result[i] = String.join("\n", Files.readAllLines(toDo.get(i).toPath()));
		}
		return result;
	}

	//write the results (age, hasborder, constraints, fitness, dimension, and level) back to the parent to assign
	public void writeResults(String[] values) throws FileNotFoundException, UnsupportedEncodingException {
		int startIndex = this._id * this._size;
		for(int i=0; i<values.length; i++) {
			PrintWriter writer = new PrintWriter(this._outputFolder + (startIndex + i) + ".txt", "UTF-8");
			writer.print(values[i]);
			writer.close();
		}
	}

	//delete all the input files that were used to make the chromosomes
	public void clearInputFiles() {
		int startIndex = this._id * this._size;
		for(int i=0; i<this._size; i++) {
			File f = new File(this._inputFolder + (startIndex + i) + ".txt");
			f.delete();
		}
	}
	
	public void sendResponse() {
		File f = new File(this._signalFolder + (this._id) + ".response");
		try {
			f.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
