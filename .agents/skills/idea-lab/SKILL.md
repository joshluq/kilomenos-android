---
name: idea-lab
description: Product incubator, competitive benchmarking, and value hypothesis generator for KmSafe. Analyzes fleet and driver psychology, references industry-leading apps (Tesla, Fuelio, Drivvo, N26, Revolut), and scores feature opportunities via the ICE/RICE framework before code is written.
---
# Idea Lab -- Product Incubator & Competitive Benchmarking (KmSafe)

This command operates as a **product incubator and strategic innovation lab**. Its mission is to explore ideas, analyze market benchmarks, and design high-impact psychological and business solutions for drivers and fleet operators, **before writing a single line of code**.

It leverages the methodologies defined in po-digital-experience-fintech.

---

## When to Use This Command
- When exploring an abstract idea or user wish (e.g., "notify when a nearby gas station drops price", "gamify monthly mileage budget", "vacation mode").
- When benchmarking how leading automotive, telemetry, and Fintech apps solve a user friction point.
- When prioritizing product bets according to their expected impact on Retention (D30), Freemium conversion, or anxiety reduction.

---

## Step-by-Step Execution Protocol

When the user invokes /idea-lab [topic or idea]:

### 1. Problem Diagnosis & Driver Psychology
- **Control Anxiety**: What uncertainty or pain point does this idea alleviate? (Renting excess mileage penalties? Fuel/electricity price volatility? Forgetting to log a trip?).
- **Zero-Friction Principle**: How can the driver extract immediate value in under 5 seconds without tedious manual typing?

### 2. Competitive Benchmarks & Reference Patterns
- Analyze patterns from benchmark applications:
  - **Automotive & Telemetry**: Tesla app, Drivvo, Fuelio, Waze, Geotab.
  - **Fintech & Behavioral UX**: N26, Revolut, Cleo (cognitive decompression, immediate visual feedback, positive reinforcement streaks).
- Identify which superior architectural or UX pattern KmSafe should adopt or refine.

### 3. Value Proposition in KmSafe (Freemium Alignment)
- **Core / Free Tier**: What local-first functionality provides instant utility with zero server cost?
- **Premium Tier**: What advanced component justifies subscription conversion? (e.g., predictive volatility alerts, multi-vehicle cloud sync, unlimited audit history).

### 4. ICE Scoring & AARRR Metrics
Evaluate the idea across a rapid scorecard:
- **Impact (1-10)**: Effect on Activation, D30 Retention, or Premium Conversion.
- **Confidence (1-10)**: Certainty that the hypothesis will solve the core problem.
- **Ease (1-10)**: Technical implementation effort within KmSafe existing Clean Architecture.
- **North Star Telemetry Event**: Define the exact telemetry event and parameters to track post-launch.

### 5. Output Deliverable
Produce a concise executive brief structured as:
1. **The Opportunity**: 1 clear paragraph on user and business value.
2. **Key Benchmark Takeaways**: 2-3 lessons from competitor patterns.
3. **Proposed UI Mechanics**: How this maps into the 4-Layer Clean UI pattern.
4. **Next Steps**: Recommendation to promote the approved concept to /new-feature for technical specification.
