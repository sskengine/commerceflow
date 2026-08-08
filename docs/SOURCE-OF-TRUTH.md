# Sources of truth

The canonical product requirements are `CommerceFlow Technical Specification v2.1` and its published Confluence pages. This repository does not silently redefine those requirements.

Repository artifacts have the following authority:

| Artifact | Authority |
|---|---|
| Confluence technical specification | approved business and system requirements |
| `docs/TRACEABILITY.csv` | implementation mapping for this release |
| `contracts/openapi` | executable HTTP contract baseline |
| `contracts/asyncapi` | executable event contract baseline |
| Java code and tests | actual implemented behavior |
| `docs/IMPLEMENTATION-STATUS.md` | explicit planned versus implemented scope |
| Jira/PR/commit | authorized change scope and evidence |

When artifacts disagree, ECA should report the disagreement and cite both sources; it must not infer that the newest Git commit automatically overrides an approved requirement.

