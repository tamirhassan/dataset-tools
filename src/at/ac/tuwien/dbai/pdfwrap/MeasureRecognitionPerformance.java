// ICDAR 2013 Table Competition
// Author: Tamir Hassan
// Published under the Apache License Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package at.ac.tuwien.dbai.pdfwrap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import at.ac.tuwien.dbai.pdfwrap.ProcessFile;
import at.ac.tuwien.dbai.pdfwrap.analysis.PageProcessor;
import at.ac.tuwien.dbai.pdfwrap.model.document.CharSegment;
import at.ac.tuwien.dbai.pdfwrap.model.document.CompositeSegment;
import at.ac.tuwien.dbai.pdfwrap.model.document.GenericSegment;
import at.ac.tuwien.dbai.pdfwrap.model.document.LineFragment;
import at.ac.tuwien.dbai.pdfwrap.model.document.LineSegment;
import at.ac.tuwien.dbai.pdfwrap.model.document.OpTuple;
import at.ac.tuwien.dbai.pdfwrap.model.document.Page;
import at.ac.tuwien.dbai.pdfwrap.model.document.TextBlock;
import at.ac.tuwien.dbai.pdfwrap.model.document.TextFragment;
import at.ac.tuwien.dbai.pdfwrap.model.document.TextLine;
import at.ac.tuwien.dbai.pdfwrap.pdfread.PDFObjectExtractor;
import at.ac.tuwien.dbai.pdfwrap.utils.ListUtils;
import at.ac.tuwien.dbai.pdfwrap.utils.SegmentUtils;
import at.ac.tuwien.dbai.pdfwrap.utils.Utils;

public class MeasureRecognitionPerformance
{

	public static final String REG = "-reg";
	public static final String STR = "-str";
	public static final String NO_PAGE = "-nopage";
	public static final String DEBUG = "-debug";
	public static final String COMPARE = "-compare";
	public static final String IGNORE_CHARS = "-ignorechars";
	public static final String IGNORE_CHARS_2 = "-ignorechars2";
	public static final String STR_IGNORE = "-strignore";
	
