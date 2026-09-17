$p = 'C:\Users\Dell\workspace\fixna-localboost\docs\09-postman\fixna-localboost-api-v1.postman_collection.json'
Select-String -Path $p -Pattern '^      "name"|__CHUNK_MARKER|__SENTINEL__' | ForEach-Object {
    Write-Output ("{0}: {1}" -f $_.LineNumber, $_.Line.Trim())
}