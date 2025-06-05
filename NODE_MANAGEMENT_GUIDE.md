# Navigation Node Management Feature

## Overview

I've implemented a comprehensive navigation node management system that allows you to manually place
navigation nodes on the map instead of using hardcoded nodes. This gives you full control over the
pathfinding network.

## What I Changed

### 1. Removed Hardcoded Nodes

- All predefined navigation nodes have been removed from `NavigationRepository.kt`
- The system now starts with an empty node network

### 2. Added Node Management Infrastructure

- **NavNode Model**: Added `isUserCreated` field to distinguish user-placed nodes
- **Database Storage**: User nodes are stored in Firebase Firestore under
  `nodes/{userId}/user_nodes`
- **Repository Methods**: Added CRUD operations for navigation nodes
- **ViewModel Integration**: Added node management methods to `MapViewModel`

### 3. Enhanced UI Components

#### MapScreen

- **Node Placement Button**: Floating action button to toggle node placement mode
- **Visual Indicators**: Nodes are displayed with different colors based on type:
    - Blue: Walkway nodes
    - Green: Door nodes
    - Purple: Elevator nodes
    - Amber: Stairs nodes
    - Red: Obstacle nodes
- **Interactive Controls**: Tap to select, drag to move, long-press for options

#### AdminScreen

- **Mode Toggle**: Switch between POI management and Node management
- **Dual Interface**: Separate controls for POI and Node operations
- **Node Properties**: View and edit node type, connections, and position

### 4. Enhanced FloorPlanViewer

- **Node Visualization**: Displays user-created navigation nodes with icons
- **Touch Interaction**: Supports tapping, dragging, and placement of nodes
- **Mode Switching**: Different behaviors for POI mode vs Node mode
- **Visual Feedback**: Selected nodes are highlighted with larger circles

## How to Use

### Step 1: Access Node Placement Mode

1. Open the app and navigate to the Map screen
2. Log in as an admin user (you'll see "👑 ADMIN MODE" at the top)
3. Click the floating action button (+ icon) in the bottom-right corner
4. The button will turn blue and show a checkmark - you're now in node placement mode

### Step 2: Place Navigation Nodes

1. **Add Nodes**: Tap anywhere on the map to place a navigation node
2. **Select Nodes**: Tap on existing nodes to select them (they'll show a larger highlight)
3. **Move Nodes**: In admin mode, drag nodes to reposition them
4. **Delete Nodes**: Select a node and click the red delete button that appears

### Step 3: Alternative Admin Interface

1. Navigate to Admin Panel from the navigation bar
2. Click "Node Mode" button to switch from POI to Node management
3. Toggle "Edit Mode" switch to enable placement/editing
4. Use the map interface to place and manage nodes

### Step 4: Node Properties

- **Type**: All new nodes default to WALKWAY type
- **Connections**: Nodes can be connected to create pathways (advanced feature)
- **Positioning**: Precise coordinate control through drag-and-drop

## Important Notes

### Pathfinding Behavior

- **With Nodes**: The pathfinding engine will use your placed nodes to find routes
- **Without Nodes**: The system falls back to basic obstacle avoidance
- **Node Connections**: Currently auto-managed, but can be customized through the repository methods

### Permissions

- Only admin users can place/edit navigation nodes
- Regular users can only view existing nodes and POIs
- Node data is saved per-user in Firebase

### Best Practices for Node Placement

1. **Main Pathways**: Place nodes along major corridors and walkways
2. **Intersections**: Add nodes at corridor intersections for better routing
3. **Entrances**: Place nodes near room entrances and building exits
4. **Spacing**: Keep reasonable distance between nodes (20-50 pixels recommended)
5. **Coverage**: Ensure all important areas are reachable through your node network

## Technical Implementation

### Data Storage

```kotlin
// Nodes are stored in Firebase under:
// nodes/{userId}/user_nodes/{nodeId}
{
  "x": 650.0,
  "y": 400.0, 
  "floor": 1,
  "connections": ["node_123", "node_456"],
  "isWalkable": true,
  "type": "WALKWAY"
}
```

### Key Classes Modified

- `NavNode`: Added user creation flag
- `NavigationRepository`: Node CRUD operations
- `MapViewModel`: Node management state and actions
- `FloorPlanViewer`: Node visualization and interaction
- `MapScreen`: Node placement controls
- `AdminScreen`: Advanced node management

## Next Steps

After placing your navigation nodes:

1. **Test Routing**: Select a POI to see how pathfinding uses your nodes
2. **Refine Placement**: Adjust node positions based on routing results
3. **Add Connections**: Use the repository methods to create custom node connections
4. **Share Network**: Your node network will be available to other users of your app instance

The system now gives you complete control over the navigation network. Start by placing nodes along
the main walkable paths in your building, then test the pathfinding to ensure good route coverage.