	/**
     * This is the default encoding of the text to be output.
     */
    public static final String DEFAULT_ENCODING =
        //null;
        //"ISO-8859-1";
        //"ISO-8859-6"; //arabic
        //"US-ASCII";
        "UTF-8";
        //"UTF-16";
        //"UTF-16BE";
        //"UTF-16LE";
    
/**
     * Infamous main method.
     *
     * @param args Command line arguments, should be one and a reference to a file.
     *
     * @throws Exception If there is an error parsing the document.
     */
    public static void main(String[] args) throws Exception
    {
        String inPrefix = null;
        String resultFile = null;
        String GTFile = null;
        String PDFFile = null;
        boolean rulingLines = true;
        boolean processSpaces = false;
        boolean compare = false;
        int currentArgumentIndex = 0;
        String password = "";
        String encoding = DEFAULT_ENCODING;
        PDFObjectExtractor extractor = new PDFObjectExtractor();
        int startPage = 1;
        int endPage = Integer.MAX_VALUE;
        boolean toConsole = false;
        boolean str = false;
        boolean debug = false;
        boolean pageCheck = true;
        int normRule = 0;
        
        for( int i=0; i<args.length; i++ )
        {
            if( args[i].equals( STR ) )
            {
                str = true;
            }
            else if( args[i].equals( REG ) )
            {
                str = false;
            }
            else if( args[i].equals( DEBUG ) )
            {
                debug = true;
            }
            else if( args[i].equals( COMPARE ) )
            {
                compare = true;
            }
            else if( args[i].equals( NO_PAGE ) )
            {
                pageCheck = false;
            }
            else if( args[i].equals( IGNORE_CHARS ) )
            {
                normRule = 1;
            }
            else if( args[i].equals( IGNORE_CHARS_2 ) )
            {
                normRule = 2;
            }
            else if( args[i].equals( STR_IGNORE ) )
            {
            	str = true;
                normRule = 1;
            }
            /* Uncomment if switches required
            else if( args[i].equals( ENCODING ) )
            {
                i++;
                if( i >= args.length )
                {
                    usage();
                }
                encoding = args[i];
            }
            else if( args[i].equals( DIALOG ))
            {
            	JFileChooser fcIn = new JFileChooser();
            	// set up the JFileChoosers
        		ExampleFileFilter inFilter = new ExampleFileFilter();
        	    inFilter.addExtension("pdf");
        	    inFilter.setDescription("Portable Document Format");
        	    fcIn.addChoosableFileFilter(inFilter);
//        	    fcIn.setFileFilter(inFilter);
        	    ExampleFileFilter inFilter2 = new ExampleFileFilter();
        	    inFilter2.addExtension("png");
        	    inFilter2.addExtension("tif");
        	    inFilter2.addExtension("tiff");
        	    inFilter2.addExtension("jpg");
        	    inFilter2.addExtension("jpeg");
        	    inFilter2.setDescription("Scanned Image");
        	    fcIn.addChoosableFileFilter(inFilter2);
        	    fcIn.setFileFilter(inFilter);
        	    
            	if (fcIn.showOpenDialog(fcIn) == JFileChooser.APPROVE_OPTION)
            		inFile = fcIn.getSelectedFile().getCanonicalPath();
            }
            */
            else
            {
                if( inPrefix == null )
                {
                    inPrefix = args[i];
                }
                else if (resultFile == null)
                {
                	resultFile = args[i];
                }
                else
                {
                	PDFFile = args[i];
                }
            }
        }

        if( inPrefix == null)
        {
            usage();
        }

        // Generate input file names
        
        if (resultFile == null)
        {
	        PDFFile = inPrefix + ".pdf";
	        if (!str)
	        {
	        	resultFile = inPrefix + "-reg-result.xml";
	        	GTFile = inPrefix + "-reg.xml";
	        }
	        else
	        {
	        	resultFile = inPrefix + "-str-result.xml";
	        	GTFile = inPrefix + "-str.xml";
	        }
        }
        else
        {
        	// all three files specified at command line
        	GTFile = inPrefix;
        }
        
        
        System.out.println("Using     GTFile: " + GTFile);
        System.out.println("Using resultFile: " + resultFile);
        System.out.println("Using    PDFFile: " + PDFFile);
        
        
        // decide whether we have a pdf or image (TODO: command-line override)
        /*
        boolean pdf = true;
		if (inFile.endsWith("png") ||
			inFile.endsWith("tif") ||
			inFile.endsWith("tiff")||
			inFile.endsWith("jpg") ||
			inFile.endsWith("jpeg")||
			inFile.endsWith("PNG") ||
			inFile.endsWith("TIF") ||
			inFile.endsWith("TIFF") ||
			inFile.endsWith("JPG") ||
			inFile.endsWith("JPEG")) pdf = false;
		*/
        
//		System.err.println("Processing: " + inFile);
		
        // load the input files
        File inputResultFile = new File(resultFile);
        File inputGTFile = new File(GTFile);
        
        // parse XML of the input XML file
        
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        
        // new
        /*
        docBuilderFactory.setValidating(true);
        docBuilderFactory.setNamespaceAware(true);
        
        docBuilderFactory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", true);
        */
        // end new
        
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        //new
//        docBuilder.setEntityResolver(FSEntityResolver.instance());
        //end new
        
        Document inputResultDocument = docBuilder.parse(inputResultFile);
        Document inputGTDocument = docBuilder.parse(inputGTFile);
        
        Document resultDocument = null;
        
        
        // set up page processor object
        PageProcessor pp = new PageProcessor();
        pp.setProcessType(PageProcessor.PP_CHAR);
        pp.setRulingLines(rulingLines);
        pp.setProcessSpaces(processSpaces);
        // no iterations should be automatically set to -1
        
        System.out.println("startPage: " + startPage);
        System.out.println("endPage: " + endPage);
        
        int count = -1;
        /*
        for (Page p : pdfResult)
        {
        	count ++;
        	System.out.println("page " + count);
        	System.out.println("found page with " + p.getItems().size() + " items");
        	ListUtils.printList(p.getItems());
        	System.out.println("===================================================");
        }
        */
        
        // now replace the coordinates
        if (!str)
        {
        	// PDF processing for region evaluation
        	File inputPDFFile = new File(PDFFile);
	        byte[] inputDoc = getBytesFromFile(inputPDFFile);

	        List<Page> pdfResult = ProcessFile.processPDF(inputDoc, pp, startPage, endPage, 
	        	encoding, password, null, false);
	        
	        for (Page p : pdfResult)
	        {
	        	if (p.getRotation() == 90)
    			{
	        		float yAdjust = p.getWidth() - p.getHeight(); // is -ve
	        		for (GenericSegment gs : p.getItems())
	        		{
	        			gs.setY1(gs.getY1() + yAdjust);
	        			gs.setY2(gs.getY2() + yAdjust);
	        		}
    			}
	        }
	        
        	evaluateResultReg(inputResultDocument, inputGTDocument, pdfResult);
        }
        else
        {
        	evaluateResultStr(inputResultDocument, inputGTDocument, debug, pageCheck, normRule);
        }
      
    }
    
