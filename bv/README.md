# Project Dashboard

## 📊 Executive Summary

**124** total issues | **73%** complete | **14** ready to work | **3** blocked

## 🎯 Top Priorities

The graph analysis identified these as the highest-impact items to work on:

### 1. Investigate Android AArch64 built-image FFI layout initialization failure
**ID:** `jolt-android-lfu.2.6.1` | **Impact Score:** 0.11

**Why this matters:**
- ✅ Currently unclaimed - available for work
- 🚨 High priority (P1) - prioritize this work

## 🚧 Critical Bottlenecks

These issues are blocking the most downstream work. Clearing them has outsized impact:

| Issue | Title | Unblocks | Status |
|-------|-------|----------|--------|
| `jolt-android-lfu.6.2` | Render the adaptive gallery shell and... | **5** issues | Ready |
| `jolt-android-lfu.2.3` | Drive the existing shared reducer fro... | **2** issues | Ready |
| `jolt-android-lfu.2.4` | Verify one shared Raylib application ... | **2** issues | Blocked by 1 |
| `jolt-android-jkb.2` | Provision native Linux GTK validation... | **1** issues | Ready |
| `jolt-android-lfu.2.7` | Load packaged assets and writable sta... | **1** issues | Blocked by 1 |

## 📈 Graph Analysis

- **Dependency Density:** 0.009 (🟢 Healthy) — Issues are well-isolated and can be parallelized
- **Graph Size:** 124 issues with 140 dependencies
- **Cycles:** None detected ✓

## 🏃 Quick Wins

Low-effort items that clear the path forward:

- **jolt-android-lfu.6.2**: Render the adaptive gallery shell and navigation (unblocks 5)
  - *Unblocks 5 items, high priority*
- **jolt-android-jkb.2**: Provision native Linux GTK validation host (unblocks 1)
  - *Unblocks 1 items*
- **jolt-android-lfu.3**: Raylib validation and feasibility assessment (unblocks 1)
  - *Unblocks 1 items*
- **jolt-android-lfu.2.3**: Drive the existing shared reducer from the Raylib host (unblocks 2)
  - *Unblocks 2 items*
- **jolt-android-lfu.2.4**: Verify one shared Raylib application on Linux and Android (unblocks 2)
  - *Unblocks 2 items*

## 📋 Status Summary

**By Priority:** P0: 1 | P1: 60 | P2: 56 | P3: 5 | P4: 2

**By Type:** bug: 5 | decision: 2 | epic: 19 | feature: 32 | task: 66

---

*Generated Aug 30, 2026 at 9:27 AM UTC by [bv](https://github.com/Dicklesworthstone/beads_viewer)*

