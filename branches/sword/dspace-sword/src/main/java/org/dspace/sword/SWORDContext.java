package org.dspace.sword;

import java.sql.SQLException;

import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.content.Collection;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import java.util.List;
import java.util.ArrayList;

public class SWORDContext 
{
	private EPerson authenticated = null;
	
	private EPerson onBehalfOf = null;

	public EPerson getAuthenticated() 
	{
		return authenticated;
	}

	public void setAuthenticated(EPerson authenticated) 
	{
		this.authenticated = authenticated;
	}

	public EPerson getOnBehalfOf() 
	{
		return onBehalfOf;
	}

	public void setOnBehalfOf(EPerson onBehalfOf) 
	{
		this.onBehalfOf = onBehalfOf;
	}
	
	/**
	 * Is the given eperson a DSpace administrator?  This translates
	 * as asking the question of whether the given eperson a member
	 * of the special DSpace group Administrator, with id 1
	 * 
	 * @param eperson
	 * @return	true if administrator, false if not
	 * @throws SQLException
	 */
	public boolean isUserAdmin(Context context)
		throws DSpaceSWORDException
	{
		try
		{
			if (this.authenticated != null)
			{
				Group admin = Group.find(context, 1);
				return admin.isMember(this.authenticated);
			}
			return false;
		}
		catch (SQLException e)
		{
			throw new DSpaceSWORDException(e);
		}
	}
	
	/**
	 * Is the given eperson a DSpace administrator?  This translates
	 * as asking the question of whether the given eperson a member
	 * of the special DSpace group Administrator, with id 1
	 * 
	 * @param eperson
	 * @return	true if administrator, false if not
	 * @throws SQLException
	 */
	public boolean isOnBehalfOfAdmin(Context context)
		throws DSpaceSWORDException
	{
		try
		{
			if (this.onBehalfOf != null)
			{
				Group admin = Group.find(context, 1);
				return admin.isMember(this.onBehalfOf);
			}
			return false;
		}
		catch (SQLException e)
		{
			throw new DSpaceSWORDException(e);
		}
	}
	
	public boolean isUserInGroup(Group group)
	{
		if (this.authenticated != null)
		{
			return isInGroup(group, this.authenticated);
		}
		return false;
	}
	
	public boolean isOnBehalfOfInGroup(Group group)
	{
		if (this.onBehalfOf != null)
		{
			return isInGroup(group, this.onBehalfOf);
		}
		return false;
	}
	
	/**
	 * Is the given eperson in the given group, or any of the groups
	 * that are also members of that group.  This method recurses
	 * until it has exhausted the tree of groups or finds the given
	 * eperson
	 * 
	 * @param group
	 * @param eperson
	 * @return	true if in group, false if not
	 */
	public boolean isInGroup(Group group, EPerson eperson)
	{
		EPerson[] eps = group.getMembers();
		Group[] groups = group.getMemberGroups();
		
		// is the user in the current group
		for (int i = 0; i < eps.length; i++)
		{
			if (eperson.getID() == eps[i].getID())
			{
				return true;
			}
		}
		
		// is the eperson in the sub-groups (recurse)
		if (groups != null && groups.length > 0)
		{
			for (int j = 0; j < groups.length; j++)
			{
				if (isInGroup(groups[j], eperson))
				{
					return true;
				}
			}
		}
		
		// ok, we didn't find you
		return false;
	}
	
	public Collection[] getAllowedCollections(Context context)
		throws DSpaceSWORDException
	{
		try
		{
			// locate the collections to which the authenticated user has ADD rights
			Collection[] cols = Collection.findAuthorized(context, null, Constants.ADD);

			// if there is no onBehalfOf user, just return the list
			if (this.getOnBehalfOf() == null)
			{
				return cols;
			}
			
			// if the onBehalfOf user is an administrator, return the list
			if (this.isOnBehalfOfAdmin(context))
			{
				return cols;
			}
			
			// if we are here, then we have to filter the list of collections
			List<Collection> colList = new ArrayList<Collection>();
			
			for (int i = 0; i < cols.length; i++)
			{
				// we check each collection to see if the onBehalfOf user
				// is permitted to deposit
				
				// urgh, this is so inefficient, but the authorisation API is
				// a total hellish nightmare
				Group subs = cols[i].getSubmitters();
				if (isOnBehalfOfInGroup(subs))
				{
					colList.add(cols[i]);
				}
			}
			
			// now create the new array and return that
			Collection[] newCols = new Collection[colList.size()];
			newCols = colList.toArray((Collection[]) newCols);
			return newCols;
		}
		catch (SQLException e)
		{
			throw new DSpaceSWORDException(e);
		}
	}
	
	public boolean canSubmitTo(Context context, Collection collection)
		throws DSpaceSWORDException
	{
		Group subs = collection.getSubmitters();
		if (isUserAdmin(context))
		{
			if (isOnBehalfOfAdmin(context))
			{
				return true;
			}
			return isOnBehalfOfInGroup(subs);
		}
		else
		{
			if (isUserInGroup(subs))
			{
				if (isOnBehalfOfAdmin(context))
				{
					return true;
				}
				return isOnBehalfOfInGroup(subs);
			}
			return false;
		}
		
	}
}
