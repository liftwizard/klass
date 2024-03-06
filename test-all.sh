#!/bin/bash

set -uo pipefail

BRANCHES=$(git branch --list --contains origin/main)
REVISIONS=$(git rev-list $BRANCHES --not origin/main --)
git test run --retest --test default $REVISIONS
