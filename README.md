# Bcarserver

[![Java CI with Maven](https://github.com/futesat/bcarserver/actions/workflows/ci.yml/badge.svg)](https://github.com/futesat/bcarserver/actions/workflows/ci.yml)
[![Docker Build and Push](https://github.com/futesat/bcarserver/actions/workflows/docker.yml/badge.svg)](https://github.com/futesat/bcarserver/actions/workflows/docker.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)

Boskicar Server - A Spring Boot application for controlling a Raspberry Pi-based car with GPIO controls.

## Features

- RESTful API for car control (forward, backward, steering)
- Joystick and throttle control
- GPIO integration with Raspberry Pi (lights, fans, motors)
- PWM control via pi-blaster
- Steering wheel calibration
- UDP message handling
- System control (shutdown, reboot, deploy)

## Requirements

- Java 21
- Maven 3.9+
- Docker (optional, for containerized deployment)
- Raspberry Pi with GPIO support (for hardware features)

## Building

### Using Maven

```bash
mvn clean package
```

### Using Docker

```bash
docker build -t bcarserver .
```

## Running

### Locally

```bash
java -jar target/bcarserver-1.2.0-RELEASE.jar
```

### With Docker

```bash
docker run -p 3333:3333 \
  -e SERVER_PORT=3333 \
  --privileged \
  bcarserver
```

**Note:** The `--privileged` flag is required for GPIO access on Raspberry Pi.

## Configuration

Environment variables:

- `SERVER_PORT`: Server port (default: 3333)

## API Endpoints

### Movement Control

- `POST /forward/{speed}` - Move forward (speed: 0-100)
- `POST /backward/{speed}` - Move backward (speed: 0-100)
- `POST /stop` - Stop all movement
- `POST /joystick/{angle}/{strength}` - Joystick control (angle: 0-360, strength: 0-100)
- `POST /throttle/{fbOrderSpeed}/{lrStrength}` - Throttle control
- `POST /steeringwheel/{angle}` - Set steering wheel angle (0-180)

### System Control

- `POST /lights/{status}` - Control lights (ON/OFF)
- `POST /fans/{status}` - Control fans (ON/OFF)
- `POST /mobilecontrol/{status}` - Enable/disable mobile control (ON/OFF)
- `POST /enginecontrol/{status}` - Enable/disable engine control (ON/OFF)
- `POST /steeringwheelcontrol/{status}` - Enable/disable steering wheel control (ON/OFF)
- `POST /throttlecontrol/{status}` - Enable/disable throttle control (ON/OFF)

### Status & Maintenance

- `GET /status/{complete}` - Get system status (complete: true/false)
- `GET /logs` - Retrieve application logs
- `POST /calibrate/steeringwheel` - Calibrate steering wheel
- `POST /shutdown` - Shutdown system
- `POST /reboot` - Reboot system
- `POST /deploy` - Deploy new JAR file

## Testing

Run tests with:

```bash
mvn test
```

Tests include:
- Context loading verification
- API endpoint integration tests
- GPIO null-safety checks (for non-hardware environments)

## Hardware Setup

This application is designed to run on a Raspberry Pi with:
- GPIO pins for relay control
- ADS1015 ADC for analog input (steering wheel position sensor)
- pi-blaster for PWM control
- I2C bus for sensor communication

## Development

The application uses:
- Spring Boot 2.7.18
- Spring Integration for UDP messaging
- Pi4J for GPIO control
- Scheduled tasks for order processing (150ms intervals)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Service Installation

For systemd service installation on Raspberry Pi, see `scripts/boskicar.service`.
