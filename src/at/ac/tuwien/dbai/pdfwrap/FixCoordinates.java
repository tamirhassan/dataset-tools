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
import java.util.List;

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

public class FixCoordinates
{

	public static final String REG = "-reg";
	public static final String STR = "-str";
	
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
        String inXML = null;
        String inPDF = null;
        String outFile = null;
        boolean rulingLines = true;
        boolean toXHTML = true;
        boolean borders = true;
        boolean processSpaces = false;
        int currentArgumentIndex = 0;
        String password = "";
        String encoding = DEFAULT_ENCODING;
        PDFObjectExtractor extractor = new PDFObjectExtractor();
        int startPage = 1;
        int endPage = Integer.MAX_VALUE;
        boolean toConsole = false;
        boolean str = false;
        
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
                if( inXML == null )
                {
                    inXML = args[i];
                }
                else if( inPDF == null)
                {
                	inPDF = args[i];
                }
                else if ( outFile == null )
                {
                    outFile = args[i];
                }
            }
        }

        if( inXML == null || inPDF == null)
        {
            usage();
        }

        // TODO: add an option to generate PDF name based on infile name (suffixes)
        // TODO: function to rename old file; new file has name of old?
        
        if( outFile == null && inXML.length() >4 )
        {
            outFile = inXML.substring( 0, inXML.length() -4 ) + ".corr.xml";
        }
        
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
        File inputXMLFile = new File(inXML);
        File inputPDFFile = new File(inPDF);
        
        // parse XML of the input XML file
        
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document inputDocument = docBuilder.parse(inputXMLFile);
        
        Document resultDocument = null;
        byte[] inputDoc = getBytesFromFile(inputPDFFile);
        
        // set up page processor object
        PageProcessor pp = new PageProcessor();
        pp.setProcessType(PageProcessor.PP_CHAR);
        pp.setRulingLines(rulingLines);
        pp.setProcessSpaces(processSpaces);
        // no iterations should be automatically set to -1
        
        System.out.println("startPage: " + startPage);
        System.out.println("endPage: " + endPage);
        
        // do the processing
        List<Page> pdfResult = ProcessFile.processPDF(inputDoc, pp, startPage, endPage, 
        		encoding, password, null, false);
    	
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
        	replaceCoordinatesReg(inputDocument, pdfResult);
        else
        	replaceCoordinatesStr(inputDocument, pdfResult);
        
        // now output the XML Document by serializing it to output
        Writer output = null;
        if( toConsole )
        {
            output = new OutputStreamWriter( System.out );
        }
        else
        {
            if( encoding != null )
            {
                output = new OutputStreamWriter(
                    new FileOutputStream( outFile ), encoding );
            }
            else
            {
                //use default encoding
                output = new OutputStreamWriter(
                    new FileOutputStream( outFile ) );
            }
            //System.out.println("using out put file: " + outFile);
        }
        
        ProcessFile.serializeXML(inputDocument, output);
        
        //System.out.println("resultDocument: " + resultDocument);
        
