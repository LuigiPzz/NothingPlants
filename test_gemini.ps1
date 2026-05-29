$response = Invoke-RestMethod -Uri "https://generativelanguage.googleapis.com/v1beta/models?key=$env:GEMINI_API_KEY" -Method Get
$response.models | Where-Object { $_.name -like "*flash*" } | Select-Object name, supportedGenerationMethods | ConvertTo-Json -Depth 5
