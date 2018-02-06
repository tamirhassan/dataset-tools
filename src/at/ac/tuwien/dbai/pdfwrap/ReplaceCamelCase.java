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

public class ReplaceCamelCase
{
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
        String inFile = null;
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
        
        inFile = args[0];
        outFile = inFile.substring( 0, inFile.length() -4 ) + ".new.xml";
        
        // load the input files
        File inputFile = new File(inFile);
        
        // parse XML of the input XML file
        
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document inputDocument = docBuilder.parse(inputFile);
        
//      Document resultDocument = null;
        
        // now replace the coordinates
        replaceCamelCaseStr(inputDocument);
        
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
            //System.out.println("using output file: " + outFile);
        }
        
        ProcessFile.serializeXML(inputDocument, output);
        
        if( output != null )
        {
            output.close();
        }
    }
    
    static void replaceCamelCaseStr(Document inputDocument)
    {
    	// TODO: check whether no of input params correct
    	
//    	Document resultDocument = setUpXML("todo-rename");
    	
		// load the wrapper
        // normalize text representation
        inputDocument.getDocumentElement().normalize();
        //NodeList listOfWrappers = wrapperDocument.getElementsByTagName("pdf-wrapper");
        
//        Element rootItem = (Element)inputDocument.getElementsByTagName("table").item(0);
//        Element firstPage = (Element)rootItem.getElementsByTagName("region").item(0);
        
        int tableNo = -1;
        
        // loop through tables
        NodeList tableNodes = inputDocument.getElementsByTagName("table");
    	for (int t = 0; t < tableNodes.getLength(); t ++)
    	{
    		Element tableElement = (Element)tableNodes.item(t);
    		// loop through regions
    		NodeList regionNodes = tableElement.getElementsByTagName("region");
    		for (int r = 0; r < regionNodes.getLength(); r ++)
    		{
    			tableNo ++;
    			Element regionElement = (Element)regionNodes.item(r);
//    			int pageNo = Integer.parseInt(regionElement.getAttribute("page"));
    			
    			// loop through cell nodes
    			
    			NodeList cellNodes = regionElement.getElementsByTagName("cell");
        		for (int c = 0; c < cellNodes.getLength(); c ++)
        		{
        			Element cellElement = (Element)cellNodes.item(c);
    			
        			int startCol = Integer.parseInt(cellElement.getAttribute("startCol"));
        			int endCol = startCol;
        			if (cellElement.hasAttribute("endCol"))
        				endCol = Integer.parseInt(cellElement.getAttribute("endCol"));
        			int startRow = Integer.parseInt(cellElement.getAttribute("startRow"));
        			int endRow = startRow;
        			if (cellElement.hasAttribute("endRow"))
        				endRow = Integer.parseInt(cellElement.getAttribute("endRow"));
        			
    				cellElement.setAttribute("start-col", Integer.toString(startCol));
    				if (startCol != endCol)
    					cellElement.setAttribute("end-col", Integer.toString(endCol));
    				cellElement.setAttribute("start-row", Integer.toString(startRow));
    				if (startRow != endRow)
    					cellElement.setAttribute("end-row", Integer.toString(endRow));
    				
    				cellElement.removeAttribute("startCol");
    				cellElement.removeAttribute("endCol");
    				cellElement.removeAttribute("startRow");
    				cellElement.removeAttribute("endRow");
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