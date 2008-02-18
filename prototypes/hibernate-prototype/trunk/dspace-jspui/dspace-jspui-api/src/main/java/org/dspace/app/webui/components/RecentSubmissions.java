/*
 * RecentSubmissions.java
 *
 * Version: $Revision:  $
 *
 * Date: $Date:  $
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.webui.components;

import org.dspace.content.Item;


/**
 * Basic class for representing the set of items which are recent submissions
 * to the archive
 * 
 * @author Richard Jones
 *
 */
public class RecentSubmissions
{
	/** The set of items being represented */
	private Item[] items;
	
	/**
	 * Construct a new RecentSubmissions object to represent the passed
	 * array of items
	 * 
	 * @param items
	 */
	public RecentSubmissions(Item[] items)
	{
		this.items = items;
	}

	/**
	 * obtain the number of recent submissions available
	 * 
	 * @return	the number of items
	 */
	public int count()
	{
		return items.length;
	}
	
	/**
	 * Obtain the array of items
	 * 
	 * @return	an array of items
	 */
	public Item[] getRecentSubmissions()
	{
		return items;
	}
	
	/**
	 * Get the item which is in the i'th position.  Therefore i = 1 gets the
	 * most recently submitted item, while i = 3 gets the 3rd most recently
	 * submitted item
	 * 
	 * @param i		the position of the item to retrieve
	 * @return		the Item
	 */
	public Item getRecentSubmission(int i)
	{
		if (i < items.length)
		{
			return items[i];
		}
		else
		{
			return null;
		}
	}
}
