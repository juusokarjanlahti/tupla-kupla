# tupla-kupla backend

Spring Boot backend, built with Maven.

## Requirements

JDK 25. Use the bundled wrapper (`./mvnw`) rather than a system Maven.

## Formatting

Java sources are formatted with [Spotless](https://github.com/diffplug/spotless) using
[google-java-format](https://github.com/google/google-java-format) in Google style —
2-space indents, 100-column lines.

| Command | What it does |
| --- | --- |
| `./mvnw spotless:apply` | Reformat all Java sources in place |
| `./mvnw spotless:check` | Report violations without changing anything |

Spotless also removes unused imports and sorts the remaining ones.

It is deliberately **not** bound to a build phase, so it never fails a normal build. If you
later want it enforced, add an `<execution>` binding `check` to the `validate` phase; that is
the point at which CI would start rejecting unformatted code.

It is scoped to Java sources only. Spotless can sort and reformat `pom.xml` as well, but
turning that on rewrote all 77 lines of it to no real benefit, so it is left alone.

Most IDEs can apply google-java-format on save — see the google-java-format README for the
IntelliJ and Eclipse plugins — which is less friction than remembering the Maven goal.
