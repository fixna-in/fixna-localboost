$root = 'C:\Users\Dell\workspace\fixna-localboost\backend\src\main\java'
Get-ChildItem -Path $root -Recurse -Filter '*Controller.java' | Sort-Object FullName | ForEach-Object {
    Write-Output ('=== ' + $_.Name)
    Select-String -Path $_.FullName -Pattern '@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@PatchMapping|@DeleteMapping|@Tag' |
        ForEach-Object { Write-Output ('    ' + $_.Line.Trim()) }
}