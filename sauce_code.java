import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Enumeration;
import java.io.IOException;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Iterator;

public class sauce_code
{
    // holds the support value for a given pair
    private Hashtable<String, Integer> support_value = new Hashtable<String,Integer>();

    // containes the scope (key) and list of function calls (values) for each scope
    private Hashtable<String, ArrayList<String>> parsed_callgraph;

    // holds all key-value pairs for the callgraph
    private HashSet<String> allValues = new HashSet<String>();

    // list to hold all the error messages that are to be printed out to the screen
    ArrayList<String> prints = new ArrayList<String>();

    // default values
    static int T_SUPPORT = 3;
    static double T_CONFIDENCE = 0.65;

    /*
    Creates a HashTable from the callgraph file
    */
    public static Hashtable<String,ArrayList<String>> parseFile(String callgraph_loc)
    {
        Hashtable<String, ArrayList<String>> table = new Hashtable<String,ArrayList<String>>();	

        try
        {
            // read from the callgraph file
            FileReader fileRdr = new FileReader(callgraph_loc);
            BufferedReader buf = new BufferedReader(fileRdr);
            String line = buf.readLine();

            int state = 0;
            String current_scope = null;
            while ((line = buf.readLine()) != null)
            {
                switch (state)
                {
                    // finds out what scope we are in at the moment,
                    // creates a value under a key in the hashtable
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
                            break;
                        }
                    // finds out which function we are calling from within the scope,
                    // creates a key in the hashtable
                    case(0):
                        if (line.startsWith("Call graph node for function"))
                        {
                            String[] scope_list = line.split("\'");
                            current_scope = scope_list[1];
                            ArrayList<String> nlist = new ArrayList<String>();
                            table.put(current_scope, nlist);
                            state = 1;
                            break;
                        }
                    default:
                        if (line.length() == 0)
                        {
                            state = 0;
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
            // get the unique keys
            HashSet<String> valueSet = new HashSet<String>(values);
            values = new ArrayList<String>(valueSet);

            for(int i = 0; i < values.size(); i++)
            {
                allValues.add(values.get(i));

                for(int j = i + 1; j < values.size(); j++)
                {
                    // order set in alpabetical order
                    String pair = sortAlphabetical(values.get(i), values.get(j));
                    supportHandler(pair);
                }

            supportHandler(values.get(i));
            }
        }
    }

    /*
    Orders a given set in alpabetical order
    */
    private String sortAlphabetical(String i, String j)
    {
        String pair = (i.compareTo(j) > 0) ? i + ":" + j : j + ":" + i;
        return pair;
    }

    /*
    Increments the Support value for the pair,
    initializes the Support value if the pair is new
    */
    private void supportHandler(String pair)
    {
        Integer support = support_value.get(pair);
        if(support == null)
        {
            // if the pair is new, we set its Support value as 1
            support_value.put(pair, 1);
        }
        else
        {
            support_value.put(pair, new Integer(support + 1));
        }
    }

    public void findBugs(Hashtable<String, ArrayList<String>> parsed_callgraph)
    {
        // enumeration of the keys in this hashtable.
        Enumeration<String> keyEnum = parsed_callgraph.keys();

        while(keyEnum.hasMoreElements())
        {
            // gets the next key from the set
            String nextKey = (String)keyEnum.nextElement();
            // stores the keys in a list
            ArrayList<String> keyList = (ArrayList<String>)parsed_callgraph.get(nextKey);
            // create a set containing the keys
            HashSet<String> keysSet  = new HashSet<String>(keyList);

            Iterator it = keysSet.iterator();
            while(it.hasNext())
            {
                String nextSet = (String)it.next();
                Iterator<String> iter = allValues.iterator();
                while(iter.hasNext())
                {
                    String nextValue = iter.next();
                    String key = sortAlphabetical(nextSet, nextValue);

                    if(!support_value.containsKey(key) || !support_value.containsKey(nextSet))
                    {
                        continue;
                    }

                    int pairSupport = support_value.get(key).intValue();
                    int singleSupport = support_value.get(nextSet).intValue();
                    double  confidence = (double)pairSupport/singleSupport;

                    if(confidence >= T_CONFIDENCE &&  pairSupport >= T_SUPPORT)
                    {
                        if(!keysSet.contains(nextValue))
                        {
                            addToBugList(nextKey, nextSet, nextValue, pairSupport, confidence);
                        }
                    }
                }
            }
        }
    }

    /*
    Adds the error message to an ArrayList so the messages can be sorted
    before they are printed out onto the screen
    */
    private void addToBugList(String nextKey, String nextSet, String nextValue, int pairSupport, double confidence)
    {
        String pair = nextSet.compareTo(nextValue) > 0 ? nextValue + ", " + nextSet : nextSet + ", " + nextValue;
        String conf = String.format("%.2f", confidence * 100);
        prints.add("bug: " + nextSet + " in " + nextKey + ", " + "pair: (" + pair + "), support: " + pairSupport + ", confidence: " + conf + "%");
    }

    /*
    Sorts the ArrayList of error messages and prints them out onto the screen
    */
    public void printBugs()
    {
        Iterator<String> printIterator = prints.iterator();
        while(printIterator.hasNext())
        {
            System.out.println(printIterator.next());
        }
    }

    public static void main(String [] args)
    {
        sauce_code sauce_hot = new sauce_code();
        String callgraph = null;

        // Process command line args
        if(args.length >= 2)
        {
            T_SUPPORT = Integer.parseInt(args[1]);
        }
        if(args.length >= 3)
        {
            T_CONFIDENCE = (double)Integer.parseInt(args[2]) / 100;
        }

        // inside pipair file, it generates nameOfTheTest.bc.callgraph using OPT command
        callgraph = args[0] + ".callgraph";
        final String callgraph_location = callgraph;
        Hashtable<String, ArrayList<String>> parsed_callgraph = sauce_hot.parseFile(callgraph_location);
        sauce_hot.parseFromCallGraph(parsed_callgraph);
        sauce_hot.findBugs(parsed_callgraph);
        sauce_hot.printBugs();
    }
}
