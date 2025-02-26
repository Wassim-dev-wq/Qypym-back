#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

CORE_SERVICES=("service-registry" "api-gateway")
AUTH_SERVICES=("keycloak" "auth-service")
APP_SERVICES=("user-service" "match-service")
DB_SERVICES=("user-service-db" "match-service-db" "keycloak-db")
INFRASTRUCTURE=("redis" "kafka" "zookeeper" "kafdrop")
ALL_SERVICES=("${CORE_SERVICES[@]}" "${AUTH_SERVICES[@]}" "${APP_SERVICES[@]}")
ALL_CONTAINERS=("${ALL_SERVICES[@]}" "${DB_SERVICES[@]}" "${INFRASTRUCTURE[@]}")

ENV=${ENV:-"dev"}
ENV_DIR="./.env"
DOCKER_DIR="./docker"
COMPOSE_FILES=(-f docker-compose.yml -f "docker-compose.${ENV}.yml")

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

check_environment() {
    log_step "Checking environment files..."

    if [ ! -f "${ENV_DIR}/.env.base" ]; then
        log_error "Base environment file ${ENV_DIR}/.env.base not found!"
        exit 1
    fi

    if [ ! -f "${ENV_DIR}/.env.${ENV}" ]; then
        log_error "Environment file ${ENV_DIR}/.env.${ENV} not found!"
        exit 1
    fi

    if [ ! -f "${DOCKER_DIR}/docker-compose.yml" ]; then
        log_error "Base Docker compose file ${DOCKER_DIR}/docker-compose.yml not found!"
        exit 1
    fi

    if [ ! -f "${DOCKER_DIR}/docker-compose.${ENV}.yml" ]; then
        log_error "Docker compose file ${DOCKER_DIR}/docker-compose.${ENV}.yml not found!"
        exit 1
    fi

    cat "${ENV_DIR}/.env.base" "${ENV_DIR}/.env.${ENV}" > "${DOCKER_DIR}/.env"
    log_info "Environment set to ${ENV}"
}

check_docker() {
    log_step "Checking Docker and Docker Compose..."

    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker first."
        exit 1
    fi

    if ! docker info &> /dev/null; then
        log_error "Docker daemon is not running. Please start Docker first."
        exit 1
    fi

    if ! command -v docker compose &> /dev/null; then
        log_warn "Docker Compose v2 command not found. Checking for docker-compose..."
        if ! command -v docker-compose &> /dev/null; then
            log_error "Docker Compose is not installed. Please install Docker Compose first."
            exit 1
        fi
        log_warn "Using docker-compose instead of docker compose."
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="docker compose"
    fi
}

maven_clean() {
    local service=$1
    log_info "Cleaning Maven target for $service..."
    (cd "../$service" && ./mvnw clean)
}

maven_build() {
    local service=$1
    local skip_tests=${2:-true}
    local test_param=""
    if [ "$skip_tests" = true ]; then
        test_param="-DskipTests"
    fi
    log_info "Building Maven project for $service..."
    (cd "../$service" && ./mvnw clean install $test_param)

    if [ $? -ne 0 ]; then
        log_error "Maven build failed for $service"
        exit 1
    fi
}

docker_start() {
    local service=$1
    log_info "Starting service $service..."
    (cd "${DOCKER_DIR}" && $COMPOSE_CMD "${COMPOSE_FILES[@]}" up -d "$service")
}

docker_stop() {
    local service=$1
    log_info "Stopping service $service..."
    (cd "${DOCKER_DIR}" && $COMPOSE_CMD "${COMPOSE_FILES[@]}" stop "$service")
}

docker_logs() {
    local service=$1
    local lines=${2:-100}
    log_info "Showing logs for $service..."
    (cd "${DOCKER_DIR}" && $COMPOSE_CMD "${COMPOSE_FILES[@]}" logs --tail=$lines -f "$service")
}

docker_build() {
    local service=$1
    log_info "Building Docker image for $service..."
    (cd "${DOCKER_DIR}" && $COMPOSE_CMD "${COMPOSE_FILES[@]}" build "$service")
}

service_status() {
    log_step "Checking service status..."
    (cd "${DOCKER_DIR}" && $COMPOSE_CMD "${COMPOSE_FILES[@]}" ps)
}

clean() {
    local service=$1
    log_step "Cleaning services..."

    if [ "$service" == "all" ]; then
        for svc in "${ALL_SERVICES[@]}"; do
            maven_clean $svc
        done
    else
        maven_clean $service
    fi
}

prepare() {
    local service=$1
    local skip_tests=${2:-true}
    log_step "Preparing services..."

    if [ "$service" == "all" ]; then
        for svc in "${ALL_SERVICES[@]}"; do
            maven_build "$svc" "$skip_tests"
        done
    else
        maven_build "$service" "$skip_tests"
    fi
}

build() {
    local service=$1
    log_step "Building Docker images..."

    if [ "$service" == "all" ]; then
        for svc in "${ALL_SERVICES[@]}"; do
            docker_build "$svc"
        done
    else
        docker_build $service
    fi
}

