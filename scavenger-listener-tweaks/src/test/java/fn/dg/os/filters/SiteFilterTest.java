package fn.dg.os.filters;

import io.vertx.core.json.JsonObject;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientEvent;
import org.junit.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SiteFilterTest {

    @Test
    public void test000() {
        final RemoteCacheManager remote = new RemoteCacheManager();
        final RemoteCache<String, String> cache = remote.getCache();
        cache.clear();

        final TestListener listener = new TestListener(cache);
        cache.addClientListener(listener);

        try {
            cache.put("001"
                , new JsonObject().put("data-center", "A").toString()
            );
            listener.expectOnlyCreatedEvent("001");

            cache.put("002"
                , new JsonObject().put("data-center", "B").toString()
            );
            listener.expectNoEvents();
        } finally {
            cache.removeClientListener(listener);
        }
    }

    @ClientListener(filterFactoryName = "site-filter-factory")
    //@ClientListener
    private static final class TestListener {

        BlockingQueue<ClientCacheEntryCreatedEvent> events =
            new ArrayBlockingQueue<>(16);

        final RemoteCache<String, String> cache;

        private TestListener(RemoteCache<String, String> cache) {
            this.cache = cache;
        }

        @ClientCacheEntryCreated
        @SuppressWarnings("unused")
        public void handleCreatedEvent(ClientCacheEntryCreatedEvent e) {
            events.add(e);
        }

        public void expectOnlyCreatedEvent(String key) {
            expectSingleEvent(key);
        }

        public void expectSingleEvent(String key) {
            ClientCacheEntryCreatedEvent createdEvent = pollEvent();
            assertEquals(key, createdEvent.getKey());
            assertEquals(serverDataVersion(cache, key), createdEvent.getVersion());
            assertEquals(0, events.size());
        }

        public void expectNoEvents() {
            assertEquals(events.toString(), 0, events.size());
        }

        private long serverDataVersion(RemoteCache<String, ?> cache, String key) {
            long v1 = cache.getVersioned(key).getVersion();
            long v2 = cache.getWithMetadata(key).getVersion();
            assertEquals(v1, v2);
            return v1;
        }

        public ClientCacheEntryCreatedEvent pollEvent() {
            try {
                ClientCacheEntryCreatedEvent e =
                    events.poll(10, TimeUnit.SECONDS);
                assertNotNull(e);
                return e;
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
        }

    }

}
