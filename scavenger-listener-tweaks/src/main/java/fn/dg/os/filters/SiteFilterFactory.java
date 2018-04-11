package fn.dg.os.filters;

import io.vertx.core.json.JsonObject;
import org.infinispan.filter.NamedFactory;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilterFactory;
import org.infinispan.notifications.cachelistener.filter.EventType;
import org.kohsuke.MetaInfServices;

@MetaInfServices
@NamedFactory(name = "site-filter-factory")
public class SiteFilterFactory implements CacheEventFilterFactory {

    private static final String SITE_NAME = System.getenv("SITE");
    private static final SiteFilter FILTER_INSTANCE = new SiteFilter();

    @Override
    public CacheEventFilter<String, String> getFilter(Object[] objects) {
        return FILTER_INSTANCE;
    }

    private static final class SiteFilter implements CacheEventFilter<String, String> {

        @Override
        public boolean accept(String key, String oldValue, Metadata oldMetadata, String newValue, Metadata newMetadata, EventType eventType) {
            final JsonObject json = new JsonObject(newValue);
            final String site = json.getString("data-center");
            return site.equals(SITE_NAME);
        }

    }

}
