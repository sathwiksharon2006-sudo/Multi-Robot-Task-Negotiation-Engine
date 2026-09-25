# Multi-Robot Task Negotiation Engine
### High-Density Autonomous Mobile Robot (AMR) Swarm Coordination Platform

---

## 1. Problem Statement
In high-density industrial smart warehouses and gigafactories, operating fleets of 500+ heterogeneous Autonomous Mobile Robots (AMRs) presents critical challenges:
- **Centralized Single Point of Failure (SPOF):** If a central coordination server drops offline or experiences network partitions, standard fleets lock up, halting operations.
- **Narrow Bottlenecks & Deadlocks:** High robot density causes circular waiting in corridors and intersections where traditional greedy algorithms freeze.
- **Dynamic Resource Contention:** Simultaneous claims over shared pickup stations, delivery drop-offs, and charging pads.
- **Heterogeneous Workloads & Energy Depletion:** Different robot classes (heavy load, fast transport, picker, inspection) have varying speeds, payloads, and battery draw profiles.

---

## 2. Solution Overview
The **Multi-Robot Task Negotiation Engine** is a hybrid decentralized coordination platform. While the Central Controller provides global mission ingestion and aggregate telemetry, the individual robots are autonomous decision-making agents capable of:
1. **Peer-to-Peer Task Allocation:** Multi-attribute distributed bidding over simulated local radio gossip when the central server is unavailable.
2. **Trajectory Collision Forecasting:** Continuous 3–10s lookahead using spatial grid hashing detecting head-on, rear-end, crossing, and bottleneck conflicts before physical convergence.
3. **Dynamic Right-of-Way (RoW) Negotiation:** Dynamic priority exchange factoring mission criticality, battery urgency, inertia, and starvation-prevention waiting escalation.
4. **Graph-Theoretic Deadlock Resolution:** Real-time Wait-For Graph (WFG) cycle detection that directs lowest-impact robots into turnout bays or bypass routes.
5. **Proactive Energy Management:** Automatic mission detachment and handover upon critical battery detection with autonomous navigation to charging pads.
6. **Graceful Degradation:** Full operational continuity when the central controller drops offline.

---

## 3. System Architecture

```
                       [ OPERATIONAL DASHBOARD ]
                                   │
              ┌────────────────────┴────────────────────┐
              ▼                                         ▼
   [ Central Controller ]                    [ P2P Gossip Mesh ]
   - Global Ingestion                        - Neighborhood Auctions
   - System KPIs                             - Local Conflict Resolution
              │                                         │
              └────────────────────┬────────────────────┘
                                   ▼
                       [ FLEET SIMULATOR ENGINE ]
        ┌──────────────────────────┼──────────────────────────┐
        ▼                          ▼                          ▼
 [ Spatial Hash Grid ]      [ Collision Predictor ]   [ Deadlock Engine ]
 - O(1) Spatial Lookups    - Trajectory Lookahead     - Wait-For Graph (WFG)
 - 500+ AMRs @ 60 FPS      - Head-on / Crossing Risk  - Cycle Breaker / Bypass
        │                          │                          │
        └──────────────────────────┼──────────────────────────┘
                                   ▼
                         [ ROBOT AGENT FLEET ]
                   - Transport AMR  - Picker AMR
                   - Heavy AGV      - Fast Transport
                   - Inspection Rover
```

### Decision Locality:
- **Global Decisions (Central Controller):** Task batch generation, global charging slot registry, long-term analytics.
- **Local Decisions (AMR Peer-to-Peer):** Immediate corridor yield/pass decisions, intersection traversal sequencing, evasive braking, turnout docking, emergency mission handover, and local task gossip.

---

## 4. Robot Model & Capabilities
The fleet consists of 5 heterogeneous AMR classes:
1. **Transport AMR:** General utility (1.0 m/s, 120 kg capacity, 100 Ah).
2. **Picker AMR:** Automated shelving picking arm (0.9 m/s, 60 kg capacity, 90 Ah).
3. **Heavy Load AGV:** Heavy pallet movement (0.7 m/s, 400 kg capacity, 180 Ah).
4. **Fast Transport AMR:** Express item transit (1.5 m/s, 50 kg capacity, 80 Ah).
5. **Inspection Drone-Rover:** Sensor telemetry and safety scanning (1.3 m/s, 20 kg capacity, 75 Ah).

