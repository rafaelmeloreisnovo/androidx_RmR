# RmR Dependency Graph

This graph defines the isolated RmR dependency boundaries and keeps AndroidX core modules unmodified.

```mermaid
graph TD
    APP[Consumer App]

    APP --> RMR_EXT[rmr-extensions\nnamespace: rmr.extensions\npackage: rmr.core.extensions]
    APP --> RMR_LIFECYCLE[rmr-lifecycle]
    APP --> RMR_NAV[rmr-navigation]
    APP --> RMR_PREF[rmr-preference]
    APP --> RMR_ROOM[rmr-room]
    APP --> RAFAELIA[rafaelia]

    RMR_EXT --> RMR_CORE[rmr-core]
    RMR_LIFECYCLE --> RMR_CORE
    RMR_NAV --> RMR_CORE
    RMR_PREF --> RMR_CORE
    RMR_ROOM --> RMR_CORE
    RAFAELIA --> RAFAELIA_CORE[rafaelia-core]

    RMR_CORE -.isolated from .-> AXCORE[androidx.core (upstream)]
```

## Notes

- `rmr-extensions` is the dedicated extension module with `rmr.*` namespace isolation.
- `rmr-core` remains the stable primitive layer used by all RmR feature modules.
- No upstream AndroidX core package files are modified by this refactor.
