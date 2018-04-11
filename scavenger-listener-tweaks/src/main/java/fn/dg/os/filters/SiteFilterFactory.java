package fn.dg.os.filters;

import io.vertx.core.json.JsonObject;
import org.infinispan.filter.NamedFactory;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilterFactory;
import org.infinispan.notifications.cachelistener.filter.EventType;
import org.kohsuke.MetaInfServices;

import java.util.logging.Logger;

@MetaInfServices
@NamedFactory(name = "site-filter-factory")
public class SiteFilterFactory implements CacheEventFilterFactory {

    static final Logger log = Logger.getLogger(SiteFilterFactory.class.getName());

    private static final String SITE_NAME = System.getenv("SITE");
    private static final SiteFilter FILTER_INSTANCE = new SiteFilter();

    @Override
    public CacheEventFilter<String, String> getFilter(Object[] objects) {
        log.info("Called getFilter");

        if (SITE_NAME == null)
            log.severe("SITE env variable must be defined");
        else
            log.info(String.format(
                "env.SITE=%s", SITE_NAME
            ));

        return FILTER_INSTANCE;
    }

    private static final class SiteFilter implements CacheEventFilter<String, String> {

        @Override
        public boolean accept(String key, String oldValue, Metadata oldMetadata, String newValue, Metadata newMetadata, EventType eventType) {
            log.fine(String.format(
                "Accept %s for env.SITE=%s?", newValue, SITE_NAME
            ));

            final JsonObject json = new JsonObject(newValue);
            final String site = json.getString("data-center");

            log.fine(String.format(
                "Site in value is: %s", site
            ));

            return site.equals(SITE_NAME);
        }

    }

}
