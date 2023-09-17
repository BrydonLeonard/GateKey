FROM amazoncorretto:17

WORKDIR /gatekey

# Set config env vars
ENV GATE_KEY_ALLOWED_CALLERS $ALLOWED_CALLERS
ENV GATE_KEY_TELEGRAM_BOT_TOKEN $TELEGRAM_BOT_TOKEN
ENV GATE_KEY_DB_PATH '/persistent/KeyDb.db'
ENV GATE_KEY_CERT_PATH '/persistent/certificate.pfw'

VOLUME /GateKey
EXPOSE 443
ARG TAR_FILE=build/distributions/GateKey-0.0.1.tar
ADD ${TAR_FILE} ./
ENTRYPOINT ["/gatekey/GateKey-0.0.1/bin/GateKey"]