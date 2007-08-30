package org.dspace.sword;

import org.dspace.content.Item;

public class DepositResult
{
	private Item item;

	private String verboseDescription;
	
	/**
	 * @return the item
	 */
	public Item getItem()
	{
		return item;
	}

	
	/**
	 * @param item the item to set
	 */
	public void setItem(Item item)
	{
		this.item = item;
	}


	
	/**
	 * @return the verboseDescription
	 */
	public String getVerboseDescription()
	{
		return verboseDescription;
	}


	
	/**
	 * @param verboseDescription the verboseDescription to set
	 */
	public void setVerboseDescription(String verboseDescription)
	{
		this.verboseDescription = verboseDescription;
	}
	
	
	
}
