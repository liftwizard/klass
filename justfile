set dotenv-filename := ".envrc"

group_id_with_slashes := "cool/klass"

import ".just/console.just"
import ".just/maven.just"
import ".just/git.just"
import ".just/git-test.just"

# `just --list--unsorted`
[group('default')]
default:
    @just --list --unsorted

# `mise install`
mise:
    mise install --quiet
    mise current

# clean (maven and git)
@clean: _clean-git _clean-maven _clean-m2

markdownlint:
    markdownlint --config .markdownlint.jsonc  --fix .

# Run all formatting tools for pre-commit
precommit: mvn
    uv tool run pre-commit run --all-files

# mvn archetype
@archetype MVN=default_mvn:
    just _run "{{MVN}} {{ANSI_GREEN}}install{{ANSI_DEFAULT}} --also-make --projects klass-maven-archetype"

# Override this with a command called `woof` which notifies you in whatever ways you prefer.
# My `woof` command uses `echo`, `say`, and sends a Pushover notification.
echo_command := env('ECHO_COMMAND', "echo")

coverage_example_app := "xample-projects/coverage-example/coverage-example-dropwizard-application"
coverage_example_config := coverage_example_app / "config-local.json5"
coverage_example_main := "cool.klass.xample.coverage.dropwizard.application.CoverageExampleApplication"

# Run coverage-example app and open swagger UI
[group('run')]
run-coverage-example:
    #!/usr/bin/env bash
    set -uo pipefail
    echo "{{ANSI_BOLD}}Starting CoverageExampleApplication...{{ANSI_NORMAL}}"
    echo "Swagger UI will open at http://localhost:8080/swagger/"
    echo "ReDoc will be available at http://localhost:8080/redoc/"
    echo ""

    # Open swagger after a delay (in background)
    (sleep 20 && open http://localhost:8080/swagger/) &

    # Run the application (foreground, so Ctrl+C stops it)
    just _run "mvn -f {{ANSI_YELLOW}}{{coverage_example_app}}/pom.xml{{ANSI_DEFAULT}} exec:java -Dexec.mainClass=\"{{ANSI_YELLOW}}{{coverage_example_main}}{{ANSI_DEFAULT}}\" -Dexec.args=\"server {{ANSI_YELLOW}}{{coverage_example_config}}{{ANSI_DEFAULT}}\""
