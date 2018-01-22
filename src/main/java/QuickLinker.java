import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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

// TODO: This entire class should be abstracted out into another class
// that is not static for testability!

public class QuickLinker {

	/** 
	 * Run QuickLinker.
	 */
	public static void main(String[] args) 
	        throws IOException, ParseException {
		
        CommandLine cmd = getCommandLineOptions(args);
        runWithParameters(cmd);

        // TODO: Timing each of the following:
        // time to read input
        // time to run all algorithms
        // time to run each algorithm
        // time to write all outputs
        // time to write each output
        // avg time to run an algorithm
        // avg time to write an output
	}

    /**
     * TODO Add JavaDoc
     */
	public static void runWithParameters(CommandLine cmd) 
			throws IOException, ParseException {

		// First, check to see if RLCSP is provided. If so, we do a different
		// set of things.
        if (cmd.hasOption("rlcsp")) { 
            checkRLCSPParams(cmd);

            InputReaderRLCSP input = readGraphFromParamsRLCSP(cmd);

            long maxk = getKfromCommandLine(cmd);
            AlgorithmRLCSP execute = new AlgorithmRLCSP(input, maxk);

            execute.run();
            
            PrintWriter edgeWriter;
            PrintWriter pathWriter;

		    String prefix= cmd.getOptionValue("out-prefix");
            edgeWriter = new PrintWriter(prefix + "-ranked-edges.txt");
            pathWriter = new PrintWriter(prefix + "-paths.txt");

            edgeWriter.append(execute.edgeOutput);
            pathWriter.append(execute.pathOutput);

            edgeWriter.close();
            pathWriter.close();
        }
        else {
            InputReader input = readGraphFromParams(cmd);

            ArrayList<String> sourceTargetFiles = 
                getSourcesAndTargets(cmd, input);

            ArrayList<String> outputPrefixes = getOutputPrefixes(cmd, input);

            validateParameters(sourceTargetFiles, outputPrefixes, input, cmd);

            runAlgorithmOverSourceTargetPairs(
                sourceTargetFiles, outputPrefixes, input, cmd);
        }
	}

    /**
     * Verify that all parameters necessary for RLCSP QuickLinker have been
     * passed.
     */
	public static void checkRLCSPParams(CommandLine cmd) 
	        throws ParseException {
	    if (!cmd.hasOption("dfa")) {
            throw new ParseException("File containing labeled DFA edgelist " + 
                "must be specified with --dfa <file>");
	    }
	    if (!cmd.hasOption("dfaNodeTypes")) {
            throw new ParseException("File specifying DFA sources and " +
                "targets must be specified with --dfaNodeTypes");
	    }
	}

    /**
     * TODO Add JavaDoc
     */
	public static InputReader readGraphFromParams(CommandLine cmd) 
	        throws IOException {
		String graphFileName = cmd.getOptionValue("network");
        double edgePenalty = getEdgePenaltyFromCommandLine(cmd);
		return readGraph(graphFileName, edgePenalty); 
	}

    /**
     * This method wraps the InputReader constructor.
     * TODO: We can use this method for timing the read.
     */
    public static InputReader readGraph(String graphFileName, 
            Double edgePenalty) throws IOException {

        InputReader input = new InputReader(graphFileName, edgePenalty); 
        return input;
    }

    /**
     * TODO Add JavaDoc
     */
	public static InputReaderRLCSP readGraphFromParamsRLCSP(CommandLine cmd) 
	        throws IOException {
	    File network = new File(cmd.getOptionValue("network"));
	    File networkSourcesTargets = 
	        new File(cmd.getOptionValue("nodeTypes"));

	    File dfa = new File(cmd.getOptionValue("dfa"));
	    File dfaSourcesTargets = 
	        new File(cmd.getOptionValue("dfaNodeTypes"));

	    InputReaderRLCSP input = new InputReaderRLCSP(network, 
	        networkSourcesTargets, dfa, dfaSourcesTargets);

	    return input;
	}

    /**
     * This method wraps the InputReaderRLCSP constructor. 
     * TODO: We can use this method for timing the read.
     */
	public static InputReaderRLCSP readGraphRLCSP(File network,
	        File networkSourcesTargets, File dfa, File dfaSourcesTargets) 
	        throws IOException {

	        InputReaderRLCSP input = new InputReaderRLCSP(network, 
	            networkSourcesTargets, dfa, dfaSourcesTargets);

	        return input;
	}

