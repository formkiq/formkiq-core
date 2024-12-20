FROM quay.io/keycloak/keycloak:26.0.6

# Set environment variables for Keycloak
ENV KC_BOOTSTRAP_ADMIN_USERNAME=admin
ENV KC_BOOTSTRAP_ADMIN_PASSWORD=admin

# Copy the config.json file into the image
COPY formkiq-realm.json /tmp/formkiq-realm.json

# Expose necessary ports
EXPOSE 8081