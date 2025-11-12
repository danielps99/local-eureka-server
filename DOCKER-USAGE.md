# Docker Usage Guide

## Quick Start

### 1. Build the image
```bash
docker-compose build
```

### 2. Start the container
```bash
docker-compose up -d
```

### 3. View logs
```bash
docker-compose logs -f
```

### 4. Stop the container
```bash
docker-compose down
```

## Changing External Services Configuration

**The easiest way:** Simply edit the `config/external-services.json` file on your host machine, then restart the container:

```bash
# Edit the file
nano config/external-services.json
# or
vim config/external-services.json

# Restart to apply changes
docker-compose restart
```

The file is mounted as a volume, so any changes you make will be picked up after restarting.

## Example: Adding a New Service

Edit `config/external-services.json`:

```json
{
  "services": [
    {
      "serviceName": "hub-user-data",
      "externalUrl": "https://api-dev.bdws.com.br/hub-user-data",
      "port": 443,
      "secure": true,
      "instanceId": null
    },
    {
      "serviceName": "my-new-service",
      "externalUrl": "https://example.com/my-service",
      "port": 443,
      "secure": true
    }
  ]
}
```

Then restart:
```bash
docker-compose restart
```

## Using Docker Run (Alternative)

If you prefer `docker run` instead of docker-compose:

```bash
# Build the image
docker build -t eureka-server .

# Run with mounted config
# EUREKA_EXTERNAL_SERVICES_CONFIG_FILE is maped to eureka.external-services.config-file
# and read in @Value("${eureka.external-services.config-file:classpath:external-services.json}")
docker run -d \
  --name eurekaserver \
  -p 8761:8761 \
  -v $(pwd)/config/external-services.json:/app/config/external-services.json:ro \
  -e EUREKA_EXTERNAL_SERVICES_CONFIG_FILE=file:/app/config/external-services.json \
  eureka-server
```

## Access Eureka Dashboard

Once running, access the Eureka dashboard at:
- http://localhost:8761