---

## 5. Algorithmic Highlights

### A. Task Bidding Score
$$Score = (D_{pickup} \times 1.5) + WorkloadCost + BatteryPenalty - CapabilityBonus - PriorityBenefit$$
- Where $BatteryPenalty$ heavily penalizes robots $< 25\%$ battery ($+500$).
- When the central controller is OFFLINE, robots form neighborhood gossip clusters to evaluate bids locally without external infrastructure.

### B. Dynamic Right-of-Way & Starvation Prevention
$$RoW = EmergencyBonus + (Priority \times 50) + \min(T_{wait} \times 15, 400) + BatteryUrgency + LoadInertia$$
- **Starvation Guard:** A robot stalled at an intersection escalates its priority by $+15$ points/sec. Within 8–10 seconds, it outscores oncoming traffic, preventing queue starvation.

### C. Wait-For Graph (WFG) Deadlock Resolution
- A directed graph $G = (V, E)$ tracks robot dependencies ($R_A \rightarrow R_B$).
- Cycles ($R_1 \rightarrow R_2 \rightarrow \dots \rightarrow R_1$) are identified via DFS.
- The engine identifies the robot with the lowest cost impact and issues an evasive command (pull into turnout bay, reverse step, or compute dynamic A* bypass).

---

## 6. Jury Demonstration Guide (10 Automated Phases)
Clicking **"⚡ START JURY DEMO"** triggers an automated 10-phase demonstration:
1. **Phase 1: Initial Deployment:** 100 robots spawn with auction-based task distribution.
2. **Phase 2: Fleet Scale (500 Robots):** Demonstrates high-density fleet coordination using spatial hashing.
3. **Phase 3: Workload Wave:** Injects multi-priority task batches showing capability-matched bidding.
4. **Phase 4: Collision Prediction & P2P RoW:** Injects opposing trajectory; shows trajectory forecast, peer negotiation, and corridor yield.
5. **Phase 5: Circular Deadlock Detection:** Forces 4-way circular dependency; WFG cycle triggers turnout diversion.
6. **Phase 6: Critical Battery & Mission Handover:** Drains battery to 5%; shows immediate mission migration and auto-docking.
7. **Phase 7: Hardware Failure Obstacle:** Injects motor failure; corridor is physically blocked, task is reassigned, and traffic reroutes around the failure.
8. **Phase 8: Central Controller Failure (DECENTRALIZED):** Kills Central Controller. Dashboard marks `OFFLINE` while robots continue peer-to-peer coordination uninterrupted.
9. **Phase 9: Controller Resynchronization:** Restores central controller and synchronizes fleet state.
10. **Phase 10: Operational Audit:** Validates zero mission loss and compiles real operational metrics.

---

## 7. Interactive Control Center
- **Simulation Speeds:** 1x, 2x, 5x speed multipliers.
- **Fleet Density:** Buttons for 10, 100, and 500 robots.
- **Workload Generator:** Quick `+25 TASKS` or `FLOOD 1,000 TASKS`.
- **Failure Injection Suite:**
  - *Create Collision:* Forces two robots onto opposing collision courses.
  - *Create Deadlock:* Forces circular gridlock.
  - *Fail Robot / Clear:* Disables drive motors, creating physical obstacles.
  - *Drain Battery:* Simulates urgent battery depletion.
  - *Block Corridor:* Places dynamic maintenance barriers.
  - *Kill / Restore Central Controller:* Toggles decentralized P2P failover.

---

## 8. Technology Stack
- **Platform:** Android Jetpack Compose (M3 Industrial Dark Theme)
- **Kinematics & Simulation:** Kotlin Coroutines (30 FPS discrete state loop)
- **Spatial Indexing:** Spatial Hash Grid ($O(1)$ neighbor queries)
- **Navigation:** A* Search with dynamic reservation penalties
- **Cycle Detection:** Directed Wait-For Graph (WFG) with Depth-First Search
- **Telemetry UI:** High-performance hardware-accelerated Compose Canvas with Pan & Pinch-to-Zoom gestures.
