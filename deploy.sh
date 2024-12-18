#!/bin/sh
rm -rf docs
(cd reagent;./deploy.sh)
(cd uix;./deploy.sh)
(cd helix;./deploy.sh)
