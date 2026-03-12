# HatchTracker Module Registry

| Module | Description | Key Components |
| :--- | :--- | :--- |
| `:core:domain` | Domain models and Repository contracts. | `Flock`, `FlockRepository`. |
| `:core:navigation` | Navigation abstractions. | `AppRoute`, `Navigator`. |
| `:core:data` | Data persistence and repository implementations. | `FlockRepositoryImpl`, `FlockDao`. |
| `:core:ui` | Design system and common UI components. | `Theme`, `AskHatchyCard`. |
| `:core:di` | Dependency Injection bindings. | `RepositoryModule`. |
| `:core:testing` | Shared test utilities. | `CoroutinesTestRule`, `FakeFlockRepository`. |
| `:core:common` | Base classes and network utilities. | `NetworkMonitor`, `BaseViewModel`. |
| `:core:billing` | Subscription and entitlement management. | `SubscriptionStateManager`. |
| `:core:ads` | Advertising logic. | `AdManager`. |
| `:core:infrastructure` | Low-level OS integrations (Receivers, Policy). | `BootReceiver`. |
| `:feature:support` | Support ticket management. | `HelpSupportScreen`. |
| `:feature:breeding` | Breeding simulations and advisor. | `BreedingScreen`. |
... (and more)
