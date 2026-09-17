$root = 'C:\Users\Dell\workspace\fixna-localboost\backend\src\main\java'
$files = Get-ChildItem -Path $root -Recurse -Filter '*Controller.java'
foreach ($f in $files) {
    $rel = $f.FullName.Substring($root.Length + 1)
    Write-Output ("=== " + $rel)
    $lines = Select-String -Path $f.FullName -Pattern '@(RequestMapping|GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping)'
    foreach ($l in $lines) { Write-Output ("    " + $l.Line.Trim()) }
}