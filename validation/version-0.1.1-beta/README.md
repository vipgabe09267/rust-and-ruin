# Rust & Ruin 0.1.1-beta verification

This rebuild corrects the release number to **0.1.1-beta**. The offline build, package audit and resource audit passed.

An entry-by-entry comparison with the tested 0.1.0-beta expansion found only one changed entry: `fabric.mod.json`, with its `version` changed to `0.1.1-beta`. All **267 compiled classes** and every other archive entry are byte-identical. The prior 399-test and graphical receipts remain the original gameplay baseline; they were not rewritten or rerun for this metadata correction.

- [Exact artifact comparison and SHA-256](verification.json)
- [Build log](build.log)
- [Package audit](package.json)
- [Resource audit](resources.json)
- [Original gameplay validation](../expansion-2026-11-14/README.md)

Distributable: `build/libs/rust-and-ruin-0.1.1-beta.jar`, **3,501,095 bytes**, SHA-256 `957F54966E70FFA9B3B27136D337EBF3D9A1E6551DF112D13DA39A7BC808BE2A`.
