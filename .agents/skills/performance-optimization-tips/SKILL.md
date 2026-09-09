---
name: performance-optimization-tips
description: >-
  Optimizes software performance across execution speed, memory footprint, and architectural scalability. Identifies CPU bottlenecks, inefficient algorithms, excessive allocations, memory leaks, and unnecessary UI rendering or recompositions, delivering benchmarkable, production-ready code. Use this skill when diagnosing slowness, high memory consumption, frame drops, or when profiling performance-critical code paths.
---

# Performance Optimization Tips

This skill provides an engineering framework to diagnose, analyze, and eliminate performance bottlenecks. It focuses on maximizing execution speed, minimizing memory consumption, and ensuring sustainable scalability while preserving code readability and correctness.

---

## Core Engineering Goals

1. **Speed (Execution Latency & Throughput)**:
   - Minimize CPU cycles and wall-clock execution time.
   - Prevent blocking operations on latency-sensitive threads (e.g., UI/Main thread).
   - Maximize parallelization and non-blocking asynchronous execution.

2. **Memory Usage (Allocations & Retention)**:
   - Reduce peak memory footprint and heap allocations.
   - Eliminate Garbage Collection (GC) pressure caused by short-lived allocations in hot paths.
   - Detect and resolve retained references, memory leaks, and unbounded collections.

3. **Scalability (Load & Volume Growth)**:
   - Ensure algorithmic complexity remains viable as data volume scales ($O(1)$ or $O(n \log n)$ over $O(n^2)$).
   - Implement pagination, lazy loading, and caching strategies.
   - Prevent cascading queries, unbounded buffers, and I/O bottlenecks.

---

## Detection Areas: What to Hunt For

### 1. Bottlenecks & Hot Paths
- Expensive computations executed repeatedly inside tight loops.
- Blocking I/O or synchronous disk/network calls on the main thread.
- Unindexed lookups, redundant sorting, and suboptimal collections (e.g., `List` linear search where a `Set` or `Map` provides $O(1)$).

### 2. Inefficient Logic & Resource Waste
- Redundant mapping, filtering, or transformations over multi-element collections.
- Premature or unnecessary object instantiation inside hot paths or event loops.
- Missing caching, memoization, or debounce/throttle mechanisms on high-frequency inputs.

### 3. Unnecessary Rendering & UI Thrashing
- **Recomposition / Re-render Cascades**: UI components recalculating or redrawing without state changes (unstable parameters, lambda allocations in composition).
- **Missing Memoization**: Expensive calculations running during UI composition instead of being cached with `remember`, `derivedStateOf`, or state hoisting.
- **Unbounded Lists**: Failure to recycle views or virtualize large collections (e.g., using columns/rows instead of lazy/virtualized layouts).

---

## Step-by-Step Optimization Workflow

1. **Profile & Isolate the Hotspot**:
   - Determine whether the bottleneck is CPU-bound, memory-bound, I/O-bound, or UI render-bound.
   - Quantify the baseline performance (execution time, allocation count, frame render time).

2. **Evaluate Invariants & Algorithmic Complexity**:
   - Audit time and space complexity ($O(N)$).
   - Check if work can be avoided entirely, cached, deferred, or calculated lazily.

3. **Eliminate Waste & Optimize Allocations**:
   - Reuse object instances, use primitive specializations, or pool allocations in hot loops.
   - Streamline data pipelines to perform single-pass operations.

4. **Isolate & Stabilize UI Rendering**:
   - Stabilize parameters and state flows to enable skipping re-renders/recompositions.
   - Isolate rapidly changing state to leaf nodes of the UI tree.

5. **Verify Correctness & Benchmark**:
   - Confirm that optimizations preserve business invariants, thread safety, and edge-case behaviors.
   - Compare before vs. after metrics to validate measurable improvement.

---

## Standard Output Format

When responding with this skill, structure your output strictly according to the following deliverables:

### 1. Performance Issues
- **Bottlenecks Identified**: Exact location and nature of CPU, I/O, or thread-blocking bottlenecks.
- **Inefficient Logic**: Analysis of redundant calculations, sub-optimal algorithmic complexity, or unneeded allocations.
- **Unnecessary Rendering**: Detailed breakdown of wasted UI renders, recompositions, or layout passes.

### 2. Optimization Strategies
- **Technical Rationale**: Clear architectural and algorithmic justification for each proposed change.
- **Expected Gains**: Projected impact on execution speed, memory footprint (GC reduction), and rendering stability.
- **Trade-off Analysis**: Evaluation of trade-offs between execution speed, memory usage, and code maintainability.

### 3. Improved Code
- **Production-Ready Implementation**: Refactored, benchmarkable, and idiomatic code incorporating all optimizations.
- **In-Code Documentation**: Targeted comments highlighting performance-critical patterns, memoization keys, or allocation-free paths.
- **Verification Guidelines**: Recommended profiling checks or benchmark assertions to measure the performance improvement.
