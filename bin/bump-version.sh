#!/bin/sh

newVersion=`perl -npe "s/version in ThisBuild\s+:=\s+\"(.*)\"/\1/" version.sbt | sed -e "/^$/d"`

for f in $(/bin/ls src/sbt-test/sbt-one-log/*/project/plugins.sbt); do
  echo $f;
  perl -i -npe "s/addSbtPlugin\(\"io.github.chikei\".*/addSbtPlugin\(\"io.github.chikei\" % \"sbt-one-log\" % \"$newVersion\"\)/" $f; \
done;