    //evaluateResultStr
    //for GT and result
    	// normalize textual content
    	// PROTO-LINK FINDING (for both GT table and result table): 
    	// go through each cell in turn and look at all neighbours (4-way)
    		// (if blank cell, go to next cell in that direction)
    		// if (proto-link does not already exist)
    			// add proto-link with object references to both cells
    	// duplicate GT list and map result to this list, deleting elements as matched
    	// use above list to calculate P & R
    
    //do this across all combinations of tables in document to find best match
    
    static List<Region> getRegions(Document inputDocument, List<Page> pdfResult)
    {
    	inputDocument.getDocumentElement().normalize();
    	
    	List<Region> regions = new ArrayList<Region>();
    	
    	int regionNum = 0;
        // loop through tables
        NodeList tableNodes = inputDocument.getElementsByTagName("table");
    	for (int t = 0; t < tableNodes.getLength(); t ++)
    	{
    		Element tableElement = (Element)tableNodes.item(t);
    		// loop through regions
    		NodeList regionNodes = tableElement.getElementsByTagName("region");
    		for (int r = 0; r < regionNodes.getLength(); r ++)
    		{
    			Element regionElement = (Element)regionNodes.item(r);
    			
    			// get bounding-box node
        		Element bBoxNode = (Element)regionElement.
        			getElementsByTagName("bounding-box").item(0);
        		
    			// necessary for chaudhury
    			if (bBoxNode.hasAttribute("x1") &&
    				bBoxNode.hasAttribute("x2") &&
    				bBoxNode.hasAttribute("y1") &&
    				bBoxNode.hasAttribute("y2"))
    			{
	    			Region thisRegion = new Region();
	    			regionNum ++;
	    			thisRegion.num = regionNum;
	        		
	        		/*
	    			thisRegion.setX1(Integer.parseInt(bBoxNode.getAttribute("x1")));
	    			thisRegion.setX2(Integer.parseInt(bBoxNode.getAttribute("x2")));
	    			thisRegion.setY1(Integer.parseInt(bBoxNode.getAttribute("y1")));
	    			thisRegion.setY2(Integer.parseInt(bBoxNode.getAttribute("y2")));
	    			*/
	        		
	    			// necessary for chaudhury!
	    			thisRegion.setX1(Float.parseFloat(bBoxNode.getAttribute("x1")));
	    			thisRegion.setX2(Float.parseFloat(bBoxNode.getAttribute("x2")));
	    			thisRegion.setY1(Float.parseFloat(bBoxNode.getAttribute("y1")));
	    			thisRegion.setY2(Float.parseFloat(bBoxNode.getAttribute("y2")));
	    			
	//    			uncommented for Fang!
	    			thisRegion.correctNegativeDimensions();
	    			
	    			int pageNo = Integer.parseInt(regionElement.getAttribute("page"));
	    			Page pdfPage = pdfResult.get(pageNo - 1); // pageNo 1-based!
	    			thisRegion.page = pageNo;
	//    			pdfPage.findBoundingBox();
	//    			System.out.println("page " + pageNo + ": " + pdfPage);
	    			
	    			List<TextBlock> textBlockPageItems = new ArrayList<TextBlock>();
	    			for (GenericSegment gs : pdfPage.getItems())
	    				if (gs.getClass() == TextBlock.class)
	    					textBlockPageItems.add((TextBlock)gs);
	    			
	    			// add all characters with intersecting centres
	    			thisRegion.getItems().addAll(ListUtils.
	    				findElementsWithCentresWithinBBox(textBlockPageItems, thisRegion));
	    			
	    			if (!SegmentUtils.intersects(thisRegion, pdfPage))
	    				System.err.println("WARN: Region " + regionNum + " does not intersect page");
	    			if (thisRegion.getItems().size() == 0)
	    				System.err.println("WARN: Region " + regionNum + " has no items");
	    			
	    			if (thisRegion.getY2() >= thisRegion.getY1() && 
	    				thisRegion.getX2() >= thisRegion.getX1() &&
	//    				thisRegion.getItems().size() > 0)
	    				SegmentUtils.intersects(thisRegion, pdfPage))
	    				regions.add(thisRegion);
    			}
    		}
    	}
    	
    	return regions;
    }
    
