# Audit Data

So far, we've built a simple read service. We could add create, update, and delete and we'd have a "simple crud" version of Stack Overflow. But the real Stack Overflow services are more advanced. All updates and deletes are _non-destructive_. When you edit a question or answer, you can still view all of its old versions.

## Versions

[Here's a real example](https://stackoverflow.com/a/49938998) from Stack Overflow. The answer has 3 versions, at the time of writing. The url returns the latest version. There's a link with text like ["edited Apr 20 at 10:10"](https://stackoverflow.com/posts/49938998/revisions) which you can follow to see all versions. Each version has a number, a timestamp, the author's username, and the full text of the answer at the time.

## Optimistic locking

Imagine that two users click [Edit](https://stackoverflow.com/questions/49938832/did-eclipse-collections-get-deprecated-by-java-8/49938998#49938998) at the same time, make changes, and click "Save Edits". Without a carefully designed api, whoever saves second could silently overwrite the changes of the first. Stack Overflow uses version numbers to implement optimistic locking. Each mutating service takes the edited text, plus the version number that the edit applies to. In this example, the second user would get an error message saying something about attempting to edit version 3, while the current version is 4. They'd then have an opportunity to view the new edits and retry against the new version.

## Audit features

Klass treats these as separate features, which build upon each other.

- **Temporal milestoning** makes edits non-destructive, and adds the active time ranges to each piece of data.
- **Versioning** adds simple version numbers (1, 2, 3, ...) to each active time range. Versioned data must be temporally milestoned.
- **Auditing** adds tracking of the created-by author, last-updated-by author, and created-on time. Audited data must be temporally milestoned.
- **Optimistic locking** enhances "mutating" services to expect a version parameter, and to fail with a helpful error message on stale versions. Optimistically locked data must be versioned.
