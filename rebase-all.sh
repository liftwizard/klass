#!/bin/bash

set -Eeuo pipefail

git stash

ONTO=main

declare -a branches=(
  "bootstrap-writer"
  "KlassResponseReladomoJsonSerializer"
  "broke"
  "fix"
  "include-abstract"
  "klass-model-reladomo-tree_invariants"
  "unused-deps"
  "bootstrap-coverage-example"
  "klass-graphql-reladomo-finder-fetcher"
  "klass-reladomo-graphql-deep-fetcher"
  "bootstrap-inheritance"
  "klass-reladomo-tree-deep-fetcher"
  "reference-properties"
  "split-project"
  "spectacle"
  "reladomo-interface"
  "no-mapped-mod"
  "no-JsonView"
  "klass-model-bootstrapped-dropwizard-application-logging"
  "image-rendering"
  "id-mismatch-json"
  "graphql-fragment-generation"
  "dto-exclude-private"
  "docs"
  "blueprint-item-table"
  "badges"
  "astah"
  "QuestionResourceManualTest"
  "CacheAssetServlet"
  "GraphQLFragmentGenerator"
  "ReladomoContextJsonSerializer"
  "foreign-key-order"
  "graphql-logging-config"
  "***REMOVED***-meta-model-json"
  "domain-model-json"
  "wip"
  "wip2"
  "klass.version"
  "quote-sql-identifiers"
  "intellij-formatting"
  "open-source"
  # "foreign-key-inference-1105"
  # "***REMOVED***-game-data"
  # "***REMOVED***-config"
  # "***REMOVED***-blueprint-content"
  # "audit-only-version"
)

for branch in "${branches[@]}"
do
    echo "Rebasing branch: $branch"
    git checkout "$branch" && git pull origin "$ONTO" --rebase
done

git checkout dev
