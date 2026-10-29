param([ValidateSet('mathdrill_dev','mathdrill_test')] [string]$Database='mathdrill_dev')
$ErrorActionPreference='Stop'
$ProjectRoot=Split-Path $PSScriptRoot -Parent
$PrivateRoot=Join-Path $ProjectRoot '.local'
$toolchain=Get-Content -LiteralPath (Join-Path $PrivateRoot 'toolchain.json') -Raw | ConvertFrom-Json
$runtime=Get-Content -LiteralPath (Join-Path $PrivateRoot 'runtime.json') -Raw | ConvertFrom-Json
$env:JAVA_HOME=$toolchain.javaHome
$env:ANDROID_HOME=$toolchain.sdkRoot
$env:Path="$env:JAVA_HOME/bin;$env:Path"
$env:MATHDRILL_DB_URL="jdbc:mysql://127.0.0.1:13306/${Database}?connectionTimeZone=UTC"
$env:MATHDRILL_DB_USER=$runtime.dbUser
$env:MATHDRILL_DB_PASSWORD=$runtime.dbPassword
$env:MATHDRILL_KEYSTORE="file:$PrivateRoot/dev-server.p12"
$env:MATHDRILL_KEYSTORE_PASSWORD=$runtime.keystorePassword
$env:MATHDRILL_SEED_PASSWORD=$runtime.seedPassword
$env:MATHDRILL_SEED_ENABLED='true'
$env:MATHDRILL_ADMIN_PASSWORD=$runtime.adminPassword
