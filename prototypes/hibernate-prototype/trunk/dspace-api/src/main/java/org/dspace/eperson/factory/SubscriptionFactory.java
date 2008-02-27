package org.dspace.eperson.factory;

import java.util.UUID;

import org.dspace.core.Context;
import org.dspace.eperson.Subscription;

public class SubscriptionFactory
{
    public static Subscription getInstance(Context context) {
        UUID uuid = UUID.randomUUID();
        Subscription subscription = new Subscription();
        subscription.setUuid(uuid);
        return subscription;
    }
}
