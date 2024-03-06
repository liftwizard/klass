#!/usr/bin/env bash

#
# Copyright 2024 Craig Motlin
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -euxo pipefail

# fresh
git -C ~/projects/klass-fresh checkout main
git -C ~/projects/klass-fresh pull origin main --rebase --set-upstream
git -C ~/projects/klass-fresh checkout dev
git -C ~/projects/klass-fresh pull origin dev --rebase --set-upstream
git -C ~/projects/klass-fresh fetch --all --prune
git -C ~/projects/klass-fresh optimize

# klass
rm -rf ~/projects/klass-rewrite
git clone ~/projects/klass-fresh ~/projects/klass-rewrite
cd ~/projects/klass-rewrite

# klass.com ==> klass.cool
git -C ~/projects/klass-rewrite filter-repo \
    --paths-from-file ~/projects/klass/rewrite/rewrite-1-paths.txt

# klass.com ==> klass.cool
git -C ~/projects/klass-rewrite filter-repo \
    --replace-text ~/projects/klass/rewrite/rewrite-1-text.txt \
    --replace-message ~/projects/klass/rewrite/rewrite-1-message.txt

# remove ***REMOVED*** directories and strings
git -C ~/projects/klass-rewrite filter-repo \
    --invert-paths \
    --paths-from-file ~/projects/klass/rewrite/rewrite-2-paths.txt \
    --replace-text ~/projects/klass/rewrite/rewrite-2-text.txt \
    --replace-message ~/projects/klass/rewrite/rewrite-2-message.txt

# Change dates earlier than 2024-03-06 to 2024-03-06
git -C ~/projects/klass-rewrite filter-repo \
    --commit-callback '
        if commit.committer_date < b"1709683200 -0000" or commit.author_date < b"1709683200 -0000":
            commit.committer_date = b"1709683200 -0000"
            commit.author_date    = b"1709683200 -0000"
    ' --force

git -C ~/projects/klass-rewrite test add --test default 'bash build.sh --pushover --record'

# ***REMOVED***.school
rm -rf ~/projects/***REMOVED***-rewrite
git clone ~/projects/klass-fresh ~/projects/***REMOVED***-rewrite
cd ~/projects/***REMOVED***-rewrite

# klass.com ==> klass.cool
git -C ~/projects/***REMOVED***-rewrite filter-repo \
    --paths-from-file ~/projects/klass/rewrite/rewrite-1-paths.txt

# klass.com ==> klass.cool
git -C ~/projects/***REMOVED***-rewrite filter-repo \
    --replace-text ~/projects/klass/rewrite/rewrite-1-text.txt \
    --replace-message ~/projects/klass/rewrite/rewrite-1-message.txt

# remove klass
git -C ~/projects/***REMOVED***-rewrite filter-repo \
    --invert-paths \
    --paths-from-file ~/projects/klass/rewrite/rewrite-3-paths-remove.txt

git -C ~/projects/***REMOVED***-rewrite filter-repo \
    --path-rename xample-projects/***REMOVED***/:

git -C ~/projects/***REMOVED***-rewrite filter-repo \
    --replace-text ~/projects/klass/rewrite/rewrite-3-text.txt \
    --replace-message ~/projects/klass/rewrite/rewrite-3-message.txt

git -C ~/projects/***REMOVED***-rewrite test add --test default 'bash build.sh --pushover --record'

bash build.sh
