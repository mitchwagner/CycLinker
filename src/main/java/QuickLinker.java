import java.io.IOException;
import org.apache.commons.cli.ParseException;

public class QuickLinker {

	/** 
	 * Run QuickLinker.
	 */
	public static void main(String[] args) 
	        throws IOException, ParseException {

	    Parser parser = new Parser(args);
	    parser.parse();	
	}
}
