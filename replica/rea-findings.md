# Rea evidence ledger

Official rea-agents@6.8.0 CLI, executed with bundled Node 24.22.1. Full JSON retained in workspace work/rea-media-evidence.json and work/rea-media-boundaries.json. No assembly was loaded or executed by these commands.

Question: can the redesigned shell preserve Pocket Music's shipped managed Windows integration without another desktop runtime?

- Observed: admitted PE/CLI metadata, assembly PocketMusic.Media, implementation CIL and managed architecture AnyCPU. Evidence ev_ad5b37e6072d3aab0906f680e59d45e0f5a90103e24621d2927cc2709fea264e.
- Bounded observation: no P/Invoke import rows, native implementation rows, ReadyToRun marker or mixed/native-header indicator in this DLL. Evidence ev_3f8d0abd12e02830fac555367fb27c53bda22cdf910a28dd3379f0a851210070.
- Implementation decision: keep the managed bridge and change WPF presentation. Runtime regression tests separately validate the Windows controls.

Artifact: PocketMusic.Media.dll. SHA-256 5043ab82f9e97f7aa37466ad3fec57f6d5d20d1f76ac8523a502f46be0e6e09f. MVID 261974de-7957-4ff8-bb06-9c1b1b7f58dd.

Limitations: static inspection did not resolve CLR dependencies, decompile C#, execute code, verify native libraries/exports or inspect other DLLs. Zero rows in this DLL do not imply the whole app has no native calls. No Spotify binary/private implementation was analyzed. Spotify UX came from Replica's public-source observations. No persistent MCP registration/decompiler installation was needed.
