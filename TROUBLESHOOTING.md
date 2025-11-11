# Troubleshooting Guide

## Issue: "Load balancer does not have available server for client: hub-user-data"

If you see this error even though the service appears registered in Eureka, follow these steps:

### 1. Verify Service Registration

Check that the service is registered in Eureka:

```bash
# Check if service is registered
curl http://localhost:8761/eureka/apps/HUB-USER-DATA

# Or check all services
curl http://localhost:8761/eureka/apps
```

You should see the service with:
- `status: UP`
- Recent `lastRenewalTimestamp` (within last 30 seconds)
- `vipAddress: hub-user-data` (lowercase)

### 2. Client Application Configuration

Your client application (`agendamentos`) **must** have the following configuration in `application.yml`:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    fetch-registry: true  # CRITICAL: Must be true
    registry-fetch-interval-seconds: 5  # Refresh every 5 seconds

spring:
  cloud:
    loadbalancer:
      cache:
        enabled: false  # Disable cache to avoid stale lookups
        ttl: 0
```

**Important Notes:**
- `fetch-registry: true` is **required** - without it, the client won't fetch services from Eureka
- `registry-fetch-interval-seconds: 5` ensures the client refreshes frequently
- Disabling LoadBalancer cache prevents stale service instances

### 3. Enable Debug Logging

Add debug logging to see what's happening:

```yaml
logging:
  level:
    org.springframework.cloud.loadbalancer: DEBUG
    org.springframework.cloud.openfeign.loadbalancer: DEBUG
    com.netflix.discovery: DEBUG
    com.netflix.eureka: DEBUG
```

This will show:
- When the registry is fetched
- What services are discovered
- LoadBalancer service selection

### 4. Restart Client Application

**After registering services in Eureka, you must restart your client application** to fetch the updated registry.

The client fetches the registry on startup, so if the service was registered after the client started, it won't see it until:
- The client restarts, OR
- The `registry-fetch-interval-seconds` interval passes (if configured)

### 5. Verify Service Discovery

Check the client logs for:
- `Fetched registry successfully` - indicates registry fetch worked
- `DiscoveryClient_AGENDAMENTOS` - your client's discovery client
- Any errors about fetching the registry

### 6. Check Service Name Case

Eureka stores services with uppercase `app` name (`HUB-USER-DATA`) but uses lowercase `vipAddress` (`hub-user-data`). Spring Cloud should handle this automatically, but verify your Feign client uses lowercase:

```java
@FeignClient(value = "hub-user-data", path = "/hub-user-data")  // lowercase
```

### 7. Timing Issues

If the service was just registered:
1. Wait a few seconds for the heartbeat to complete
2. Restart the client application
3. Or wait for the `registry-fetch-interval-seconds` to pass

### 8. Verify Eureka Server URL

Ensure your client is pointing to the correct Eureka server:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/  # Must match your server port
```

### Common Issues

#### Issue: Service registered but client can't find it
**Solution:** 
- Ensure `fetch-registry: true` in client config
- Restart client application
- Check Eureka server URL is correct

#### Issue: Service found but requests fail
**Solution:**
- Check if service has base path - add `path = "/base-path"` to `@FeignClient`
- Verify external URL is accessible
- Check HTTPS/HTTP configuration

#### Issue: Service disappears after a while
**Solution:**
- Check heartbeat service is running (should see logs every 25 seconds)
- Verify `@EnableScheduling` is enabled in Eureka server
- Re-register the service if evicted

### Quick Verification Checklist

- [ ] Service appears in `http://localhost:8761/eureka/apps/HUB-USER-DATA`
- [ ] Service status is `UP`
- [ ] `lastRenewalTimestamp` is recent (within 30 seconds)
- [ ] Client has `fetch-registry: true`
- [ ] Client has correct `defaultZone` URL
- [ ] Client application was restarted after service registration
- [ ] LoadBalancer cache is disabled
- [ ] Debug logging is enabled

### Still Not Working?

1. Check Eureka server logs for registration errors
2. Check client logs for discovery errors
3. Verify network connectivity between client and Eureka server
4. Try accessing Eureka REST API directly from client machine:
   ```bash
   curl http://localhost:8761/eureka/apps
   ```

