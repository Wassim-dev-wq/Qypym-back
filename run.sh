#!/bin/bash

MICROSERVICES=("config-server" "service-discovery" "api-gateway")

function clean() {
    local service=$1
    if [ "$service" == "all" ]; then
        echo "Cleaning all Maven target directories..."
        for service in "${MICROSERVICES[@]}"; do
            echo "Cleaning $service..."
            (cd $service && mvn clean)
        done
    else
        echo "Cleaning Maven target directory for $service..."
        (cd $service && mvn clean)
    fi
}

function prepare() {
    local service=$1
    clean $service
    if [ "$service" == "all" ]; then
        echo "Building Maven projects for all services..."
        for service in "${MICROSERVICES[@]}"; do
            echo "Building $service..."
            (cd $service && mvn clean install -DskipTests)
        done
    else
        echo "Building Maven project for $service..."
        (cd $service && mvn clean install -DskipTests)
    fi
}

function build() {
    local service=$1
    if [ "$service" == "all" ]; then
        echo "Building Docker images for all services..."
        docker-compose build
    else
        echo "Building Docker image for $service..."
        docker-compose build $service
    fi
}

function start() {
    local service=$1
    if [ "$service" == "all" ]; then
        echo "Starting all services..."
        docker-compose up -d
    else
        echo "Starting service $service..."
        docker-compose up -d $service
    fi
}

function build_start() {
    build $service
    start $service
}

function down() {
    local service=$1
    if [ "$service" == "all" ]; then
        echo "Stopping and removing all services..."
        docker-compose down
    else
        echo "Stopping and removing service $service..."
        docker-compose stop $service
        docker-compose rm -f $service
    fi
}

function purge() {
    down $1
    local service=$1
    if [ "$service" == "all" ]; then
        echo "Removing all Docker images and volumes..."
        docker-compose down --rmi all --volumes --remove-orphans
    else
        echo "Removing Docker image for $service..."
        docker-compose rm -f $service
        docker rmi $(docker images $service -q)
    fi
}

if [ $# -lt 1 ]; then
    usage
fi

COMMAND=$1
SERVICE=${2:-all}

case "$COMMAND" in
    prepare)
        prepare $SERVICE
        ;;
    build)
        build $SERVICE
        ;;
    start)
        start $SERVICE
        ;;
    build_start)
        build_start $SERVICE
        ;;
    down)
        down $SERVICE
        ;;
    purge)
        purge $SERVICE
        ;;
    clean)
        clean $SERVICE
        ;;
    *)
        usage
        ;;
esac
