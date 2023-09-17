FROM amazoncorretto:17

WORKDIR /gatekey

# These all need to be provided when building the image
ARG ALLOWED_CALLERS
ARG TELEGRAM_BOT_TOKEN
ARG CERT_PASSWORD

# Set env vars
ENV GATE_KEY_ALLOWED_CALLERS $ALLOWED_CALLERS
ENV GATE_KEY_TELEGRAM_BOT_TOKEN $TELEGRAM_BOT_TOKEN
ENV GATE_KEY_DB_PATH '/persistent/KeyDb.db'
ENV GATE_KEY_CERT_PATH '/persistent/certificate.pfw'
ENV GATE_KEY_CERT_PASSWORD $CERT_PASSWORD

VOLUME /GateKey
EXPOSE 443
ARG TAR_FILE=build/distributions/GateKey-0.0.1.tar
ADD ${TAR_FILE} ./
ENTRYPOINT ["/gatekey/GateKey-0.0.1/bin/GateKey"]