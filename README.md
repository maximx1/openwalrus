[![Build Status](https://travis-ci.org/maximx1/openwalrus.svg?branch=develop)](https://travis-ci.org/maximx1/openwalrus)
[![Stories in Ready](https://badge.waffle.io/maximx1/openwalrus.svg?label=ready&title=Ready)](http://waffle.io/maximx1/openwalrus)

OpenWalrus
==========

#### More to come
* This is a publicly built secret project.
* I'm going to be building it for a while and storing in Github without description.
* This is just a side project for me to do when I have time.

#### Installation
First download the latest tarball from `https://github.com/maximx1/openwalrus/releases`, untar it, and then run:
```
    # docker build -t maximx1/openwalrus .
    # docker run -p 9000:9000 -d --log-driver=syslog -v ~/.openwalrus-data/:/data/db maximx1/openwalrus
```

#### Configuration
You need to update application.conf. You can the current `conf/application.conf` prior to building or create a new one from [this template](https://github.com/maximx1/openwalrus/blob/master/conf/application.conf). 

Tasks:
* Run `./activator playUpdateSecret` to update the play secret.
* Update `mongodb.default.host="mongodb://localhost:27017"` with a valid [Mongo URI](https://docs.mongodb.org/manual/reference/connection-string/).
* Set your collection name in `mongodb.default.name="openwalrus-db"`.
* You can uncomment `# application.context="/openwalrus"` to add a context root.