//        temporarily disabled
//        serializeXML(resultDocument, output);
        
        if( output != null )
        {
            output.close();
        }
    }
    
    static void replaceCoordinatesReg(Document inputDocument, List<Page> pdfResult)
    {
//    	Document resultDocument = setUpXML("todo-rename");
    	
		// load the wrapper
        // normalize text representation
        inputDocument.getDocumentElement().normalize();
        //NodeList listOfWrappers = wrapperDocument.getElementsByTagName("pdf-wrapper");
        
//        Element rootItem = (Element)inputDocument.getElementsByTagName("table").item(0);
//        Element firstPage = (Element)rootItem.getElementsByTagName("region").item(0);
        
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
    			CompositeSegment thisRegion = new CompositeSegment();
    			int pageNo = Integer.parseInt(regionElement.getAttribute("page"));
    			Page pdfPage = pdfResult.get(pageNo - 1); // pageNo 1-based!
    			
    			// loop through instruction nodes
    			NodeList instructionNodes = regionElement.getElementsByTagName("instruction");
        		for (int i = 0; i < instructionNodes.getLength(); i ++)
        		{
        			Element instructionElement = (Element)instructionNodes.item(i);
        			String instrIdText = instructionElement.getAttribute("instr-id"); // 0-based!
        			String subInstrIdText = instructionElement.getAttribute("subinstr-id"); // is always correct
        			
        			int pageInstrId = Integer.parseInt(instrIdText);
        			int instrId = pageInstrId;
        			
        			// find out instrId relative to file (not page!)
        			if (pageNo > 1)
        			{
        				Page prevPage = pdfResult.get(pageNo - 2);
        				instrId = prevPage.getLastOpIndex() + 1 + pageInstrId; // +1 because 0-based (-1 ++)
        				
        				// correct instruction IDs
        				instructionElement.setAttribute("instr-id", Integer.toString(instrId));
        			}
        			
        			if (subInstrIdText == null | subInstrIdText.length() <= 0)
        			{
        				// no sub-instruction; TJ instruction
        				for (GenericSegment gs : pdfPage.getItems())
        				{
        					if (gs instanceof TextBlock)
        					{
	        					TextBlock tb = (TextBlock)gs;
	        					TextLine tl = tb.getItems().get(0); // contains always 1 item
	        					LineFragment lf = tl.getItems().get(0); // contains always 1 item
	        					TextFragment tf = lf.getItems().get(0); // contains always 1 item
	        					
	    						List<OpTuple> sourceOps = tf.sourceOps();
	    						for (OpTuple ot : sourceOps)
	    						{
	//    							System.out.println("1ot: " + ot);
	    							if (ot.getOpIndex() == instrId)
	    								thisRegion.getItems().add(gs); 
	    								// TODO: and exit from loop prematurely
	    						}
        					}
        				}
        			}
        			else
        			{
        				int subInstrId = Integer.parseInt(subInstrIdText);
        				// Tj instruction
        				for (GenericSegment gs : pdfPage.getItems())
        				{
        					if (gs instanceof TextBlock)
        					{
	        					TextBlock tb = (TextBlock)gs;
	        					TextLine tl = tb.getItems().get(0); // contains always 1 item
	        					LineFragment lf = tl.getItems().get(0); // contains always 1 item
	        					TextFragment tf = lf.getItems().get(0); // contains always 1 item
	        					
	    						List<OpTuple> sourceOps = tf.sourceOps();
	    						for (OpTuple ot : sourceOps)
	    						{
	//    							System.out.println("2ot: " + ot);
	    							if (ot.getOpIndex() == instrId &&
	    								ot.getArgIndex() == subInstrId)
	    								thisRegion.getItems().add(gs); 
	    								// TODO: and exit from loop prematurely
	    						}
        					}
        				}
        			}
        		}
    			// get bounding-box node
        		Element bBoxNode = (Element)regionElement.
        			getElementsByTagName("bounding-box").item(0);
        		
        		// get old attributes
        		int oldX1 = Integer.parseInt(bBoxNode.getAttribute("x1"));
        		int oldY1 = Integer.parseInt(bBoxNode.getAttribute("y1"));
        		int oldX2 = Integer.parseInt(bBoxNode.getAttribute("x2"));
        		int oldY2 = Integer.parseInt(bBoxNode.getAttribute("y2"));
        		
//        		System.out.println("old attributes: x1="+oldX1+" y1="+oldY1+" x2="+oldX2+" y2="+oldY2);
        		
        		System.out.println("correcting bounding box");
        		
        		// calculate new attributes
        		thisRegion.findBoundingBox();
//        		System.out.println("new attributes: " + thisRegion);
        		
        		int newX1 = (int)thisRegion.getX1();
        		bBoxNode.setAttribute("x1", Integer.toString(newX1));
        		int newY1 = (int)thisRegion.getY1();
        		bBoxNode.setAttribute("y1", Integer.toString(newY1));
        		int newX2 = (int)thisRegion.getX2();
        		bBoxNode.setAttribute("x2", Integer.toString(newX2));
        		int newY2 = (int)thisRegion.getY2();
        		bBoxNode.setAttribute("y2", Integer.toString(newY2));
        		
        		if (oldX1 != newX1 || oldX2 != newX2)
        		{
        			System.err.println("Warning: X co-ordinates do not match!");
        			System.err.println("bBox with " + thisRegion.getItems().size() + " items");
        			System.err.println("old attributes: x1="+oldX1+" y1="+oldY1+" x2="+oldX2+" y2="+oldY2);
        			System.err.println("new attributes: " + thisRegion);
        		}
    		}
    	}
        