    static void evaluateResultReg(Document inputResultDocument, 
    	Document inputGTDocument, List<Page> pdfResult)
    {
    	List<Region> resultRegions = getRegions(inputResultDocument, pdfResult);
    	List<Region> GTRegions = getRegions(inputGTDocument, pdfResult);
    	
    	System.out.println("GT Regions: " + GTRegions.size());
    	System.out.println("Result Regions: " + resultRegions.size());
    	
    	List<Region> dupResultRegions = new ArrayList<Region>();
    	for (Region r : resultRegions)
    		dupResultRegions.add(r);
    	
    	// determine region correspondence for each page
    	HashMap<Region, Region> regionCorrespondenceMap =
    		new HashMap<Region, Region>();
    	
    	for (Region thisGTRegion : GTRegions)
    	{
    		// get all "unused" result regions on page
    		List<Region> resultRegionsOnPage = new ArrayList<Region>();
    		for (Region thisResultRegion : dupResultRegions)
    			if (thisResultRegion.getPage() == thisGTRegion.getPage())
    				resultRegionsOnPage.add(thisResultRegion);
    		
    		// find the one with greatest number of intersecting characters
    		List<Region> greatestIntersecting = new ArrayList<Region>();
    		int greatestNumIntersectingElems = 0;
    		for (Region testRegion : resultRegionsOnPage)
    		{
    			List<GenericSegment> intersectingElems =
    				ListUtils.intersection(thisGTRegion.getItems(), testRegion.getItems());
    			
    			if (intersectingElems.size() > greatestNumIntersectingElems)
    			{
    				greatestIntersecting.clear();
    				greatestIntersecting.add(testRegion);
    				greatestNumIntersectingElems = intersectingElems.size();
    			}
    			else if (intersectingElems.size() == greatestNumIntersectingElems &&
    				greatestNumIntersectingElems > 0)
    			{
    				greatestIntersecting.add(testRegion); // now 2 or more greatest
    			}
    		}
    		
    		if (greatestIntersecting.size() == 0)
    		{
    			// no correspondence found
    		}
    		else if (greatestIntersecting.size() == 1)
    		{
    			// correspondence easy
    			regionCorrespondenceMap.put(thisGTRegion, greatestIntersecting.get(0));
    			dupResultRegions.remove(greatestIntersecting.get(0));
    		}
    		else if (greatestIntersecting.size() > 1)
    		{
    			System.err.println("warning: correspondence found with > 1 region");
    			
    			// find correspondence with fewest FPs, i.e. smallest region
    			
    			// find smallest region size
    			int smallestRegionSize = Integer.MAX_VALUE;
    			
    			for (Region testRegion : greatestIntersecting)
    				if (testRegion.getItems().size() < smallestRegionSize)
    					smallestRegionSize = testRegion.getItems().size();
    			
    			// generate list of smallest maximal regions
    			List<Region> smallestMaximalRegions = new ArrayList<Region>();
    			for (Region testRegion : greatestIntersecting)
    				if (testRegion.getItems().size() == smallestRegionSize)
    					smallestMaximalRegions.add(testRegion);
    			
    			// print error message if > 1 smallest maximal region
    			if (smallestMaximalRegions.size() > 1)
    				System.err.println("ambiguity: > 1 smallest maximal region! ");
    			
    			// use the first in the list as a correspondence
    			regionCorrespondenceMap.put(thisGTRegion, smallestMaximalRegions.get(0));
    			dupResultRegions.remove(greatestIntersecting.get(0));
    		}
    	}
    	
    	// determine which regions are pure and complete
    	int regionNum = 0; // 1-based counting
    	int detectedRegions = 0;
    	int completeRegions = 0;
    	int pureRegions = 0;
    	
    	/*
    	List<Region> fpRegions = new ArrayList<Region>();
    	for (Region r : resultRegions)
    		fpRegions.add(r); // not sure if duplication really necessary
    	*/
    	
    	for (Region thisGTRegion : GTRegions)
    	{
    		regionNum ++;
    		Region resultRegion = regionCorrespondenceMap.get(thisGTRegion);
//    		fpRegions.remove(resultRegion);
    		
    		boolean complete = false; // default if no region found
    		String purityResult = "N/A";
    		
    		System.out.print("Region: " + regionNum);
    		
//    		if (resultRegion != null)
//    			System.out.print("  result reg: " + resultRegion.num);	
    			
    		System.out.print("  GT Items: " + 
    			thisGTRegion.getItems().size());
    		
    		if (resultRegion != null)
    		{
    			complete = true;
        		// check for completeness
        		for (GenericSegment gs : thisGTRegion.getItems())
        			if (!resultRegion.getItems().contains(gs))
        				complete = false;
        		
    			boolean pure = true; // only counted if result present
    			// check for purity
    			int correctItems = 0;
    			for (GenericSegment gs : resultRegion.getItems())
    			{
        			if (!thisGTRegion.getItems().contains(gs))
        				pure = false;
        			else correctItems ++;
    			}
    			
    			System.out.print("  correct items: " + correctItems + 
    					"  result items: " + resultRegion.getItems().size());

    			System.out.print("  result reg: " + resultRegion.num);
    			
    			detectedRegions ++;
    			if (pure) pureRegions ++;
    			purityResult = Boolean.toString(pure);
    		}
    		if (complete) completeRegions ++;
    		
    		System.out.println("  complete: " + complete +
    			"  pure: " + purityResult);
    	}
    	
//    	System.out.println("FP regions: " + fpRegions.size());
    	
    	if (dupResultRegions.size() > 0)
    		System.out.println("--------------- FALSE POSITIVE REGIONS FOUND! --------------");
    		
    	System.out.println("FP regions: " + dupResultRegions.size());
//    	regionNum = 0;
    	int fpItems = 0;
    	for (Region r : dupResultRegions)
    	{
//    		regionNum ++;
    		System.out.print("FP Region: " + r.num + "  items: " + r.getItems().size() + "  ");
    		fpItems += r.getItems().size();
    	}
    	
    	if (dupResultRegions.size() > 0)
    		System.out.println("FP items: " + fpItems);
    	
    	System.out.print("Overall Result for Document:  Completeness: " + completeRegions + " / " + GTRegions.size() +
    		" = " + (completeRegions/GTRegions.size()));
    	if (detectedRegions > 0)
    		System.out.println("  Purity: " + pureRegions + " / " + detectedRegions +
        		" = " + (pureRegions/detectedRegions));
    	else
    		System.out.println("  Purity = " + pureRegions + " / " + detectedRegions + " = N/A");
    }
    
