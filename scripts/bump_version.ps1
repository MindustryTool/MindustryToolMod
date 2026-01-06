param (
    [ValidateSet("patch", "minor", "major")]
    [string]$Type = "patch"
)

$propFile = "$PSScriptRoot/../gradle.properties"
$content = Get-Content $propFile -Raw

if ($content -match "modVersion=(\d+)\.(\d+)\.(\d+)") {
    $major = [int]$matches[1]
    $minor = [int]$matches[2]
    $patch = [int]$matches[3]

    switch ($Type) {
        "patch" { $patch++ }
        "minor" { $minor++; $patch = 0 }
        "major" { $major++; $minor = 0; $patch = 0 }
    }

    $newVersion = "$major.$minor.$patch"
    $newContent = $content -replace "modVersion=\d+\.\d+\.\d+", "modVersion=$newVersion"
    Set-Content $propFile $newContent

    Write-Host "Version bumped to $newVersion" -ForegroundColor Green

    # Auto-commit
    git add $propFile
    git commit -m "chore: bump version to $newVersion"
    Write-Host "Committed change to git." -ForegroundColor Cyan
    Write-Host "Run 'git push origin main' to release!" -ForegroundColor Yellow
} else {
    Write-Error "Could not find modVersion in gradle.properties"
}
