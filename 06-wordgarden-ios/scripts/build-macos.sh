#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v xcodebuild >/dev/null || { echo 'ERROR: Build iOS requires macOS + Xcode.'; exit 1; }
if ! command -v xcodegen >/dev/null; then brew install xcodegen; fi
xcodebuild -version
xcodegen generate
swift test --package-path Core
xcodebuild -project WordGarden.xcodeproj -scheme WordGarden -configuration Debug \
  -destination 'generic/platform=iOS Simulator' -derivedDataPath build/Simulator \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO build > build-simulator.log 2>&1 || { tail -n 150 build-simulator.log; exit 1; }
xcodebuild -project WordGarden.xcodeproj -scheme WordGarden -configuration Release \
  -destination 'generic/platform=iOS' -archivePath build/WordGarden.xcarchive \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY='' \
  archive > build-device.log 2>&1 || { tail -n 150 build-device.log; exit 1; }
mkdir -p build/Package/Payload
ditto build/WordGarden.xcarchive/Products/Applications/WordGarden.app build/Package/Payload/WordGarden.app
test -d build/Package/Payload/WordGarden.app/PlugIns/WordGardenWidgets.appex
test -f build/Package/Payload/WordGarden.app/vocabulary.json
test -f build/Package/Payload/WordGarden.app/PlugIns/WordGardenWidgets.appex/vocabulary.json
(
  cd build/Package
  zip -qry ../WordGarden-unsigned.ipa Payload
)
ditto -c -k --sequesterRsrc --keepParent build/Simulator/Build/Products/Debug-iphonesimulator/WordGarden.app build/WordGarden-Simulator.zip
echo 'SUCCESS: Simulator build and unsigned physical-device IPA produced.'
echo 'The IPA is NOT directly installable until signed. Keep the widget extension when signing.'
