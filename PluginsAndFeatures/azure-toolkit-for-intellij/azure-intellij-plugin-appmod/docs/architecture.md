# App Modernization Module Architecture

## Overview

The `azure-intellij-plugin-appmod` module provides the "Migrate to Azure" functionality in Azure Toolkit for IntelliJ. It serves as a bridge to integrate GitHub Copilot App Modernization plugin with Azure Toolkit.

## Plugin Relationship Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                              IntelliJ IDEA                                       │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │                      Azure Toolkit for IntelliJ                            │  │
│  │                                                                            │  │
│  │  ┌────────────────────────┐    ┌────────────────────────────────────────┐  │  │
│  │  │   service-explorer     │    │      resource-connector-lib            │  │  │
│  │  │                        │    │                                        │  │  │
│  │  │  ┌──────────────────┐  │    │  ┌──────────────────────────────────┐  │  │  │
│  │  │  │MigrateToAzureNode│  │    │  │   MigrateToAzureFacetNode        │  │  │  │
│  │  │  └────────┬─────────┘  │    │  └───────────────┬──────────────────┘  │  │  │
│  │  └───────────┼────────────┘    └──────────────────┼─────────────────────┘  │  │
│  │              │                                    │                        │  │
│  │              └─────────────────┬──────────────────┘                        │  │
│  │                                ▼                                           │  │
│  │  ┌──────────────────────────────────────────────────────────────────────┐  │  │
│  │  │                 azure-intellij-plugin-appmod                         │  │  │
│  │  │                                                                      │  │  │
│  │  │  • IMigrateOptionProvider (Extension Point Interface)                │  │  │
│  │  │  • MigrateNodeData (Data Model)                                      │  │  │
│  │  │  • MigratePluginInstaller (Plugin Detection/Installation)            │  │  │
│  │  │  • MigrateToAzureAction (Context Menu)                               │  │  │
│  │  │                                                                      │  │  │
│  │  └──────────────────────────────────┬───────────────────────────────────┘  │  │
│  │                                     │                                      │  │
│  │                                     │ Extension Point                      │  │
│  │                                     ▼                                      │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                        │                                         │
│                                        │ implements                              │
│                                        ▼                                         │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │              GitHub Copilot App Modernization Plugin                       │  │
│  │                         (appmod-intellij)                                  │  │
│  │                                                                            │  │
│  │  ┌──────────────────────────────────────────────────────────────────────┐  │  │
│  │  │     MyMigrationProvider implements IMigrateOptionProvider            │  │  │
│  │  │                                                                      │  │  │
│  │  │     • createNodeData() → Returns migration options                   │  │  │
│  │  │     • isApplicable()   → Check project compatibility                 │  │  │
│  │  └──────────────────────────────────────────────────────────────────────┘  │  │
│  │                                                                            │  │
│  │  Depends on: com.github.copilot (GitHub Copilot)                          │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

## Data Flow

```
User Action (click/expand)
         │
         ▼
┌─────────────────────┐
│  Entry Point        │  (MigrateToAzureNode / MigrateToAzureFacetNode / MigrateToAzureAction)
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐     No      ┌─────────────────────┐
│ Plugin Installed?   │────────────▶│ Show Install Dialog │
└─────────┬───────────┘             └─────────────────────┘
          │ Yes
          ▼
┌─────────────────────┐
│ Load Extension      │
│ Providers           │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Filter by           │
│ isApplicable()      │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Sort by Priority    │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Call createNodeData │
│ for each provider   │
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│ Display Nodes       │
│ in UI               │
└─────────────────────┘
```

## Module Structure

```
azure-intellij-plugin-appmod/
├── build.gradle.kts
├── docs/
│   └── architecture.md
└── src/main/
    ├── java/com/microsoft/azure/toolkit/intellij/appmod/
    │   ├── IMigrateOptionProvider.java       # Extension Point interface
    │   ├── MigrateNodeData.java              # Node data model
    │   ├── MigratePluginInstaller.java       # Plugin detection & installation
    │   ├── MigrateToAzureNode.java           # Service Explorer entry point
    │   ├── MigrateToAzureAction.java         # Context menu entry point
    │   ├── InstallPluginDialog.java          # Installation confirmation dialog
    │   └── RestartIdeDialog.java             # Restart prompt dialog
    └── resources/
        ├── META-INF/azure-intellij-plugin-appmod.xml
        └── icons/app_mod.svg
```

## Entry Points

The module provides **three entry points** for users to access migration functionality:

### 1. Service Explorer Node (`MigrateToAzureNode`)
- **Location**: Azure Explorer panel → "Migrate to Azure" node
- **Behavior**: 
  - If plugins installed → Shows child nodes from extension providers
  - If plugins not installed → Double-click triggers installation dialog

