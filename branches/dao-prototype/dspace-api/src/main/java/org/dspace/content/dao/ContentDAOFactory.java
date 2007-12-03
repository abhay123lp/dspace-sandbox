package org.dspace.content.dao;

import java.lang.reflect.InvocationTargetException;

import org.dspace.core.Context;

public class ContentDAOFactory
{
    public static ContentDAO getInstance(ContentDAO dao, Context context)
    {
        ContentDAO instantiated = null;

        try
        {
            instantiated = dao.getClass().getConstructor(Context.class).newInstance(context);
        }
        catch (InstantiationException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        catch (InvocationTargetException e)
        {
            e.printStackTrace();
        }
        catch (NoSuchMethodException e)
        {
            e.printStackTrace();
        }

        return instantiated;
    }
}
