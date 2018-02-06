// ICDAR 2013 Table Competition
// Author: Tamir Hassan
// Published under the Apache License Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package at.ac.tuwien.dbai.pdfwrap;

import at.ac.tuwien.dbai.pdfwrap.model.document.CompositeSegment;
import at.ac.tuwien.dbai.pdfwrap.model.document.GenericSegment;

public class Region extends CompositeSegment<GenericSegment>
{
	int page = -1;
//	int numChars = -1;
	int num = -1;

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
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