#!/usr/bin/env bash
#=========================================
# THIS IS LOCAL DISTRIBUTION SCRIPT
#=========================================
VERSION=2.18
#===================================================
# Remove previous distribution
#===================================================
rm -rf grapes-$VERSION grapes-$VERSION.tar.gz
#===================================================
# create directory and copy config and bin files
#===================================================
mkdir -p grapes-$VERSION/lib/core
mkdir -p grapes-$VERSION/lib/services
mkdir -p grapes-$VERSION/config
cp -r bin grapes-$VERSION/.
cp config/train.yaml grapes-$VERSION/config/.
#===================================================
# copy JAR to lib directory
#===================================================
cp target/grapes-$VERSION-core.jar grapes-$VERSION/lib/core/
#===================================================
# create tarball for the package and copy to userweb
# web space with clas12 privileges.
#============================================
tar -cf grapes-$VERSION.tar grapes-$VERSION
gzip grapes-$VERSION.tar
scp grapes-$VERSION.tar.gz clas12@ifarm1901:/group/clas/www/clasweb/html/clas12offline/distribution/grapes/
#============================================
# THE END OF THE SCRIPT
#============================================