    static int compareARs(List<AdjacencyRelation> GTAR, List<AdjacencyRelation> resultAR, boolean debug, int normRule)
    {
    	// return number of correct detections
    	int retVal = 0;
    	
    	// duplicate both input lists
    	List<AdjacencyRelation> dupGTAR = new ArrayList<AdjacencyRelation>();
    	for (AdjacencyRelation ar : GTAR)
    		dupGTAR.add(ar);
    	List<AdjacencyRelation> dupResultAR = new ArrayList<AdjacencyRelation>();
    	List<AdjacencyRelation> dup2ResultAR = new ArrayList<AdjacencyRelation>();
    	for (AdjacencyRelation ar : resultAR)
    		dupResultAR.add(ar);
    	
    	// iterate through GT adjacency relations
    	for (int i = 0; i < dupGTAR.size(); i ++)
    	{
    		AdjacencyRelation arGT = dupGTAR.get(i);
    		
    		for (int j = 0; j < dupResultAR.size(); j ++)
    		{
    			AdjacencyRelation arResult = dupResultAR.get(j);
    			if (arGT.isEqual(arResult, normRule))
    			{
    				retVal ++;
    				dupGTAR.remove(i);
    				dupResultAR.remove(j);
    				i --; // will be incremented the next iteration
    				break; // out of j loop
    			}
    		}
    	}
    	
    	if (debug)
    	{
    		// if dupGTAR still contains elements, these have not been detected
    		System.out.println("not detected:");
    		ListUtils.printList(dupGTAR);
    		
    		// TODO: does not work!
    		
    		// if dupResultAR still contains elements, these are false positives
    		System.out.println("false positive:");
    		ListUtils.printList(dupResultAR);
    	}
    	
    	return retVal;
    }
    
