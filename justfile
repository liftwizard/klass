set shell := ["bash", "-O", "globstar", "-c"]
set dotenv-filename := ".envrc"

group_id_with_slashes := "cool/klass"

import ".just/maven.just"
import ".just/git.just"
import ".just/git-rebase.just"
import ".just/git-test.just"

# Setup the project (mise) and run the default build (mvn)
default: mise mvn

# mise install
mise:
    mise plugin install maven
    mise plugin install mvnd https://github.com/joschi/asdf-mvnd
    mise install
    mise current

# clean (maven and git)
clean: _clean-git _clean-maven _clean-m2

# end-to-end test for git-test
test: _check-local-modifications clean mvn && _check-local-modifications

# Count lines of code
scc:
    scc **/src/{main,test}

# Override this with a command called `woof` which notifies you in whatever ways you prefer.
# My `woof` command uses `echo`, `say`, and sends a Pushover notification.
echo_command := env('ECHO_COMMAND', "echo")
