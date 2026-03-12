# Modularization Migration Guide

## How to add a new Feature
1.  **Define Domain**: Add needed models and repository interfaces to `:core:domain`.
2.  **Implement Data**: implement repository interfaces in `:core:data`.
3.  **Bind DI**: Update `:core:di` to bind the new implementation.
4.  **Create Module**: Create `:feature:yourfeature` using `hatchtracker.android.library` plugin.
5.  **Wire Up**: depend on `:core:domain`, `:core:navigation`, and `:core:ui`.

## Best Practices
- Keep ViewModels in feature modules.
- Use `Navigator` for cross-feature navigation.
- Always use `libs.*` for dependencies.
- Write tests in feature modules using `:core:testing`.
