#!/usr/bin/env bash
set -x

if [ "$1" = 'run-webapi' ]; then
  set -e
  if [ -f "/var/lib/shared/honeur.env" ]; then
    source /var/lib/shared/honeur.env
    export $(cut -d= -f1 /var/lib/shared/honeur.env)
  fi

  if [[ ! -z "$FEDER8_WEBAPI_SECURE" && "$FEDER8_WEBAPI_SECURE" == "true" ]]; then
    JAVA_OPTS="${JAVA_OPTS} -Dsecurity.provider=AtlasRegularSecurity -Dsecurity.token.expiration=43200 -Dsecurity.origin=* -Dsecurity.cors.enabled=true"

    if [[ ! -z "$FEDER8_WEBAPI_CENTRAL" && "$FEDER8_WEBAPI_CENTRAL" == "true" ]]; then
      JAVA_OPTS="${JAVA_OPTS} -Dsecurity.oid.clientId=${FEDER8_WEBAPI_OIDC_CLIENT_ID} -Dsecurity.oid.apiSecret=${FEDER8_WEBAPI_OIDC_SECRET} -Dsecurity.oid.url=${FEDER8_WEBAPI_OIDC_ISSUER_URI} -Dsecurity.oid.redirectUrl=${FEDER8_WEBAPI_OIDC_REDIRECT_URL}"
    else

    fi
  fi

  exec java ${DEFAULT_JAVA_OPTS} ${JAVA_OPTS} \
    -cp ".:WebAPI.jar:WEB-INF/lib/*.jar${CLASSPATH}" \
    org.springframework.boot.loader.WarLauncher
fi

exec "$@"