//    	return resultDocument;
    }

    static void replaceCoordinatesStr(Document inputDocument, List<Page> pdfResult)
    {
//    	Document resultDocument = setUpXML("todo-rename");
    	
		// load the wrapper
        // normalize text representation
        inputDocument.getDocumentElement().normalize();
        //NodeList listOfWrappers = wrapperDocument.getElementsByTagName("pdf-wrapper");
        
//        Element rootItem = (Element)inputDocument.getElementsByTagName("table").item(0);
//        Element firstPage = (Element)rootItem.getElementsByTagName("region").item(0);
        
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
    			int pageNo = Integer.parseInt(regionElement.getAttribute("page"));
    			Page pdfPage = pdfResult.get(pageNo - 1); // pageNo 1-based!
    			
    			/*
    			System.out.println("pdfPage: \n");
    			for (GenericSegment gs : pdfPage.getItems())
    				System.out.println(((CompositeSegment)gs).toExtendedString());
    			*/
    			
    			// loop through cell nodes
    			
    			NodeList cellNodes = regionElement.getElementsByTagName("cell");
        		for (int c = 0; c < cellNodes.getLength(); c ++)
        		{
        			Element cellElement = (Element)cellNodes.item(c);
        			CompositeSegment thisCell = new CompositeSegment();
    			
	    			NodeList instructionNodes = cellElement.getElementsByTagName("instruction");
	        		for (int i = 0; i < instructionNodes.getLength(); i ++)
	        		{
	        			Element instructionElement = (Element)instructionNodes.item(i);
	        			String instrIdText = instructionElement.getAttribute("instr-id");
	        			String subInstrIdText = instructionElement.getAttribute("subinstr-id");
	        			
	        			int pageInstrId = Integer.parseInt(instrIdText);
	        			int instrId = pageInstrId;
	        			
	        			// find out instrId relative to file (not page!)
	        			if (pageNo > 1)
	        			{
	        				Page prevPage = pdfResult.get(pageNo - 2);
	        				instrId = prevPage.getLastOpIndex() + 1 + pageInstrId; // +1 because 0-based (-1 ++)
	        				
	        				// correct instruction IDs
	        				instructionElement.setAttribute("instr-id", Integer.toString(instrId));
	        			}
	        			
	        			if (subInstrIdText == null | subInstrIdText.length() <= 0)
	        			{
	        				// no sub-instruction; TJ instruction
	        				for (GenericSegment gs : pdfPage.getItems())
	        				{
	        					if (gs instanceof TextBlock)
	        					{
		        					TextBlock tb = (TextBlock)gs;
		        					TextLine tl = tb.getItems().get(0); // contains always 1 item
		        					LineFragment lf = tl.getItems().get(0); // contains always 1 item
		        					TextFragment tf = lf.getItems().get(0); // contains always 1 item
		        					
		    						List<OpTuple> sourceOps = tf.sourceOps();
		    						for (OpTuple ot : sourceOps)
		    						{
		//    							System.out.println("1ot: " + ot);
		    							if (ot.getOpIndex() == instrId)
		    								thisCell.getItems().add(gs); 
		    								// TODO: and exit from loop prematurely
		    						}
	        					}
	        				}
	        			}
	        			else
	        			{
	        				int subInstrId = Integer.parseInt(subInstrIdText);
	        				// Tj instruction
	        				for (GenericSegment gs : pdfPage.getItems())
	        				{
	        					if (gs instanceof TextBlock)
	        					{
		        					TextBlock tb = (TextBlock)gs;
		        					TextLine tl = tb.getItems().get(0); // contains always 1 item
		        					LineFragment lf = tl.getItems().get(0); // contains always 1 item
		        					TextFragment tf = lf.getItems().get(0); // contains always 1 item
		        					
	//	        					System.out.println("tf.items: " + tf.getItems().size());
	//	        					PP_CHAR -- contains always 1 item; with PP_FRAGMENT several
		        					
		    						List<OpTuple> sourceOps = tf.sourceOps();
		    						for (OpTuple ot : sourceOps)
		    						{
		//    							System.out.println("2ot: " + ot);
		    							if (ot.getOpIndex() == instrId &&
		    								ot.getArgIndex() == subInstrId)
		    								thisCell.getItems().add(gs); 
		    								// TODO: and exit from loop prematurely
		    						}
	        					}
	        				}
	        			}
	        		}
	    			// get bounding-box node
	        		Element bBoxNode = (Element)cellElement.
	        			getElementsByTagName("bounding-box").item(0);
	        		
	        		// get old attributes
	        		int oldX1 = Integer.parseInt(bBoxNode.getAttribute("x1"));
	        		int oldY1 = Integer.parseInt(bBoxNode.getAttribute("y1"));
	        		int oldX2 = Integer.parseInt(bBoxNode.getAttribute("x2"));
	        		int oldY2 = Integer.parseInt(bBoxNode.getAttribute("y2"));
	        		
//	        		System.out.println("old attributes: x1="+oldX1+" y1="+oldY1+" x2="+oldX2+" y2="+oldY2);
	        		
	        		System.out.println("correcting bounding box");
	        		
	        		// calculate new attributes
	        		thisCell.findBoundingBox();
//	        		System.out.println("new attributes: " + thisCell);
	        		
//	        		don't set new X co-ordinates as they are not sub-instruction accurate!
	        		int newX1 = (int)thisCell.getX1();
//	        		bBoxNode.setAttribute("x1", Integer.toString(newX1));
	        		int newY1 = (int)thisCell.getY1();
	        		bBoxNode.setAttribute("y1", Integer.toString(newY1));
	        		int newX2 = (int)thisCell.getX2();
//	        		bBoxNode.setAttribute("x2", Integer.toString(newX2));
	        		int newY2 = (int)thisCell.getY2();
	        		bBoxNode.setAttribute("y2", Integer.toString(newY2));
	        		
	        		/*
	        		if (oldX1 != newX1 || oldX2 != newX2)
	        		{
	        			System.err.println("Warning: X co-ordinates do not match!");
	        			System.err.println("bBox with " + thisCell.getItems().size() + " items");
	        			System.err.println("old attributes: x1="+oldX1+" y1="+oldY1+" x2="+oldX2+" y2="+oldY2);
	        			System.err.println("new attributes: " + thisCell.toExtendedString());
	        		}
	        		*/
	        		if (!Utils.within(newY2 - newY1, oldY2 - oldY1, 2.0f))
	        		{
	        			System.err.println("Warning: new height and old height do not match!");
	        			System.err.println("bBox with " + thisCell.getItems().size() + " items");
	        			System.err.println("old attributes: x1="+oldX1+" y1="+oldY1+" x2="+oldX2+" y2="+oldY2);
	        			System.err.println("new attributes: " + thisCell.toExtendedString());
	        		}
	        		GenericSegment newSeg = new GenericSegment(newX1, newX2, newY1, newY2);
	        		if (!(SegmentUtils.horizIntersect(newSeg, oldX1) &&
	        			SegmentUtils.horizIntersect(newSeg, oldX2)))
	        		{
	        			System.err.println("Warning: new segment does not completely cover old segment's horizontally!");
	        			System.err.println("bBox with " + thisCell.getItems().size() + " items");
	        			System.err.println("old attributes: x1="+oldX1+" y1="+oldY1+" x2="+oldX2+" y2="+oldY2);
	        			System.err.println("new attributes: " + thisCell.toExtendedString());
	        		}
        		}
    		}
    	}
        
//    	return resultDocument;
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
		
	    System.err.println( "Usage: java at.ac.tuwien.dbai.pdfwrap.ProcessFile [OPTIONS] <PDF file> [Text File]\n" +
	        "  -password  <password>        Password to decrypt document\n" +
	        "  -encoding  <output encoding> (ISO-8859-1,UTF-16BE,UTF-16LE,...)\n" +
	        "  -xmillum                     output XMIllum XML (instead of XHTML)\n" +
	        "  -norulinglines               do not process ruling lines\n" +
	        "  -spaces                      split low-level segments according to spaces\n" +
	        "  -console                     Send text to console instead of file\n" +
	        "  -startPage <number>          The first page to start extraction(1 based)\n" +
	        "  -endPage <number>            The last page to extract(inclusive)\n" +
	        "  <PDF file>                   The PDF document to use\n" +
	        "  [Text File]                  The file to write the text to\n"
	        );
	    System.exit( 1 );
	}
}