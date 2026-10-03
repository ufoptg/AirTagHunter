# AirTagHunter fork

AirTagHunter is a derivative of [OffGridPete/Fieldwatch](https://github.com/OffGridPete/Fieldwatch) (MIT License), based on Fieldwatch 1.1.17.

Changes in this fork:

- Application id `app.airtaghunter` (installs beside Fieldwatch)
- Launcher / UI branding AirTagHunter
- New-install defaults favor BLE Finder tags (AirTags and similar) and watch FINDER-class signatures
- GitHub Actions builds and uploads `AirTagHunter.apk`
- Catalog 91 accuracy: AirTag match requires Find My OF type `0x12` + length `0x19` (not bare `0x12`); drop loose `Find My` name and Samsung company-id-only SmartTag hits; live **separated** / near-owner decode on AirTag OF status bit 2

Upstream Fieldwatch copyright, disclaimer text, and third-party notices in `LICENSE` / `NOTICE` remain. Prefer this repo’s `dist/fieldwatch-signatures-v2.json` for catalog updates so accuracy rules are not overwritten by upstream.
