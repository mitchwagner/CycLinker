import java.io.IOException;


public class CycLinker {
	/* This program takes 3 arguments:
	 * args[0]: graph input file --> details in 'InputReader' file.
	 * args[1]: start/end points --> details in 'InputReader' file.
	 * args[2]: output file name prefix --> details in 'OutputReader' file.
	 */
	public static void main(String[] args)  throws IOException {
		//Read in arguments
		String graphFileName = args[0];
		String startEnd = args[1];
		String outputPrefix = args[2];
		
		//Read input; build graph.
		InputReader input = new InputReader(graphFileName, startEnd);
		
		//Execute the algorithm.
		Algorithm execute = new Algorithm(input);
		execute.run();
		
		//Output results to file.
		OutputWriter print = new OutputWriter(execute, outputPrefix);
		print.printToFile();		
	}
}
