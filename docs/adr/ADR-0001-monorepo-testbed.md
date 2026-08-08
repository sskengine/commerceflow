# ADR-0001: Monorepo for the controlled ECA testbed

Status: Accepted

CommerceFlow services are kept in one Maven reactor during the ECA development stage. This gives repeatable cross-service diffs, one CI run, atomic contract changes and deterministic demonstrations. Service boundaries remain deployable independently. A later experiment may split selected consumers into separate repositories to test ECA's cross-repository knowledge graph.

