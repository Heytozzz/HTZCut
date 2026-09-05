// IMPORTANT: this module must NEVER depend on net.minecraft.* or net.neoforged.*
// It contains only game-agnostic logic (triggers, config parsing, audio routing
// contracts, permission contracts) so it can be reused when porting to other
// Minecraft versions or loaders without rewriting the business logic.

dependencies {
    implementation("org.yaml:snakeyaml:2.2")
}