    static void evaluateResultStr(Document inputResultDocument, 
        Document inputGTDocument, boolean debug, boolean pageCheck, int normRule)
    {
        inputResultDocument.getDocumentElement().normalize();
        
        List<Table> gtTables = new ArrayList<Table>();
        List<Table> resultTables = new ArrayList<Table>();
        List<Table> remainingTables = new ArrayList<Table>();
        
        // loop through tables
        NodeList tableNodes = inputResultDocument.getElementsByTagName("table");
    	for (int t = 0; t < tableNodes.getLength(); t ++)
    	{
    		Element tableElement = (Element)tableNodes.item(t);
    		// create Table object
    		Table tab = new Table(tableElement, normRule);
    		// obtain list of adj relations
//    		resultARs.add(tab.findAdjacencyRelations());
    		resultTables.add(tab);
    	}
    	
    	// do the same for GT
    	tableNodes = inputGTDocument.getElementsByTagName("table");
    	for (int t = 0; t < tableNodes.getLength(); t ++)
    	{
    		Element tableElement = (Element)tableNodes.item(t);
    		// create Table object
    		Table tab = new Table(tableElement, normRule);
    		// obtain list of adj relations
//    		GTARs.add(tab.findAdjacencyRelations());
    		gtTables.add(tab);
    	}
    	
    	// duplicate result table list
    	for (Table t : resultTables)
    		remainingTables.add(t);
    	
    	int index = -1;
    	for (Table gtTable : gtTables)
    	{
    		index ++;
    		
    		// obtain list of tables on same page
    		List<Table> resultsOnPage = new ArrayList<Table>();
    		for (Table resultTable : remainingTables)
    			if (!pageCheck || resultTable.pageNo == gtTable.pageNo)
//    			if (true) // ana!
    				resultsOnPage.add(resultTable);
    		
    		// find the adjacency relations
    		List<AdjacencyRelation> gtAR = gtTable.findAdjacencyRelations();
    		
    		List<List<AdjacencyRelation>> resultARs = new ArrayList<List<AdjacencyRelation>>();
    		HashMap<List<AdjacencyRelation>, Table> tableHash = new HashMap<List<AdjacencyRelation>, Table>();
    		for (Table resultTable : resultsOnPage)
    		{
    			List<AdjacencyRelation> resultAR = resultTable.findAdjacencyRelations();
    			resultARs.add(resultAR);
    			tableHash.put(resultAR, resultTable);
    		}
    		
    		// create new aligned result list
//    		List<List<AdjacencyRelation>> alignedResultARs =
//    			new ArrayList<List<AdjacencyRelation>>();
    		
    		// find best matching result table
    		List<AdjacencyRelation> matchingResult = null;
    		
    		int highestCorr = -1; int numHighest = 0;
    		for (List<AdjacencyRelation> resultAR : resultARs)
    		{
    			int corrDec = compareARs(gtAR, resultAR, false, normRule);
    			if (corrDec > highestCorr)
    			{
    				highestCorr = corrDec;
    				numHighest = 1;
    				matchingResult = resultAR;
    			}
    			else if (corrDec == highestCorr)
    			{
    				numHighest ++;
    			}
    		}
    		
    		if (matchingResult != null)
    		{
    			// remove this table from result table list & add to aligned result list
//    			alignedResultARs.add(matchingResult);
    			resultARs.remove(matchingResult);
//    			System.out.println("matchingResult: " + tableHash.get(matchingResult));
    			remainingTables.remove(tableHash.get(matchingResult));
    		}
    		else
    		{
//    			alignedResultARs.add(null);
    		}
    		
    		// output result
        	System.out.print("Table " + (index + 1) + ": ");
    		
    		if (debug)
    		{
    			System.out.println();
    			System.out.println("GT ARs: ");
    			ListUtils.printList(gtAR);
    			System.out.println("result ARs: ");
    			ListUtils.printList(matchingResult);
    		}
    		
    		if (matchingResult != null)
    		{
    			int corrDet = compareARs(gtAR, matchingResult, debug, normRule);
    			System.out.print(" GT size: " + gtAR.size() + " corrDet: " + corrDet + " detected: " + matchingResult.size());
    			float prec = (float)corrDet / matchingResult.size();
    			System.out.print("  Precision: " + corrDet + " / " +
        			matchingResult.size() + " = " + prec);
    			float rec = (float)corrDet / gtAR.size();
    			System.out.println("  Recall: " + corrDet + " / " +
            			gtAR.size() + " = " + rec);
    		}
    		else
    		{
    			System.out.println("no matching result found");
    		}
    	}
    	if (remainingTables.size() > 0)
    		System.out.println("!!!!!!!!!! " + remainingTables.size() + " FALSE POSITIVE TABLES FOUND !!!!!!!!!!!!");
    	for (Table t : remainingTables)
    	{
    		System.out.println("FP table with " + t.findAdjacencyRelations().size() + " adjacency relations");
    	}
    }

