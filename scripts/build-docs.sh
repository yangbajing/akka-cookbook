#!/bin/sh

#sbt book/paradox
rm -rf docs/*
mv book/target/paradox/site/main/* docs/

