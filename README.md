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