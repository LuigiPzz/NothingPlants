$response = Invoke-RestMethod -Uri "https://generativelanguage.googleapis.com/v1beta/models?key=AIzaSyACFDKP8jDPriyqnlj4Dz5xeqwX-pwL_6g" -Method Get
$response.models | Where-Object { $_.name -like "*flash*" } | Select-Object name, supportedGenerationMethods | ConvertTo-Json -Depth 5