	//  Returns the contents of the file in a byte array.
    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
    
        // Get the size of the file
        long length = file.length();
    
        // You cannot create an array using a long type.
        // It needs to be an int type.
        // Before converting to an int type, check
        // to ensure that file is not larger than Integer.MAX_VALUE.
        if (length > Integer.MAX_VALUE) {
            // File is too large
        }
    
        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];
    
        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
               && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }
    
        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "+file.getName());
        }
    
        // Close the input stream and return bytes
        is.close();
        return bytes;
    }
    
 // try/catch moved to calling method 9.04.06
    protected static org.w3c.dom.Document setUpXML(String nodeName) 
    {
        try
        {
            DocumentBuilderFactory myFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder myDocBuilder = myFactory.newDocumentBuilder();
            DOMImplementation myDOMImpl = myDocBuilder.getDOMImplementation();
            // resultDocument = myDOMImpl.createDocument("at.ac.tuwien.dbai.pdfwrap", "PDFResult", null);
            org.w3c.dom.Document resultDocument = 
                myDOMImpl.createDocument("at.ac.tuwien.dbai.pdfwrap", nodeName, null);
            return resultDocument;
        }
        catch (ParserConfigurationException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
	/**
	 * This will print the usage requirements and exit.
	 */
	private static void usage()
	{
		// TODO: update!
		
	    System.err.println( "Usage: java at.ac.tuwien.dbai.pdfwrap.MeasureRecognitionPerformance [-reg | -str] [additional options] <inputPrefix> [resultFile] [PDFFile]\n" +
	    	"  -reg                         process table region detection result\n" +
	    	"  -str                         process table structure recognition result\n" +
	        "  -nopage\n" +
	        "  -ignoreChars\n" +
	        "  -ignoreChars2\n" +
	        "  -strIgnore\n" +
	        "  -debug\n" +
	        "  -compare\n" +
	    	"  -encoding  <output encoding> (ISO-8859-1,UTF-16BE,UTF-16LE,...)\n" +
	    	"  -password  <password>        Password to decrypt document\n" +
	        "  -startPage <number>          The first page to start extraction(1 based)\n" +
	        "  -endPage <number>            The last page to extract(inclusive)\n" +
	        "  <input prefix>               The prefix of the ground truth file, e.g. eu-001. Suffix -reg or -str is added automatically\n" +
	        "  [resultFile]                 Name or result file, if different from prefix-[reg|str]-result.xml\n" +
	        "  [PDFFile]                    Name of PDF file, if different from prefix.pdf\n"
	        );
	    System.exit( 1 );
	}
}