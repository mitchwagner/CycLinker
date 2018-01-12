import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
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
	 * args[0]: graph input file (details in 'InputReader' file)
	 * args[1]: start/end points (details in 'InputReader' file)
	 * args[2]: output file name prefix (details in 'OutputReader' file)
	 * args[3]: maximum number of paths to write (details in 'Algorithm' file)
	 */
	public static void main(String[] args) 
	        throws IOException, ParseException {
		
        CommandLine cmd = getCommandLineOptions(args);
        runWithParameters(cmd);

        // Jeff wanted:
        // time to run all algorithms
        // time to run each algorithm
        // time to write all outputs
        // time to write each output
        // avg time to run an algorithm
        // avg time to write an output
	}

	public static void runWithParameters(CommandLine cmd) 
			throws IOException, ParseException {

	    InputReader input = readGraphFromParams(cmd);

	    ArrayList<String> sourceTargetFiles = getSourcesAndTargets(cmd, input);
	    ArrayList<String> outputPrefixes = getOutputPrefixes(cmd, input);

        validateParameters(sourceTargetFiles, outputPrefixes, input, cmd);

        runAlgorithOverSourceTargetPairs(
            sourceTargetFiles, outputPrefixes, input, cmd);
	}

	public static InputReader readGraphFromParams(CommandLine cmd) 
	        throws IOException {
		String graphFileName = cmd.getOptionValue("network");
        double edgePenalty = getEdgePenaltyFromCommandLine(cmd);
		return  readGraph(graphFileName, edgePenalty); 
	}

    public static InputReader readGraph(String graphFileName, 
            Double edgePenalty) throws IOException {

        InputReader input = new InputReader(graphFileName, edgePenalty); 
        return input;
    }

    public static ArrayList<String> getSourcesAndTargets(CommandLine cmd,
            InputReader input) throws IOException {

        boolean multiRun = cmd.hasOption("multi-run");
		String arg = cmd.getOptionValue("start-ends");
		
		ArrayList<String> sourceTargetFiles = null; 

		if (multiRun) {
			sourceTargetFiles = parseFileList(arg);
		}
		else {
			sourceTargetFiles = new ArrayList<String>();
			sourceTargetFiles.add(arg);

		}

		return sourceTargetFiles;
    }


    public static ArrayList<String> getOutputPrefixes(CommandLine cmd,
            InputReader input) throws IOException {
		boolean multiRun = cmd.hasOption("multi-run");
		String arg = cmd.getOptionValue("out-prefix");

		ArrayList<String> outputPrefixes = null;
		if (multiRun) {
			outputPrefixes = parseFileList(arg);
		}
		else {
			outputPrefixes = new ArrayList<String>();
			outputPrefixes.add(arg);
		}

		return outputPrefixes;
    }

	public static void validateParameters(ArrayList<String> sourceTargetFiles,
			ArrayList<String> outputPrefixes, InputReader input, 
			CommandLine cmd) throws IOException, ParseException {

        ensureListsSameLength(sourceTargetFiles, outputPrefixes);

        boolean startEndsPenalty = cmd.hasOption("start-ends-penalty");

        ensureSourcesAndTargetsInNetwork(
            sourceTargetFiles, input, startEndsPenalty);
	} 

	public static void ensureListsSameLength(List stFiles, List outputPrefixes) 
			throws ParseException {
		int sizeA = stFiles.size();
		int sizeB = outputPrefixes.size();	

		if (sizeA != sizeB) {
            throw new ParseException("# of source/target files does not " + 
                "equal the number of output prefixes: S-T-Files: " + sizeA + 
                " Output Prefixes: " + sizeB);
		}	
	} 

    /**
     * Make sure all source/target set pairs have sources and targets in the
     * network. Takes advantage of the InputReader function to add a source/
     * target file's data. 
     */
    public static void ensureSourcesAndTargetsInNetwork(
            ArrayList<String> stFiles, InputReader input, 
            boolean startEndsPenalty) throws IOException {
        
        boolean verbose = true;

        for (int i = 0; i < stFiles.size(); i++) {
                input.AddStartEnd(
                    stFiles.get(i), startEndsPenalty, verbose);
                input.RemoveStartEnd();
        }
    }

    public static void runAlgorithOverSourceTargetPairs(ArrayList<String> 
        stFiles, ArrayList<String> outputPrefixes, InputReader input,
        CommandLine cmd) throws IOException {

        long maxk = getKfromCommandLine(cmd);
        boolean startEndsPenalty = cmd.hasOption("start-ends-penalty"); 

        boolean verbose = true;

        for (int i = 0; i < stFiles.size(); i++){
            // read the start end file
            input.AddStartEnd(stFiles.get(i), startEndsPenalty, verbose);
		
            Algorithm execute = new Algorithm(input, maxk);

            runAlgorithm(execute);
    
            writeResultToFile(execute, outputPrefixes.get(i));

            // remove the sources and targets from the graph for the next run 
            input.RemoveStartEnd();
        }

    }

    public static void writeResultToFile(Algorithm alg, String outPrefix) 
            throws IOException {

        OutputWriter print = new OutputWriter(alg, outPrefix);

        print.printToFile();
    }

    public static void runAlgorithm(Algorithm alg) {
        alg.run();
    }


    public static CommandLine getCommandLineOptions(String[] args) 
            throws ParseException {
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

    public static void handleParseException(ParseException e) 
            throws ParseException {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("QuickLinker", getOptions());
        throw e;
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
		    "Maximum number of paths to output. " + 
		    "Default is to write all paths and edges");
		return option;
    }

    public static Option getMultiRunOption() {
		Option option = new Option("m", "multi-run", false, 
		    "Option to read multiple start-ends file from the " +
		    "--start-ends option");

		return option;
    }

    public static Option getEdgePenaltyOption() {
		Option option = new Option("e", "edge-penalty", true, 
		    "Not yet implemented! Add the natural log of the specified " +
		    "penalty to the cost of each edge. This will effectively " +
		    "increase the cost of each path by the length * edge penalty");

		return option;
    }

    public static Option getSourceTargetPenaltyOption() {
		Option option = new Option("p", "start-ends-penalty", 
		    false, "Set the cost specified in the third column of the " +
		    "start-ends file to be the cost of the super-source->start and " +
		    "end->super-target edges. Otherwise will be 0");

		return option;
    }

    public static long getKfromCommandLine(CommandLine cmd) {
        if (cmd.hasOption("max-k")){
            return Long.parseLong(cmd.getOptionValue("max-k")); 
        } 
        else {
            return Long.MAX_VALUE;
        }
    }

    public static double getEdgePenaltyFromCommandLine(CommandLine cmd) {
        if (cmd.hasOption("edge-penalty")) { 
            return Double.parseDouble(cmd.getOptionValue("edge-penalty"));
        } 
        // by default, an edge penalty of 1 will not add anything to the
        // cost of an edge (log(1) is 0)
        else {
            return 1; 
        } 
    }

    public static ArrayList<String> parseFileList(String argFile)                         
            throws IOException {                                                   

        Scanner s = new Scanner( new File(argFile));                               
        ArrayList<String> list = new ArrayList<String>();                          

        while (s.hasNext()){                                                       
            list.add(s.next());                                                    
        }                                                                          

        s.close();                                                                 
        return list;                                                               
    } 
}
