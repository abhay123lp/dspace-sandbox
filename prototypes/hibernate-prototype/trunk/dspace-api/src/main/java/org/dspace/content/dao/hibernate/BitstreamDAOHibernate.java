package org.dspace.content.dao.hibernate;

import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.content.dao.BitstreamDAO;
import org.dspace.core.Context;

public class BitstreamDAOHibernate extends BitstreamDAO {
    public BitstreamDAOHibernate(Context context) {
        super(context);
    }
    
    public List<Bitstream> getDeletedBitstreams() {
        //FIXME scrivere la query
        return null;
    }
}
