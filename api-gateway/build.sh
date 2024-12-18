#!/bin/bash

# Get the parent directory path
PARENT_DIR=$(dirname $(pwd))

# Create temporary build context directory
echo "Setting up build context..."
mkdir -p build-context/repository/jdk
mkdir -p build-context/target

# Copy necessary files to build context
echo "Copying files to build context..."
cp "$PARENT_DIR/repository/jdk/jdk-17.0.12_linux-x64_bin.tar.gz" build-context/repository/jdk/
cp target/*.jar build-context/target/

# Copy Dockerfile
cp Dockerfile build-context/

# Check if JAR exists
if [ ! -f "target/"*.jar ]; then
    echo "JAR file not found in target directory. Building the application..."
    # If using Maven
    if [ -f "pom.xml" ]; then
        mvn clean package -DskipTests
    # If using Gradle
    elif [ -f "build.gradle" ]; then
        ./gradlew clean build -x test
    else
        echo "Neither pom.xml nor build.gradle found. Please build the application first."
        exit 1
    fi
fi

# Load base image if not present
if ! docker images | grep -q "debian.*bullseye-slim"; then
    echo "Loading base image..."
    docker load < "$PARENT_DIR/repository/docker/debian-base.tar"
fi

# Build the image with optimizations
echo "Building Docker image..."
docker build \
    --platform linux/amd64 \
    --network=none \
    --build-arg BUILDKIT_INLINE_CACHE=1 \
    -t api-gateway:latest \
    build-context

# Verify the Java installation in the image
echo "Verifying Java installation in the image..."
docker run --rm api-gateway:latest java -version

# Create a compressed backup of the image
echo "Saving Docker image..."
docker save api-gateway:latest | gzip > "$PARENT_DIR/repository/docker/api-gateway.tar.gz"

# Cleanup
echo "Cleaning up..."
rm -rf build-context

echo "Build completed successfully!"