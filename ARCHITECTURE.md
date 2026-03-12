# HatchTracker Architecture Guide

## High-Level modularization
The project is structured into **Core**, **Feature**, and **App** layers to ensure scalability and maintainability.

### 🧱 Core Layer
The foundation of the app. Core modules should be highly reusable and have minimal dependencies on other modules.

- **`:core:domain`**: Pure Kotlin. Contains domain models, use cases, and repository interfaces. NO Android dependencies.
- **`:core:data`**: Android Library. Implements repository interfaces and manages local/remote data sources (Room, Firestore).
- **`:core:navigation`**: Pure Kotlin. Defines the navigation contracts (`AppRoute`) and the `Navigator` interface.
- **`:core:testing`**: Pure Kotlin. Shared test helpers and fake implementations for testing.

### 🚀 Feature Layer
Encapsulates individual business features. Feature modules depend on Core modules but NOT on other Feature modules.

- **`:feature:flock`**, **`:feature:incubation`**, etc.

### 📱 App Layer
The main entry point (`:app`). Orchestrates features, provides DI bindings, and implements high-level navigation.

## Dependency Rules
1.  **Feature modules** must only depend on `:core:*` modules.
2.  **`:core:domain`** must have NO dependencies on other modules (except possibly `kotlinx-coroutines`).
3.  **`:core:data`** depends on `:core:domain`.
4.  **Circular dependencies** are strictly prohibited.