### 2. Project Explorer Node (`MigrateToAzureFacetNode`)
- **Location**: Project Explorer → Azure facet → "Migrate to Azure" node
- **Note**: Located in `azure-intellij-resource-connector-lib` module (due to `AbstractAzureFacetNode` inheritance)
- **Behavior**: Same as Service Explorer Node

### 3. Context Menu Action (`MigrateToAzureAction`)
- **Location**: Right-click on project/module → "Migrate to Azure" submenu
- **Behavior**: 
  - If plugins installed → Shows child actions from extension providers
  - If plugins not installed → Single "Install Plugins" action

## Extension Point

### Definition
```xml
<extensionPoint name="migrateOptionProvider"
                interface="com.microsoft.azure.toolkit.intellij.appmod.IMigrateOptionProvider"/>
```

Full ID: `com.microsoft.tooling.msservices.intellij.azure.migrateOptionProvider`

### Interface: `IMigrateOptionProvider`
```java
public interface IMigrateOptionProvider {
    // Check if this provider applies to the given project
    boolean isApplicable(@Nonnull Project project);
    
    // Create node data for display (can return multiple nodes)
    @Nonnull List<MigrateNodeData> createNodeData(@Nonnull Project project);
    
    // Priority for ordering (lower = first)
    default int getPriority() { return 100; }
}
```

### Data Model: `MigrateNodeData`
```java
MigrateNodeData.builder()
    .label("Node Label")                    // Required: display text
    .description("Optional description")     // Shown as location string
    .tooltip("Hover tooltip")               // Tooltip text
    .iconPath("/icons/my_icon.svg")         // Icon path (falls back to app_mod.svg)
    .visible(true)                          // Visibility control
    .onDoubleClick(anActionEvent -> {...})  // Double-click handler
    .children(childList)                    // Static children
    .childrenLoader(() -> loadChildren())   // OR lazy-loaded children
    .build();
```

## Plugin Detection & Installation

### `MigratePluginInstaller`
Central utility class for plugin management:

```java
// Check if plugins are installed
MigratePluginInstaller.isAppModPluginInstalled();  // com.github.copilot.appmod
MigratePluginInstaller.isCopilotInstalled();       // com.github.copilot

// Show installation confirmation dialog
MigratePluginInstaller.showInstallConfirmation(project, onConfirmCallback);

// Trigger installation (IntelliJ handles the rest)
MigratePluginInstaller.installPlugin(project);

// Dev mode detection (runIde task)
MigratePluginInstaller.isRunningInDevMode();
```

### Installation Flow
1. User triggers install (double-click node or context menu)
2. `showInstallConfirmation()` shows confirmation dialog
3. On confirm, `installPlugin()` calls `PluginsAdvertiser.installAndEnable()`
4. IntelliJ platform handles:
   - Plugin selection dialog (with all plugins pre-selected)
   - Download and installation
   - Restart prompt
5. In dev mode: Special message shown (don't click IDE restart, re-run `./gradlew runIde`)

## Module Dependencies

```
azure-intellij-plugin-appmod (base module)
    ↑
    ├── azure-intellij-plugin-service-explorer
    │   └── Uses: MigrateToAzureNode, Extension Point
    │
    └── azure-intellij-resource-connector-lib
        └── Contains: MigrateToAzureFacetNode (due to inheritance constraint)
```

### Why `MigrateToAzureFacetNode` is in connector-lib?
- Must extend `AbstractAzureFacetNode<AzureModule>` from connector-lib
- Moving `AbstractAzureFacetNode` to appmod would require moving many other classes
- Current design minimizes code changes while maintaining clean architecture

## External Plugin Integration

The `appmod-intellij` plugin (GitHub Copilot App Modernization) should:

1. Add dependency on `azure-intellij-plugin-appmod`
2. Implement `IMigrateOptionProvider` extension
3. Register in its `plugin.xml`:
```xml
<extensions defaultExtensionNs="com.microsoft.tooling.msservices.intellij.azure">
    <migrateOptionProvider implementation="com.example.MyMigrationProvider"/>
</extensions>
```

## UI Behavior Summary

| State | Service Explorer | Project Explorer | Context Menu |
|-------|-----------------|------------------|--------------|
| Plugins NOT installed | Node shows "(Install...)" suffix, double-click triggers install | Same as Service Explorer | Shows "Install Plugins" action |
| Plugins installed | Expand to show child nodes from providers | Same as Service Explorer | Shows submenu with actions from providers |

## Icon

- **Path**: `/icons/app_mod.svg`
- **Location**: `azure-intellij-plugin-appmod/src/main/resources/icons/`
- **Usage**: Centralized icon for all migrate-related nodes and actions
