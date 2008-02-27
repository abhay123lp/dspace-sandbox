package org.dspace.workflow;

import java.util.UUID;

import org.dspace.core.ApplicationService;
import org.dspace.core.Context;
import org.dspace.uri.ObjectIdentifier;

public class WorkflowItemFactory
{
    public static WorkflowItem getInstance(Context context)
    {
        UUID uuid = UUID.randomUUID();
        WorkflowItem wfi = new WorkflowItem();
        wfi.setIdentifier(new ObjectIdentifier(uuid));
        ApplicationService.save(context, WorkflowItem.class, wfi);

        return wfi;        
    }
}