start() {
    local service=$1
    log_step "Starting services..."
    check_environment
    check_docker

    if [ "$service" == "all" ]; then
        log_info "Starting all services in order..."

        log_info "Starting databases..."
        for svc in "${DB_SERVICES[@]}"; do
            docker_start "$svc"
        done
        sleep 15

        log_info "Starting infrastructure services..."
        for svc in "${INFRASTRUCTURE[@]}"; do
            docker_start "$svc"
        done
        sleep 10

        log_info "Starting core services..."
        for svc in "${CORE_SERVICES[@]}"; do
            docker_start "$svc"
            sleep 5
        done

        log_info "Starting authentication services..."
        for svc in "${AUTH_SERVICES[@]}"; do
            docker_start "$svc"
            sleep 5
        done

        log_info "Starting application services..."
        for svc in "${APP_SERVICES[@]}"; do
            docker_start "$svc"
            sleep 3
        done

        service_status
    else
        docker_start $service
    fi
}

stop() {
    local service=$1
    log_step "Stopping services..."
    check_environment
    check_docker

    if [ "$service" == "all" ]; then
        log_info "Stopping all services..."
        (cd "${DOCKER_DIR}" && $COMPOSE_CMD "${COMPOSE_FILES[@]}" down)
    else
        docker_stop $service
    fi
}

restart() {
    local service=$1
    log_step "Restarting services..."
    check_environment
    check_docker

    if [ "$service" == "all" ]; then
        stop "all"
        start "all"
    else
        docker_stop $service
        docker_start $service
    fi
}

purge() {
    local service=$1
    log_step "Purging services..."
    check_environment
    check_docker

    if [ "$service" == "all" ]; then
        log_info "Purging all services and volumes..."
        (cd "${DOCKER_DIR}" && $COMPOSE_CMD "${COMPOSE_FILES[@]}" down --rmi all --volumes --remove-orphans)
        docker system prune -f
    else
        log_info "Purging service $service..."
        (cd "${DOCKER_DIR}" && $COMPOSE_CMD "${COMPOSE_FILES[@]}" rm -f "$service")
        docker rmi $(docker images | grep "$service" | awk '{print $3}') 2>/dev/null || true
    fi
}

health() {
    local service=$1
    log_step "Checking service health..."
    check_environment
    check_docker

    if [ "$service" == "all" ]; then
        for svc in "${ALL_CONTAINERS[@]}"; do
            container_name=$(cd "${DOCKER_DIR}" && $COMPOSE_CMD "${COMPOSE_FILES[@]}" ps -q "$svc" 2>/dev/null)
            if [ -n "$container_name" ]; then
                health_status=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "No health check")
                echo -e "${svc}: ${health_status}"
            else
                echo -e "${svc}: Not running"
            fi
        done
    else
        container_name=$(cd "${DOCKER_DIR}" && $COMPOSE_CMD "${COMPOSE_FILES[@]}" ps -q "$service" 2>/dev/null)
        if [ -n "$container_name" ]; then
            health_status=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "No health check")
            echo -e "${service}: ${health_status}"
        else
            echo -e "${service}: Not running"
        fi
    fi
}

usage() {
    echo -e "${BLUE}QyPym Microservices Management Script${NC}"
    echo "Usage: $0 [environment] command [service] [options]"
    echo ""
    echo "Environments:"
    echo "  dev (default) | prod"
    echo ""
    echo "Commands:"
    echo "  prepare   - Clean and build Maven projects"
    echo "  build     - Build Docker images"
    echo "  start     - Start services"
    echo "  stop      - Stop services"
    echo "  restart   - Restart services"
    echo "  logs      - Show service logs"
    echo "  status    - Show status of all containers"
    echo "  health    - Check health status of services"
    echo "  purge     - Remove containers, images, and volumes"
    echo "  clean     - Clean Maven projects"
    echo ""
    echo "Services:"
    echo "  all (default) | service-name"
    echo "  Available services:"
    echo "    Core: ${CORE_SERVICES[*]}"
    echo "    Auth: ${AUTH_SERVICES[*]}"
    echo "    Apps: ${APP_SERVICES[*]}"
    echo "    DB: ${DB_SERVICES[*]}"
    echo "    Infra: ${INFRASTRUCTURE[*]}"
    echo ""
    echo "Examples:"
    echo "  $0 dev start all"
    echo "  $0 prod start api-gateway"
    echo "  $0 dev logs user-service"
    echo "  $0 health match-service"
    exit 1
}

if [ $# -lt 1 ]; then
    usage
fi

if [[ $1 == "dev" || $1 == "prod" ]]; then
    ENV=$1
    shift
fi

COMMAND=$1
SERVICE=${2:-all}

case "$COMMAND" in
    prepare)
        prepare $SERVICE ${3:-true}
        ;;
    build)
        build $SERVICE
        ;;
    start)
        start $SERVICE
        ;;
    stop)
        stop $SERVICE
        ;;
    restart)
        restart $SERVICE
        ;;
    logs)
        docker_logs $SERVICE ${3:-100}
        ;;
    status)
        service_status
        ;;
    health)
        health $SERVICE
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

exit 0