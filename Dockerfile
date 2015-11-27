# openwalrus
#
# Version     v0.0.4
FROM          ubuntu
MAINTAINER    Justin Walrath <walrathjaw@gmail.com>

# Update Box and obtain dependencies
RUN           apt-get update -y apt-get -y install software-properties-common
RUN           add-apt-repository -y ppa:openjdk-r/ppa
RUN           apt-get update && apt-get install -y openjdk-8-jdk unzip mongodb

# Set up environment variables
ENV           PORT              9000
ENV           APP_NAME          openwalrus
ENV           APP_VERSION       v0.0.4
ENV           APP_INSTALL_DIR   /tmp/$APP_NAME
ENV           REPOSITORY        https://github.com/maximx1/openwalrus

# Install the application
RUN           git clone --branch $APP_VERSION $REPOSITORY $APP_INSTALL_DIR
WORKDIR       $APP_INSTALL_DIR
RUN           ./activator playUpdateSecret
RUN           ./activator stage

# Set up mongo install
RUN           mkdir -p /data/db
VOLUME        ["/data/db"]

# Expose contained OS outside of docker
EXPOSE        $PORT

# Set run command
CMD           mongod --fork --logpath /var/log/mongod && $APP_INSTALL_DIR/target/universal/stage/bin/$APP_NAME -Dhttp.port=$PORT -J-Xms32M -J-Xmx64M