# choose base image to build off of
FROM node:lts-alpine

# set the current working directory for all commands
WORKDIR /usr/src/app

ARG DT_ACTIVE_WEB_ENV
ENV DT_ACTIVE_WEB_ENV=$DT_ACTIVE_WEB_ENV

# copy these over first and run 'npm install' so the node_modules will be cached
# until the package.json / lock changes
COPY package.json .
COPY package-lock.json .
RUN npm install

# copy over all code files
COPY . .

# expose internal docker container port to external environment
EXPOSE 4200

# specify default command to run when we run the image
ENTRYPOINT /usr/src/app/node_modules/.bin/ng serve --host 0.0.0.0 --disable-host-check -c ${DT_ACTIVE_WEB_ENV}
