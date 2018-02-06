// ICDAR 2013 Table Competition
// Author: Tamir Hassan
// Published under the Apache License Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package at.ac.tuwien.dbai.pdfwrap;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Table
{
	ArrayList<List<String>> cellMatrix;
	int pageNo;
	int normRule;
	
	public Table()
	{
		cellMatrix = new ArrayList<List<String>>();
	}
	
	// TODO: look at col and row increments!
	// TODO: complain if cell gets reassigned!
	public Table(Element tableElement, int normRule)
	{
		this.normRule = normRule;
		cellMatrix = new ArrayList<List<String>>();
		
		// loop through regions
		NodeList regionNodes = tableElement.getElementsByTagName("region");
		for (int r = 0; r < regionNodes.getLength(); r ++)
		{
			Element regionElement = (Element)regionNodes.item(r);
			pageNo = Integer.parseInt(regionElement.getAttribute("page"));
			// loop through cells
			// TODO: look at col and row increments!
			int colIncrement = 0;
			if (regionElement.hasAttribute("col-increment"))
				colIncrement = Integer.parseInt(regionElement.getAttribute("col-increment"));
			int rowIncrement = 0;
			if (regionElement.hasAttribute("row-increment"))
				rowIncrement = Integer.parseInt(regionElement.getAttribute("row-increment"));
			
	    	NodeList cellNodes = regionElement.getElementsByTagName("cell");
			for (int c = 0; c < cellNodes.getLength(); c ++)
			{
				Element cellElement = (Element)cellNodes.item(c);
//				System.out.println("c: " + c + " adding cell: " + cellElement.getTextContent());
				if (cellElement.getTextContent() != null)
//					if (cellElement.getTextContent().replaceAll("\\r|\\n|\\s", "").length() > 0)
					if (AdjacencyRelation.normalize(cellElement.getTextContent(), normRule).length() > 0)
						addCell(cellElement, colIncrement, rowIncrement);
			}
		}
	}
	
	public int numRows()
	{
		return cellMatrix.size();
	}
	
	public int numCols()
	{
		if (cellMatrix.size() >= 1)
			return cellMatrix.get(0).size();
		else return 0;
	}
	
	public void growTable(int newWidth, int newHeight)
	{
		// avoid adding a smaller row than current size!
		if (newWidth < numCols()) newWidth = numCols();
		if (newHeight < numRows()) newHeight = numRows();
		
		for (int r = numRows(); r < newHeight; r ++)
		{
			// add a new blank row with current width
			List<String> newRow = new ArrayList<String>();
			for (int i = 0; i < numCols(); i ++)
				newRow.add(null);
			
			cellMatrix.add(newRow);
		}
		
		for (int c = numCols(); c < newWidth; c ++)
		{
			// add a new blank cell to each row
			for (List<String> thisRow: cellMatrix)
				thisRow.add(null);
		}
	}
	
	// row and column numbers are zero-based
	public void setCell(int col, int row, String s)
	{
		growTable(col + 1, row + 1); // because 0-based indexing
		
		// replace blank (or content) cell with tc
		List<String> thisRow = cellMatrix.get(row);
		thisRow.set(col, s);
		
		// add to item list
//			items.add(s);
	}
	
	public void addCell(Element cellElement, int colIncrement, int rowIncrement)
	{
		int endCol, endRow;
//		System.out.println("id: " + cellElement.getAttribute("id") + cellElement.getNodeName() + cellElement.getTextContent());
		int startCol = Integer.parseInt(cellElement.getAttribute("start-col"));
		if (cellElement.hasAttribute("end-col"))
			endCol = Integer.parseInt(cellElement.getAttribute("end-col"));
		else
			endCol = startCol;
		int startRow = Integer.parseInt(cellElement.getAttribute("start-row"));
		if (cellElement.hasAttribute("end-row"))
			endRow = Integer.parseInt(cellElement.getAttribute("end-row"));
		else
			endRow = startRow;
		
		startCol += colIncrement; endCol += colIncrement;
		startRow += rowIncrement; endRow += rowIncrement;
		
		//TODO: strip textObj (or later?)
		String textObj = cellElement.getTextContent();
//		String to = textObj.replaceAll("\\r|\\n|\\s", "");
		String to = AdjacencyRelation.normalize(textObj, normRule);
		
		if (startRow > endRow || startCol > endCol)
			System.err.println
				("Start and end col/row the wrong way round!\n" + to);
		
//		System.out.println("adding " + to);
		
		for (int r = startRow; r <= endRow; r ++)
			for (int c = startCol; c <= endCol; c ++)
			{
				try
				{
//					System.out.println("setting c: " + c + " r: " + r + " with: " + to);
					String existingCell = getCell(c, r);
					if (existingCell != null)
					{
//						String ec = existingCell.replaceAll("\\r|\\n|\\s", "");
						String ec = AdjacencyRelation.normalize(existingCell, normRule);
						System.out.print("setting c: " + c + " r: " + r + " with: " + to);
						System.err.println
							("  Cell position already assigned with: " + ec + "  Check GT file!");
					}
				}
				catch(IndexOutOfBoundsException e)
				{
					// do nothing
				}
				setCell(c, r, textObj);
			}
	}
	
	public String getCell(int col, int row)
	{
//		System.out.println("in getCell with col: " + col + " numCols: " + numCols() + " row: " + row + " numRows: " + numRows());
		List<String> thisRow = cellMatrix.get(row);
		return thisRow.get(col);
	}
		
	public List<AdjacencyRelation> findAdjacencyRelations()
	{
//		System.out.println("=============================================================");
		List<AdjacencyRelation> retVal = new ArrayList<AdjacencyRelation>();

		// look right
		for (int r = 0; r < numRows(); r ++)
		{
			for (int cFrom = 0; cFrom < numCols() - 1; cFrom ++)
			{
				int cTo = cFrom + 1;
				boolean loop = true;
				while(loop && cTo < numCols())
				{
					if (getCell(cFrom, r) != null && getCell(cTo, r) != null &&
						getCell(cFrom, r) != getCell(cTo, r)) // is not the same String object
					{
//						System.out.println("adding horiz: " + getCell(cFrom, r).replaceAll("\\r|\\n|\\s", "") + " -> " + getCell(cTo, r).replaceAll("\\r|\\n|\\s", ""));
						retVal.add(new AdjacencyRelation
							(getCell(cFrom, r), getCell(cTo, r), AdjacencyRelation.DIR_HORIZ, (cTo - cFrom) - 1));
						loop = false;
					}
					else
					{
						if (getCell(cFrom, r) != null && getCell(cTo, r) != null &&
							getCell(cFrom, r) == getCell(cTo, r))
							cFrom = cTo; // advance from so that noBlanks calculation is correct
						cTo ++; // continue looping
					}
				}
			}
		}
		
		// look down
		for (int c = 0; c < numCols(); c ++)
		{
			for (int rFrom = 0; rFrom < numRows() - 1; rFrom ++)
			{
				int rTo = rFrom + 1;
				boolean loop = true;
				while(loop && rTo < numRows())
				{
					if (getCell(c, rFrom) != null && getCell(c, rTo) != null &&
						getCell(c, rFrom) != getCell(c, rTo)) // is not the same String object
					{
//						System.out.println("adding vert: " + getCell(c, rFrom).replaceAll("\\r|\\n|\\s", "") + " -> " + getCell(c, rTo).replaceAll("\\r|\\n|\\s", ""));
						retVal.add(new AdjacencyRelation
							(getCell(c, rFrom), getCell(c, rTo), AdjacencyRelation.DIR_VERT, (rTo - rFrom) - 1));
						loop = false;
					}
					else
					{
						if(getCell(c, rFrom) != null && getCell(c, rTo) != null &&
							getCell(c, rFrom) == getCell(c, rTo))
							rFrom = rTo; // advance from so that noBlanks calculation is correct
						rTo ++; // continue looping
					}
				}
			}
		}
		
		// remove duplicates (can be caused by parallel links between spanning cells)
		//(don't use equals method! must be same String object)
		boolean repeat = true;
		while(repeat)
		{
			repeat = false;
			List<AdjacencyRelation> duplicates = new ArrayList<AdjacencyRelation>();
			
			outerloop: 
				for (AdjacencyRelation ar1 : retVal)
			{
				for (AdjacencyRelation ar2 : retVal)
				{
					if (ar1 != ar2)
					{
						if (ar1.direction == ar2.direction &&
							ar1.fromText == ar2.fromText &&
							ar1.toText == ar2.toText)
						{
							duplicates.add(ar2);
							break outerloop;
						}
					}
				}
			}
			if (duplicates.size() > 0) // only one item is removed per iteration
			{
				repeat = true;
				retVal.removeAll(duplicates);
			}
		}
		
		return retVal;
	}

	// adapted from OrderedTable
	public void addAsXHTML(Document resultDocument, Element parent, int no)//, GenericSegment pageDim)
    {
		Element newPageElement = resultDocument.createElement("h2");
        
        newPageElement.appendChild
            (resultDocument.createTextNode("Table " + no));
        
        parent.appendChild(newPageElement);
		
        Element newTableElement = resultDocument.createElement("table");
        
        for (List<String> thisRow : cellMatrix)
        {
            Element newRowElement = resultDocument.createElement("tr");
            
            for (String thisCell : thisRow)
            {
            	if (thisCell != null)
            	{
	                Element newColumnElement = resultDocument.createElement("td");
	                
	                //System.out.println("in colIter with: " + thisCell);
	                /*
	                if (thisCell.getColSpan() > 1) {
	                	newColumnElement.setAttribute
	                    ("colspan", Integer.toString(thisCell.getColSpan()));
	                }
	                
	                if (thisCell.getRowSpan() > 1) {
	                	newColumnElement.setAttribute
	                    ("rowspan", Integer.toString(thisCell.getRowSpan()));
	                }
	                */
	                // this bit added 22.11.06
	                // to replace every occurrence of "\n" in the string
	                // with a <br/> tag.
	                
	                // TODO: refactor and move to e.g. Utils method
	                // so that it can be used for other segments/elements
	                // containing text.
	                
	                // remove newlines from text
	                String theText = AdjacencyRelation.normalize(thisCell, normRule);
	                
	                /*
	                if (thisCell instanceof PABlankCell)
	                	theText = "<blank>";
	                else if (thisCell == null)
	                	theText = "<null>";
	                */
	                
	                // the following lines would just add the string
	                // without <br/>s
	                //newColumnElement.appendChild
	    			//(resultDocument.createTextNode(theText));
	                String textSection = new String();// + thisCell.getColSpan() +
//	                	" " + thisCell.getRowSpan() + " ";
	                
	                for (int n = 0; n < theText.length(); n ++)
	                {
	                	String thisChar = theText.substring(n, n + 1);
	                	if (thisChar.equals("\n"))
	                	{
	                		newColumnElement.appendChild
	                			(resultDocument.createTextNode(textSection));
	                        newColumnElement.appendChild
	                        	(resultDocument.createElement("br"));
	                        textSection = "";
	                	}
	                	else
	                	{
	                		textSection = textSection.concat(thisChar);
	                	}
	                }
	                
	                if (textSection.length() > 0)
	                	newColumnElement.appendChild
	        				(resultDocument.createTextNode(textSection));
	                else
	                	newColumnElement.appendChild
        					(resultDocument.createTextNode(" "));
	                
	                newRowElement.appendChild(newColumnElement);
            	}
            	else
            	{
            		// add blank cell
            		Element newColumnElement = resultDocument.createElement("td");
            		newColumnElement.appendChild
						(resultDocument.createTextNode(" "));
            
            		newRowElement.appendChild(newColumnElement);
            	}
            }
            newTableElement.appendChild(newRowElement);
        }
        
        parent.appendChild(newTableElement);
    }
	
	/*
	public int getNumChars() {
		return numChars;
	}

	public void setNumChars(int numChars) {
		this.numChars = numChars;
	}
	*/
	
}