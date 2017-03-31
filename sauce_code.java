import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Enumeration;
import java.util.Collections;
import java.io.IOException;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.math.RoundingMode;


public class sauce_code
{
	private Hashtable<String, Integer> support_value = new Hashtable<String,Integer>();
	private Hashtable<String, ArrayList<String>> parsed_callgraph;
	private HashSet<String> allValues = new HashSet<String>();
	static int T_SUPPORT = 3;
    static float T_CONFIDENCE = 0.65f;
	NumberFormat numf = NumberFormat.getNumberInstance();
	ArrayList<String> prints = new ArrayList<String>();

    public static Hashtable<String,ArrayList<String>> parseFile(String callgraph_loc)
    {
		Hashtable<String, ArrayList<String>> table = new Hashtable<String,ArrayList<String>>();
        try
        {
            FileReader fileRdr = new FileReader(callgraph_loc);
            BufferedReader buf = new BufferedReader(fileRdr);
            String line = buf.readLine();

            int state = 0; //0 - Empty Line, 1 - Call graph
            String current_scope = null;
            while ((line = buf.readLine()) != null)
            {
                switch (state)
                {
                    case(1):
                        if (line.matches("(.*)CS<0x[0-9a-f]*> calls function(.*)"))
                        {
                            String[] scope_list = line.split("\'");
                            String func = scope_list[1];
                            ArrayList<String> curList = table.get(current_scope);
                            if (!curList.contains(func))
                            {
                                curList.add(func);
                            }
                            //System.out.println(func);
                            break;
                        }
                    case(0): //Look for a "function"
       			if (line.startsWith("Call graph node for function"))
                        {
                            String[] scope_list = line.split("\'");
                            current_scope = scope_list[1];
                            ArrayList<String> nlist = new ArrayList<String>();
                            table.put(current_scope, nlist);
                            //System.out.println("Current: " + current_scope);
                            state = 1;
                            //System.out.println(current);
                            break;
       			}
                    default:
               		if (line.length() == 0)
                        {
                            state = 0;
                            //System.out.println("");
       			}
     			break;
                }
            }
        }
        catch(IOException e)
        {
            System.out.println("Error reading file!");
            System.exit(1);
        }
	   return table;
    }
	
	private void parseFromCallGraph(Hashtable<String, ArrayList<String>> parsed_callgraph)
	{
				Enumeration keys = parsed_callgraph.elements();
		while(keys.hasMoreElements())
		{
            @SuppressWarnings("unchecked")
			ArrayList<String> values = (ArrayList<String>)keys.nextElement();
			HashSet<String> valueSet = new HashSet<String>(values); //cuz it only stores unique sets
			values = new ArrayList<String>(valueSet);
			
			for(int i = 0; i < values.size(); i++)
			{
				allValues.add(values.get(i));
				for(int j = i + 1; j < values.size(); j++)
				{
					// order set in alpabetical order if possible
					String pair = pairs(values.get(i), values.get(j)); 
					supportHandler(pair);
				}
				supportHandler(values.get(i));
			}
		}
	}
	
	private String pairs(String i, String j)
	{
		
		// order set in alpabetical order if possible
		String pair = (i.compareTo(j) > 0) ? i + ":" + j : j + ":" + i;
		return pair;
	}
	
	private void supportHandler(String pair)
	{
		
		Integer support = support_value.get(pair);
		if(support == null)
		{
			support_value.put(pair, 1);
		}
		else 
		{
			support_value.put(pair, new Integer(support + 1));
		}
	}
	
	public void findBugs(Hashtable<String, ArrayList<String>> parsed_callgraph)
	{
		System.out.println("eg for inni findbugs");
		System.out.println("T_conf: " + T_CONFIDENCE);
		System.out.println("T_supp: " + T_SUPPORT);	
		Enumeration<String> callgraphKeySet = parsed_callgraph.keys();
		while(callgraphKeySet.hasMoreElements())
		{
			String caller = (String)callgraphKeySet.nextElement();
			ArrayList<String> callsL = (ArrayList<String>)parsed_callgraph.get(caller);
			HashSet<String> calls = new HashSet<String>(callsL);

			Iterator i = calls.iterator();
			while(i.hasNext())
			{
				String f = (String)i.next();
				Iterator<String> iter = allValues.iterator();
				while(iter.hasNext())
				{
					String f2 = iter.next();
					String key = pairs(f, f2);					
					if(!support_value.containsKey(key) || !support_value.containsKey(f))
					{
						System.out.println("for inni if setningu 1");
						continue;
					}
				
					int pairSupport = support_value.get(key).intValue();
					int singleSupport = support_value.get(f).intValue();
					float confidence = (float)pairSupport/singleSupport;
					printBugs(caller, f, f2, pairSupport, confidence);

					if(confidence >= T_CONFIDENCE &&  pairSupport >= T_SUPPORT)
					{
						System.out.println("for inni if setningu 2");

						if(!calls.contains(f2))
						{
							System.out.println("for inni if setningu 3");
							printBugs(caller, f, f2, pairSupport, confidence);
						}
					}
				}
			}
		}
	}

	private void printBugs(String caller, String f, String f2, int support, float confidence)
	{
		System.out.println("eg for inni printbugs");	
		String pair;
		if(f.compareTo(f2) > 0)
		{
			pair = f2 + ", " + f;
		}
		else
		{
			pair = f + ", " + f2;
		}
		prints.add("bug: " + f + " in " + caller + ", " + "pair: (" + pair + "), support: " + support + ", confidence: " + numf.format(confidence * 100.00) + "%");
	}

    public void flushPrint()
    {
    	Collections.sort(prints);
    	for(int i = 0; i < prints.size(); i++)
    	{
    		System.out.println(prints.get(i));
    	}
    }

    public void run(String callgraph_location)
    {
        numf.setMaximumFractionDigits(2);
        numf.setMinimumFractionDigits(2);
        numf.setRoundingMode(RoundingMode.HALF_EVEN);

    	Hashtable<String, ArrayList<String>> parsed_callgraph = parseFile(callgraph_location);
    	parseFromCallGraph(parsed_callgraph);
    	findBugs(parsed_callgraph);
    	flushPrint();
    }

    public static void main(String [] args)
    {
        /*Map<String, Integer> functionHashMap = new HashMap<String, Integer>();*/
        /*Map<Pair, Integer> functionPairHashMap = new HashMap<Pair, Integer>();*/
        /*Map<String, List<String>> likelyInvariants = new HashMap<String, List<String>>();*/
        /*List<Scope> scopes = new ArrayList<Scope>();*/
        sauce_code sauce_hot = new sauce_code();
        // default arguments
        String callgraph = "empty file";

        // Process command line args
        if(args.length < 1)
        {
            System.out.println("Usage: java sauce_code <bitcode_file>");
            return;
        }
        if(args.length >= 2)
        {
            T_SUPPORT = Integer.parseInt(args[1]);
        }
        if(args.length >= 3)
        {
            T_CONFIDENCE = (float)Integer.parseInt(args[2]) / 100;
        }
        callgraph = args[0] + ".callgraph"; // inside pipair file, it generates nameOfTheTest.bc.callgraph using OPT command
        sauce_hot.run(callgraph);
    }
}
