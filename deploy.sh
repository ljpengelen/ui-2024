#!/bin/sh

rm -rf docs
mkdir docs

cp static/index.html docs

(cd helix;./deploy.sh)
(cd reagent;./deploy.sh)
(cd replicant;./deploy.sh)
(cd shadow-grove;./deploy.sh)
(cd uix;./deploy.sh)