    /**
     * TODO Add JavaDoc
     */
    public static ArrayList<String> getSourcesAndTargets(CommandLine cmd,
            InputReader input) throws IOException {

        boolean multiRun = cmd.hasOption("multi-run");
		String arg = cmd.getOptionValue("nodeTypes");
		
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

    /**
     * TODO Add JavaDoc
     */
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

    /**
     * TODO Add JavaDoc
     */
	public static void validateParameters(ArrayList<String> sourceTargetFiles,
			ArrayList<String> outputPrefixes, InputReader input, 
			CommandLine cmd) throws IOException, ParseException {

        ensureListsSameLength(sourceTargetFiles, outputPrefixes);

        boolean startEndsPenalty = cmd.hasOption("start-ends-penalty");

        ensureSourcesAndTargetsInNetwork(
            sourceTargetFiles, input, startEndsPenalty);
	} 

    /**
     * Give a list of nodetype files and a list of output prefixes, make sure
     * that the two are the same length.
     */
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

    /**
     * Iterate through all provided nodetype files and run the QuickLinker
     * algorithm on the network, using the sets of sources and targets
     * specified by each respective nodetype file in turn.
     */
    public static void runAlgorithmOverSourceTargetPairs(ArrayList<String> 
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

            // Remove the sources and targets from the graph for the next run 
            input.RemoveStartEnd();
        }

    }

    /**
     * Wraps instantiation and call to print of OutputWriter.
     * TODO: Can be used to time output writing.
     */
    public static void writeResultToFile(Algorithm alg, String outPrefix) 
            throws IOException {

        OutputWriter print = new OutputWriter(alg, outPrefix);

        print.printToFile();
    }

    /**
     * Wraps running of the algorithm.
     * TODO: Can be used to time running of the algorithm. 
     */
    public static void runAlgorithm(Algorithm alg) {
        alg.run();
    }

    /**
     * TODO: Add JavaDoc
     */
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

    /**
     * TODO: Add JavaDoc
     */
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

    /**
     * TODO: Add JavaDoc
     */
    public static void handleParseException(ParseException e) 
            throws ParseException {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("QuickLinker", getOptions());
        throw e;
    }

    /**
     * TODO: Add JavaDoc
     */
    public static Option getNetworkOption() {
		Option option = new Option("n", "network", true, "Input Network");
		option.setRequired(true);

		return option;
    }

    /**
     * TODO: Add JavaDoc
     */
    public static Option getNodeTypesOption() {
		Option option = new Option("nodeTypes", true, 
		    "File specifying the nodes to use as sources and targets");

		option.setRequired(true);

		return option;
    }

    /**
     * TODO: Add JavaDoc
     */
    public static Option getOutPrefixOption() {
        Option option = new Option("o", "out-prefix", true, 
		    "path/to/prefix for the output files");

		option.setRequired(true);

		return option;
    }

    /**
     * TODO: Add JavaDoc
     */
    public static Option getKLimitOption() {
		Option option = new Option("k", "max-k", true, 
		    "Maximum number of paths to output. " + 
		    "Default is to write all paths and edges");
		return option;
    }

    /**
     * TODO: Add JavaDoc
     */
    public static Option getMultiRunOption() {
		Option option = new Option("m", "multi-run", false, 
		    "Option to read multiple start-ends file from the " +
		    "--start-ends option");

		return option;
    }

    /**
     * TODO: Add JavaDoc
     */
    public static Option getEdgePenaltyOption() {
		Option option = new Option("e", "edge-penalty", true, 
		    "Not yet implemented! Add the natural log of the specified " +
		    "penalty to the cost of each edge. This will effectively " +
		    "increase the cost of each path by the length * edge penalty");

		return option;
    }

    /**
     * TODO: Add JavaDoc
     */
    public static Option getSourceTargetPenaltyOption() {
		Option option = new Option("p", "start-ends-penalty", 
		    false, "Set the cost specified in the third column of the " +
		    "start-ends file to be the cost of the super-source->start and " +
		    "end->super-target edges. Otherwise will be 0");

		return option;
    }

    /**
     * TODO: Add JavaDoc
     */
    public static Option getRLCSPOption() {
        Option option = new Option("rlcsp", false,
            "Run QuickLinker RLCSP variant using supplied DFA");

        return option;
    }

    /**
     * TODO: Add JavaDoc
     */
    public static Option getDFAOption() {
        Option option = new Option("dfa", false,
            "Labeled edgelist for DFA");

        return option;
    }

    /**
     * TODO: Add JavaDoc
     */
    public static Option getDFANodeTypesOption() {
        Option option = new Option("dfaNodeTypes", false,
		    "File specifying the nodes to use as sources and targets for" +
		    "the DFA");

        return option;
    }

    /**
     * TODO: Add JavaDoc
     */
    public static long getKfromCommandLine(CommandLine cmd) {
        if (cmd.hasOption("max-k")){
            return Long.parseLong(cmd.getOptionValue("max-k")); 
        } 
        else {
            return Long.MAX_VALUE;
        }
    }

    /**
     * TODO: Add JavaDoc
     */
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
 
    /**
     * TODO: Add JavaDoc
     */
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
