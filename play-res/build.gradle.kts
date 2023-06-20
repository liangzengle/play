dependencies {
  api(project(":play-core"))
  api(libs.hebernate.validator.asProvider())
  implementation(libs.bundles.el)
}
