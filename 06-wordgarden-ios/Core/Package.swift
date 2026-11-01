// swift-tools-version: 5.9
import PackageDescription
let package = Package(name: "WordGardenCore", platforms: [.iOS(.v17), .macOS(.v13)], products: [.library(name: "WordGardenCore", targets: ["WordGardenCore"])], targets: [.target(name: "WordGardenCore"), .testTarget(name: "WordGardenCoreTests", dependencies: ["WordGardenCore"])])
