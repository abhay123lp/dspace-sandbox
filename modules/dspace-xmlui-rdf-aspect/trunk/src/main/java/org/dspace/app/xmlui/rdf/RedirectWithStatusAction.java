package org.dspace.app.xmlui.rdf;

import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpResponse;

public class RedirectWithStatusAction extends AbstractAction
{

    @SuppressWarnings("unchecked")
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {

        String uri = parameters.getParameter("uri",null);
        int status = parameters.getParameterAsInteger("status");

        HttpResponse response = (HttpResponse) ObjectModelHelper.getResponse(objectModel);
        
        response.setStatus(status );
        response.setHeader("Location", uri);
        return null;
    }

}
