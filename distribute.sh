#!/usr/bin/env bash
#=========================================
# THIS IS LOCAL DISTRIBUTION SCRIPT
#=========================================
VERSION=2.6
rm -rf grapes-$VERSION grapes-$VERSION.tar.gz
mkdir -p grapes-$VERSION/lib/core
mkdir -p grapes-$VERSION/lib/services
mkdir -p grapes-$VERSION/config
cp -r bin grapes-$VERSION/.
cp target/grapes-$VERSION-SNAPSHOT.jar grapes-$VERSION/lib/core/
#cp $PROJECT/Distribution/jnp/jnp-distro/jaw-0.9/lib/jaw-0.9.jar grapes-$VERSION/lib/core/
cp ../jnp/jnp-jaw/target/jnp-jaw-2.0-SNAPSHOT-jar-with-dependencies.jar grapes-$VERSION/lib/core/jaw-2.0.jar
cp config/train.yaml grapes-$VERSION/config/.
tar -cf grapes-$VERSION.tar grapes-$VERSION
gzip grapes-$VERSION.tar
scp grapes-$VERSION.tar.gz clas12@ifarm1801:/group/clas/www/clasweb/html/clas12offline/distribution/grapes/
#rm -rf grapes-$VERSION grapes-$VERSION.tar.gz
