import java.io.IOException;
import java.util.List;
import java.util.ArrayList;


public class CycLinker {
	/* This program takes 3 arguments:
	 * args[0]: graph input file --> details in 'InputReader' file.
	 * args[1]: start/end points --> details in 'InputReader' file.
	 * args[2]: output file name prefix --> details in 'OutputReader' file.
	 * args[3]: maximum number of paths to write --> details in 'Algorithm' file.
	 */
	public static void main(String[] args)  throws IOException {
		//Read in arguments
		String graphFileName = args[0];
		String startEndsArg = args[1];
		String outputPrefixesArg = args[2];
		ArrayList<String> startEnds = new ArrayList<String>();
		ArrayList<String> outputPrefixes = new ArrayList<String>();
        int maxk = Integer.parseInt(args[3]);
        long startTime;
        double inputTime; 
        double algorithmTime = 0;
        double fileWritingTime = 0;
		
		//Read input; build graph.
        startTime = System.nanoTime();
		InputReader input = new InputReader(graphFileName);
        inputTime = (System.nanoTime() - startTime) / 1000000;
        System.out.println("Time to parse graph: " + inputTime / 1000 + " sec");

		// get the startEnds and outputPrefixes from the arguments
        if (args.length > 4 && args[4].equalsIgnoreCase("--multi-run")){
        	System.out.println("Reading the startEnds and ouputPrefixes from the specified files");
        	// parse the startEnds list and outputPrefixes list from the specified input files
        	startEnds = input.ParseFileList(startEndsArg);
        	outputPrefixes = input.ParseFileList(outputPrefixesArg);
        }
        else{
        	// put the single startEnd and outputPrefixes into a list
        	startEnds.add(startEndsArg);
        	outputPrefixes.add(outputPrefixesArg);
        }

        if (startEnds.size() != outputPrefixes.size()){
        	System.out.println("Error: # of startEnds does not equal # of outputPrefixes: " + startEnds.size() + " " + outputPrefixes.size());
        }

        for (int i = 0; i < startEnds.size(); i++){
            // read the start end file
            input.AddStartEnd(startEnds.get(i));
		
            //Execute the algorithm.
            startTime = System.nanoTime();
            Algorithm execute = new Algorithm(input, maxk);
            execute.run();
            // keep adding up the time taken to run the algorithm
            algorithmTime += (System.nanoTime() - startTime) / 1000000;
            //System.out.println("Time (ms) taken to run: " + (System.nanoTime() - startTime) / 1000000000);

            //Output results to file.
            startTime = System.nanoTime();
            OutputWriter print = new OutputWriter(execute, outputPrefixes.get(i));
            print.printToFile();		
            fileWritingTime += (System.nanoTime() - startTime) / 1000000;
            //System.out.println("Time (ms) taken to write output: " + (System.nanoTime() - startTime) / 1000000000);

            // remove the sources and targets from the graph for the next run 
            input.RemoveStartEnd();
        }
        // divide by 1000 to get the time in seconds 
        algorithmTime = algorithmTime / 1000;
        fileWritingTime = fileWritingTime / 1000;
        System.out.println("Total time to run algorithm: " + algorithmTime + " sec. Avg per run: " + algorithmTime / startEnds.size() + " sec");
        System.out.println("Total time to write output: " + fileWritingTime + " sec. Avg per run: " + fileWritingTime / startEnds.size() + " sec");
	}
}
