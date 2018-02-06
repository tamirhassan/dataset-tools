// ICDAR 2013 Table Competition
// Author: Tamir Hassan
// Published under the Apache License Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package at.ac.tuwien.dbai.pdfwrap;

import java.util.Locale;

import at.ac.tuwien.dbai.pdfwrap.model.document.CompositeSegment;
import at.ac.tuwien.dbai.pdfwrap.model.document.GenericSegment;

public class AdjacencyRelation extends CompositeSegment<GenericSegment>
{
	final static int DIR_HORIZ = 1;
	final static int DIR_VERT = 2;
	
	int direction;
	int noBlanks;
	
	String fromText;
	String toText;
	
	public AdjacencyRelation(String fromText, String toText, int direction)
	{
		this.fromText = fromText;
		this.toText = toText;
		this.direction = direction;
		this.noBlanks = 0;
	}
	
	public AdjacencyRelation(String fromText, String toText, int direction, int noBlanks)
	{
		this.fromText = fromText;
		this.toText = toText;
		this.direction = direction;
		this.noBlanks = noBlanks;
	}
	
	public boolean isEqual(AdjacencyRelation test, int normRule)
	{
		// TODO: normalize text!
		/*
		String thisFromText = this.fromText.replaceAll("\\r|\\n|\\s", "");
		String thisToText = this.toText.replaceAll("\\r|\\n|\\s", "");
		
		String testFromText = test.fromText.replaceAll("\\r|\\n|\\s", "");
		String testToText = test.toText.replaceAll("\\r|\\n|\\s", "");
		*/
		
		// write to System.err if no chars left
		if (normalize(this.fromText, normRule).length() == 0 ||
			normalize(this.toText, normRule).length() == 0 ||
			normalize(test.fromText, normRule).length() == 0 ||
			normalize(test.toText, normRule).length() == 0)
		{
			/*
			System.out.println("fromText: " + this.fromText);
			System.out.println("toText: " + this.toText);
			System.out.println("t.fromText: " + test.fromText);
			System.out.println("t.toText: " + test.toText);
			*/
			
			System.err.println("Warning: Text comparison of 0-length strings after normalization");
		}
		
		return(normalize(this.fromText, normRule).equals(normalize(test.fromText, normRule)) &&
			normalize(this.toText, normRule).equals(normalize(test.toText, normRule)) &&
			this.direction == test.direction &&
			this.noBlanks == test.noBlanks);
	}
	
	static String normalize(String s, int normRule)
    {
		if (normRule == 0)
			return s.replaceAll("\\r|\\n|\\s", "").toUpperCase(Locale.US);
		else if (normRule == 1)
			return s.replaceAll("\\r|\\n|\\s", "").replaceAll("\\W", "_").toUpperCase(Locale.US);
		else if (normRule == 2)
			return s.replaceAll("\\r|\\n|\\s", "").replaceAll("\\W", "").toUpperCase(Locale.US);
		else
			return s.replaceAll("\\r|\\n|\\s", "").toUpperCase(Locale.US); // normrule 0 as standard
//    	return s.replaceAll("\\W", "").toUpperCase(Locale.US);
    }
	
	public String toString()
	{
		return "AR" + direction + "  " + noBlanks + "  "+ normalize(this.fromText, 0) + " -> " + normalize(this.toText, 0);
	}
}