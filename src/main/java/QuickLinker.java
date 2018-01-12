import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.ParseException;

public class QuickLinker {

	/** 
	 * Run QuickLinker.
	 * This program takes 3 arguments:
	 * args[0]: graph input file --> details in 'InputReader' file.
	 * args[1]: start/end points --> details in 'InputReader' file.
	 * args[2]: output file name prefix --> details in 'OutputReader' file.
	 * args[3]: maximum number of paths to write --> details in 'Algorithm' file.
	 */
	public static void main(String[] args)  throws IOException {
		
        CommandLine cmd = getCommandLineOptions(args);

		//Read in arguments
		String graphFileName = cmd.getOptionValue("network");
		String startEndsArg = cmd.getOptionValue("start-ends");
		String outputPrefixesArg = cmd.getOptionValue("out-prefix");
		ArrayList<String> startEnds = new ArrayList<String>();
		ArrayList<String> outputPrefixes = new ArrayList<String>();

        long startTime; 
        double inputTime; 
        double algorithmTime = 0; 
        double fileWritingTime = 0;
        boolean verbose = true; 
        double edgePenalty; 

        long maxk = getKfromCommandLine(cmd);

        if (cmd.hasOption("edge-penalty")){ 
            edgePenalty = Double.parseDouble(
                cmd.getOptionValue("edge-penalty")); } else {
            // be default, an edge penalty of 1 will not add anything to the
            // cost of an edge (log(1) is 0)
            edgePenalty = 1; } boolean startEndsPenalty =
            cmd.hasOption("start-ends-penalty"); boolean multiRun =
            cmd.hasOption("multi-run");
		
        //Read input; build graph.
        startTime = System.nanoTime(); 
        InputReader input = new InputReader(graphFileName, edgePenalty); 

        inputTime = (System.nanoTime() - startTime) / 1000000; 
        System.out.println("Time to parse input graph:" + inputTime / 1000 + " sec");

		// get the startEnds and outputPrefixes from the arguments
        if (multiRun){
        	System.out.println("Reading the startEnds and ouputPrefixes from the specified files");
        	// parse the startEnds list and outputPrefixes list from the specified input files
        	startEnds = input.ParseFileList(startEndsArg);
        	outputPrefixes = input.ParseFileList(outputPrefixesArg);
            if (startEnds.size() != outputPrefixes.size()){
                System.out.println("Error: # of startEnds does not equal # of outputPrefixes: " + startEnds.size() + " " + outputPrefixes.size());
                System.exit(1);
            }

            // don't print everything out to the log file
            verbose = false;
            for (int i = 0; i < startEnds.size(); i++){
                // first make sure all of the startEnds have sources and targets in the network
                // read the start end file, which also adds the edges from super-source to sources and from targets to super-target
                input.AddStartEnd(startEnds.get(i), startEndsPenalty, verbose);
                // remove the sources and targets from the graph for the next run 
                input.RemoveStartEnd();
            }
            // we already printed the stats abou the # of sources and targets in the network
            System.out.println("Running Cyclinker on " + startEnds.size() + " sets of sources and targets");
        }
        else{
        	System.out.println("Reading the sources and targets from the file: " + startEndsArg);
        	// put the single startEnd and outputPrefixes into a list
        	startEnds.add(startEndsArg);
        	outputPrefixes.add(outputPrefixesArg);
        }

        for (int i = 0; i < startEnds.size(); i++){
            // read the start end file
            input.AddStartEnd(startEnds.get(i), startEndsPenalty, verbose);
		
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

    public static CommandLine getCommandLineOptions(String[] args) {
        Options options = getOptions();
        CommandLineParser parser = new DefaultParser();

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } 
        catch (ParseException e) {
            handleParseException(e);
        }
        return cmd;
    }

    public static Options getOptions() {
        Options options = new Options();

        options.addOption(getNetworkOption());
        options.addOption(getNodeTypesOption());
        options.addOption(getOutPrefixOption());
        options.addOption(getKLimitOption());
        options.addOption(getMultiRunOption());
        options.addOption(getEdgePenaltyOption());
        options.addOption(getSourceTargetPenaltyOption());

        return options;
    }

    public static void handleParseException(ParseException e) {
        System.out.println(e.getMessage());
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("QuickLinker", getOptions());
        System.exit(1);
    }


    public static Option getNetworkOption() {
		Option option = new Option("n", "network", true, "Input Network");
		option.setRequired(true);

		return option;
    }

    public static Option getNodeTypesOption() {
		Option option = new Option("s", "node-types", true, 
		    "File specifying the nodes to use as sources and targets");

		option.setRequired(true);

		return option;
    }

    public static Option getOutPrefixOption() {
        Option option = new Option("o", "out-prefix", true, 
		    "path/to/prefix for the output files");

		option.setRequired(true);

		return option;
    }

    public static Option getKLimitOption() {
		Option option = new Option("k", "max-k", true, 
		    "Maximum number of paths to output. Default is to write all paths and edges");
		return option;
    }

    public static Option getMultiRunOption() {
		Option option = new Option("m", "multi-run", false, 
		    "Option to read multiple start-ends file from the --start-ends option");

		return option;
    }

    public static Option getEdgePenaltyOption() {
		Option option = new Option("e", "edge-penalty", true, 
		    "Not yet implemented! Add the natural log of the specified penalty to the cost of each edge. This will effectively increase the cost of each path by the length * edge penalty");

		return option;
    }

    public static Option getSourceTargetPenaltyOption() {
		Option option = new Option("p", "start-ends-penalty", 
		    false, "Set the cost specified in the third column of the start-ends file to be the cost of the super-source->start and end->super-target edges. Otherwise will be 0");

		return option;
    }

    public static Long getKfromCommandLine(CommandLine cmd) {
        if (cmd.hasOption("max-k")){
            return Long.parseLong(cmd.getOptionValue("max-k")); 
        } 
        else {
            return Long.MAX_VALUE;
        }
    }
}
