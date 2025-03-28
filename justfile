set dotenv-filename := ".envrc"

group_id_with_slashes := "cool/klass"

import ".just/console.just"
import ".just/maven.just"
import ".just/git.just"
import ".just/git-test.just"

# Setup the project (mise) and run the default build (mvn)
default: mise mvn

# `mise install`
mise:
    mise install --quiet
    mise current

# clean (maven and git)
@clean: _clean-git _clean-maven _clean-m2

markdownlint:
    markdownlint --config .markdownlint.jsonc  --fix .

# mvn archetype
@archetype MVN=default_mvn:
    just _run "{{MVN}} {{ANSI_GREEN}}install{{ANSI_DEFAULT}} --also-make --projects klass-maven-archetype"

# Override this with a command called `woof` which notifies you in whatever ways you prefer.
# My `woof` command uses `echo`, `say`, and sends a Pushover notification.
echo_command := env('ECHO_COMMAND', "echo")