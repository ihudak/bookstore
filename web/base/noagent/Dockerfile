# choose base image to build off of
FROM node:lts-alpine
MAINTAINER dynatrace.com

ENV DOCKER_VER=1.7.2

# set the current working directory for all commands
WORKDIR /usr/src/app

# expose internal docker container port to external environment
EXPOSE 4200

# specify default command to run when we run the image
ENTRYPOINT if [ -z ${BOOKSTORE_BASE_SRV_URL+x} ] || [ "$DT_ACTIVE_WEB_ENV" != custom ] ; then \
               /usr/src/app/node_modules/.bin/ng serve --host 0.0.0.0 --disable-host-check -c ${DT_ACTIVE_WEB_ENV}; \
           else \
               sed -i "s|http://kubernetes.docker.internal/api|$BOOKSTORE_BASE_SRV_URL|g" ./src/environments/* ; \
               sed -i "s|http://localhost|$BOOKSTORE_BASE_SRV_URL|g" ./src/environments/* ; \
               sed -i "s|selectedTenant: \'\',|selectedTenant: \'$DT_ENV_ID\',|g" ./src/environments/* ; \
               /usr/src/app/node_modules/.bin/ng build ; \
               export DT_ACTIVE_WEB_ENV=production; \
               /usr/src/app/node_modules/.bin/ng serve --host 0.0.0.0 --disable-host-check -c ${DT_ACTIVE_WEB_ENV}; \
           fi
