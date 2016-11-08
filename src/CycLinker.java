import java.io.IOException;


public class CycLinker {
	/* This program takes 4 arguments:
	 * args[0]: graph input file --> details in 'InputReader' file.
	 * args[1]: start/end points --> details in 'InputReader' file.
	 * args[2]: output file name prefix --> details in 'OutputReader' file.
	 * args[3]: maximum number of paths to write --> details in 'Algorithm' file.
	 */
	public static void main(String[] args)  throws IOException {
		//Read in arguments
		String graphFileName = args[0];
		String startEnd = args[1];
		String outputPrefix = args[2];
        int maxk = Integer.parseInt(args[3]);
        long startTime;
        long endTime;
        long duration;
		
		//Read input; build graph.
        startTime = System.nanoTime();
		InputReader input = new InputReader(graphFileName, startEnd);
        endTime = System.nanoTime();

        // divide by 1000000 to get miliseconds
        duration = (endTime - startTime) / 1000000;
        System.out.println("Total time taken to read input: " + duration);
		
		//Execute the algorithm.
        startTime = System.nanoTime();
		Algorithm execute = new Algorithm(input, maxk);
		execute.run();
        endTime = System.nanoTime();
        duration = (endTime - startTime) / 1000000;
        System.out.println("Total time taken to run: " + duration);
		
		//Output results to file.
        startTime = System.nanoTime();
		OutputWriter print = new OutputWriter(execute, outputPrefix);
		print.printToFile();		
        endTime = System.nanoTime();
        duration = (endTime - startTime) / 1000000;
        System.out.println("Total time taken to write the output file: " + duration);
	}
}
