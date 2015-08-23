# openwalrus
#
# Version     v0.0.1
FROM          ubuntu
MAINTAINER    Justin Walrath <walrathjaw@gmail.com>

# Update Box and obtain dependencies
RUN           apt-get update -y apt-get -y install software-properties-common
RUN           add-apt-repository -y ppa:openjdk-r/ppa
RUN           apt-get update && apt-get install -y openjdk-8-jdk unzip mongodb

# Set up environment variables
ENV           PORT              9000
ENV           APP_NAME          openwalrus
ENV           APP_VERSION       v0.0.1
ENV           APP_ZIP_NAME      $APP_NAME-$APP_VERSION.zip
ENV           APP_INSTALL_DIR   /tmp/$APP_NAME
ENV           APP_RUN_DIR       $APP_INSTALL_DIR/$APP_NAME-1.0-SNAPSHOT

# Install the application
ADD           $APP_ZIP_NAME $APP_INSTALL_DIR/
WORKDIR       $APP_INSTALL_DIR
RUN           unzip $APP_ZIP_NAME
WORKDIR       APP_RUN_DIR

# Set up mongo install
RUN           mkdir -p /data/db
VOLUME        ["/data/db"]

# Expose contained OS outside of docker
EXPOSE        $PORT

# Set run command
CMD           mongod --fork --logpath /var/log/mongod && $APP_RUN_DIR/bin/$APP_NAME -Dhttp.port=$PORT -J-Xms32M -J-Xmx64M