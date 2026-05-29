package com.example.nothingplants.ai

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class PlantAnalysisResult(
    val score: Int,
    val notes: String,
    val daysToNext: Int
)

class PlantAnalyzer(private val apiKey: String) {

    val generativeModel = GenerativeModel(
        modelName = "gemini-3.5-flash",
        apiKey = apiKey
    )

    suspend fun analyzePlant(
        image: Bitmap?, 
        plantName: String, 
        previousIntervals: List<Int>,
        logType: String,
        fertilizerRule: String? = null,
        locationCity: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        lux: Float? = null,
        compassDirection: String? = null,
        timestamp: Long? = null
    ): PlantAnalysisResult? = withContext(Dispatchers.IO) {
        val intervalsText = if (previousIntervals.isEmpty()) {
            "Non ci sono eventi precedenti per calcolare un trend."
        } else {
            "Giorni trascorsi tra gli eventi precedenti: ${previousIntervals.joinToString(", ")}."
        }

        val eventTime = timestamp ?: System.currentTimeMillis()
        val formatter = java.text.SimpleDateFormat("dd MMMM yyyy 'alle ore' HH:mm", java.util.Locale("it", "IT"))
        val currentDate = formatter.format(java.util.Date(eventTime))

        val photoInstruction = if (image != null) "Analizza la foto allegata e le informazioni fornite" else "Basandoti SOLO sulle informazioni testuali fornite"

        val locationPrompt = if (!locationCity.isNullOrBlank()) {
            "L'utente si trova in questa località geografica: $locationCity (Latitudine: $latitude, Longitudine: $longitude).\n" +
            "Considera le caratteristiche climatiche reali di questa località (es. temperatura, umidità, stagionalità tipica della zona) per stimare accuratamente la salute e, soprattutto, i giorni trascorsi o da attendere prima della prossima cura ($logType)."
        } else {
            ""
        }

        val sensorPrompt = if (lux != null || !compassDirection.isNullOrBlank()) {
            val luxStr = if (lux != null) "$lux Lux" else "Non rilevato"
            val directionStr = if (!compassDirection.isNullOrBlank()) compassDirection else "Non rilevata"
            "\nDati dei sensori fisici ambientali misurati al momento dello scatto ($currentDate):\n" +
            "- Luminosità ambiente rilevata: $luxStr\n" +
            "- Orientamento esposizione/finestra della stanza: $directionStr\n" +
            "Usa questi dati per calcolare meglio lo stato di salute e i giorni proposti. Ad esempio, confronta l'orario dello scatto con il livello Lux: una lettura Lux molto bassa durante le ore diurne indica scarsa illuminazione (potenziale problema), mentre di sera o notte è del tutto normale. Includi questa valutazione nelle note e adegua la stima."
        } else {
            ""
        }

        val prompt = """
            Sei un botanico esperto. Oggi è il $currentDate.
            L'utente ha appena registrato un evento di tipo: $logType (WATERING = Annaffiatura, FERTILIZING = Concimatura) per una pianta di $plantName.
            $photoInstruction per restituire:
            1. Un punteggio di salute da 1 a 10 (se non c'è foto, metti 0).
            2. Una brevissima nota sullo stato della pianta (max 10 parole, se non c'è foto scrivi "Foto non fornita").
            3. Il numero di giorni stimato da attendere prima del PROSSIMO evento dello stesso tipo ($logType).
               - Se WATERING: basati sulla specie e sul trend: $intervalsText
               - Se FERTILIZING: basati sulla data odierna e sulla seguente regola ideale (se presente): "$fertilizerRule". Se siamo fuori dal periodo utile (es. regola dice primavera ed è inverno), calcola i giorni che mancano all'inizio del prossimo periodo utile.
            
            $locationPrompt
            
            $sensorPrompt
            
            Restituisci ESATTAMENTE e SOLO questo formato JSON (nessun altro testo):
            {
               "score": 8,
               "notes": "Foglie verdi in ottima salute",
               "days_to_next": 5
            }
        """.trimIndent()

        val inputContent = content {
            if (image != null) {
                image(image)
            }
            text(prompt)
        }

        val response = generativeModel.generateContent(inputContent)
        val rawText = response.text ?: return@withContext null
        val startIndex = rawText.indexOf('{')
        val endIndex = rawText.lastIndexOf('}')
        if (startIndex == -1 || endIndex == -1 || endIndex < startIndex) return@withContext null
        val jsonText = rawText.substring(startIndex, endIndex + 1).trim()
        
        val jsonObject = JSONObject(jsonText)
        val score = jsonObject.getInt("score")
        val notes = jsonObject.getString("notes")
        val daysToNext = jsonObject.getInt("days_to_next")
        
        PlantAnalysisResult(score, notes, daysToNext)
    }

    suspend fun generatePlantSummary(
        species: String, 
        image: Bitmap? = null,
        locationCity: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        isMix: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        val locationPrompt = if (!locationCity.isNullOrBlank()) {
            "L'utente si trova in questa località geografica: $locationCity (Latitudine: $latitude, Longitudine: $longitude).\n" +
            "Adatta le istruzioni di luce (light), annaffiatura (water), terriccio (soil) e concime (fertilizer) considerando le caratteristiche climatiche tipiche di questa località (es. insolazione media, temperatura, umidità dell'aria)."
        } else {
            ""
        }

        val mixPrompt = if (isMix) {
            "\nATTENZIONE: Questa scheda di cura è per un MIX di piante diverse ($species) coltivate nello stesso vaso. " +
            "Genera le istruzioni botaniche (luce, annaffiatura, terriccio, concime) che rappresentino una MEDIA/COMPROMESSO ottimale affinché queste specie possano coesistere felicemente nello stesso vaso.\n" +
            "Inoltre, valuta attentamente la compatibilità biologica di queste piante per la coesistenza nello stesso vaso. Se le piante hanno esigenze incompatibili (es. cactus con piante tropicali umide, o esigenze di luce totalmente opposte), assegna al campo 'incompatibleWarning' un messaggio d'avviso chiaro in italiano in testa che descriva l'incompatibilità biologica e sconsigli la coesistenza nello stesso vaso. Se sono compatibili o se non si tratta di un mix incompatibile, lascia il campo 'incompatibleWarning' vuoto (null o stringa vuota)."
        } else {
            ""
        }

        val prompt = """
            Sei un botanico esperto. Scrivi una sintesi concisa in italiano su questa specie di pianta: $species.
            
            $locationPrompt
            $mixPrompt
            
            Restituisci ESATTAMENTE e SOLO questo formato JSON (nessun blocco markdown ```json):
            {
               "info": "Descrizione generale della pianta, caratteristiche e curiosità. (Discorsivo ma elegante).",
               "light": "Necessità di luce",
               "water": "Necessità di acqua, indicando chiaramente circa ogni quanto innaffiare (la frequenza stimata in giorni, es. in estate e in inverno o in generale).",
               "soil": "Tipo di terreno ideale",
               "fertilizer": "Tipo di fertilizzante, ogni quanto va dato e in che periodo",
               "incompatibleWarning": "Avviso di incompatibilità se presente, altrimenti stringa vuota o null"
            }
            Sii schematico, breve e dritto al punto per i 4 parametri tecnici (specificando chiaramente la frequenza in giorni in 'water'), mantenendo un tono elegante per le info.
        """.trimIndent()

        val inputContent = content {
            if (image != null) {
                image(image)
            }
            text(prompt)
        }

        val response = generativeModel.generateContent(inputContent)
        val rawText = response.text ?: return@withContext null
        val startIndex = rawText.indexOf('{')
        val endIndex = rawText.lastIndexOf('}')
        if (startIndex == -1 || endIndex == -1 || endIndex < startIndex) return@withContext null
        rawText.substring(startIndex, endIndex + 1).trim()
    }

    suspend fun identifyPlantSpecies(image: Bitmap): String? = withContext(Dispatchers.IO) {
        val prompt = """
            Sei un botanico esperto. Analizza la foto allegata e identifica la specie della pianta o delle piante se è presente un mix nello stesso vaso.
            Determina se l'immagine ritrae un mix di piante diverse. 
            Determina il numero esatto di piante distinte presenti nella foto.
            Fornisci la specie botanica scientifica (es. "Sansevieria trifasciata", o unione di specie separate da virgola se mix) e il nome comune in italiano (es. "Lingua di suocera", o descrizione del mix in italiano).
            
            Restituisci ESATTAMENTE e SOLO questo formato JSON (senza tag markdown ```json):
            {
               "species": "Nome scientifico della specie",
               "commonName": "Nome comune in italiano",
               "isMix": true/false (true se ci sono più piante distinte o specie diverse, false altrimenti),
               "plantCount": numero_piante_rilevate (intero, es. 1 se singola pianta, 2, 3 ecc. se mix)
            }
        """.trimIndent()

        val inputContent = content {
            image(image)
            text(prompt)
        }

        val response = generativeModel.generateContent(inputContent)
        val rawText = response.text ?: return@withContext null
        val startIndex = rawText.indexOf('{')
        val endIndex = rawText.lastIndexOf('}')
        if (startIndex == -1 || endIndex == -1 || endIndex < startIndex) return@withContext null
        rawText.substring(startIndex, endIndex + 1).trim()
    }

    fun extractDaysFromText(text: String, isWater: Boolean): Int {
        val cleanText = text.lowercase(java.util.Locale.getDefault())
        
        // Cerca espressioni con "giorni"
        val giorniRegex = Regex("""(?:ogni|circa)\s+(\d+)(?:\s*-\s*(\d+))?\s+giorn[oi]""")
        giorniRegex.find(cleanText)?.let { match ->
            val start = match.groupValues[1].toIntOrNull()
            val end = match.groupValues[2].takeIf { it.isNotEmpty() }?.toIntOrNull()
            if (start != null) {
                return if (end != null) (start + end) / 2 else start
            }
        }
        
        // Cerca espressioni con "settiman"
        if (cleanText.contains("ogni settimana") || cleanText.contains("una volta a settimana") || cleanText.contains("1 volta a settimana")) {
            return 7
        }
        if (cleanText.contains("due volte a settimana") || cleanText.contains("2 volte a settimana") || cleanText.contains("bisettimanale")) {
            return 3
        }
        val settimaneRegex = Regex("""ogni\s+(\d+)\s+settiman[e]""")
        settimaneRegex.find(cleanText)?.let { match ->
            val weeks = match.groupValues[1].toIntOrNull()
            if (weeks != null) return weeks * 7
        }
        
        // Cerca espressioni con "mes"
        if (cleanText.contains("ogni mese") || cleanText.contains("una volta al mese") || cleanText.contains("1 volta al mese")) {
            return 30
        }
        val mesiRegex = Regex("""ogni\s+(\d+)\s+mes[ei]""")
        mesiRegex.find(cleanText)?.let { match ->
            val months = match.groupValues[1].toIntOrNull()
            if (months != null) return months * 30
        }
        
        // Cerca qualsiasi numero isolato
        val numberRegex = Regex("""\b(\d+)\b""")
        val numbers = numberRegex.findAll(cleanText).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
        if (numbers.isNotEmpty()) {
            if (isWater) {
                val valid = numbers.filter { it in 1..30 }
                if (valid.isNotEmpty()) return valid.first()
            } else {
                val valid = numbers.filter { it in 7..180 }
                if (valid.isNotEmpty()) return valid.first()
            }
        }

        return if (isWater) 7 else 30
    }

    suspend fun parseIntervalsBatch(
        plantsData: List<PlantBatchData>,
        locationCity: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): Map<Long, Pair<Int, Int>>? = withContext(Dispatchers.IO) {
        if (plantsData.isEmpty()) return@withContext emptyMap()

        val formatter = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("it", "IT"))
        val currentDate = formatter.format(java.util.Date())
        
        val calendar = java.util.Calendar.getInstance()
        val month = calendar.get(java.util.Calendar.MONTH) // 0-11
        val season = when (month) {
            java.util.Calendar.DECEMBER, java.util.Calendar.JANUARY, java.util.Calendar.FEBRUARY -> "Inverno"
            java.util.Calendar.MARCH, java.util.Calendar.APRIL, java.util.Calendar.MAY -> "Primavera"
            java.util.Calendar.JUNE, java.util.Calendar.JULY, java.util.Calendar.AUGUST -> "Estate"
            else -> "Autunno"
        }

        val plantsJsonArray = org.json.JSONArray()
        plantsData.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("water_text", p.waterText)
            obj.put("fertilizer_text", p.fertilizerText)
            plantsJsonArray.put(obj)
        }

        val locationPrompt = if (!locationCity.isNullOrBlank()) {
            "L'utente si trova in questa località geografica: $locationCity (Latitudine: $latitude, Longitudine: $longitude).\n" +
            "Quando estrai la frequenza di annaffiatura e concimazione, adatta la stima al clima reale di questa località geografica per la stagione corrente ($season)."
        } else {
            ""
        }

        val prompt = """
            Sei un botanico esperto. Oggi è il $currentDate (stagione: $season).
            Ti viene fornita una lista di piante in formato JSON, ciascuna con le proprie descrizioni testuale per l'annaffiatura (water_text) e per la concimazione (fertilizer_text).
            
            $locationPrompt
            
            Analizza il testo di ciascuna pianta ed estrai l'intervallo ideale in giorni per la prossima annaffiatura (water_days) e concimazione (fertilizer_days) appropriato per il periodo dell'anno corrente ($season).
            
            Regole di stima:
            - Se il testo descrive una frequenza in estate e una in inverno, e oggi siamo in $season, calcola la frequenza coerente con $season.
            - Se il fertilizzante dice di sospendere o non darlo in questo periodo ($season), imposta fertilizer_days a 0 (indica sospensione).
            - Restituisci per ogni pianta un valore numerico di giorni intero.
            
            Restituisci ESATTAMENTE e SOLO un array JSON (nessun blocco markdown o altro testo, nessun ```json):
            [
              {
                "id": 1,
                "water_days": 7,
                "fertilizer_days": 30
              }
            ]
            
            Dati delle piante:
            ${plantsJsonArray.toString()}
        """.trimIndent()

        try {
            val response = generativeModel.generateContent(prompt)
            val rawText = response.text ?: return@withContext null
            val startIndex = rawText.indexOf('[')
            val endIndex = rawText.lastIndexOf(']')
            if (startIndex == -1 || endIndex == -1 || endIndex < startIndex) return@withContext null
            val jsonText = rawText.substring(startIndex, endIndex + 1).trim()
            
            val jsonArray = org.json.JSONArray(jsonText)
            val resultMap = mutableMapOf<Long, Pair<Int, Int>>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getLong("id")
                val waterDays = obj.getInt("water_days")
                val fertilizerDays = obj.getInt("fertilizer_days")
                resultMap[id] = Pair(waterDays, fertilizerDays)
            }
            resultMap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun analyzeGrowthTrend(
        species: String,
        images: List<Bitmap>,
        dates: List<String>
    ): String? = withContext(Dispatchers.IO) {
        if (images.isEmpty()) return@withContext "Nessuna foto disponibile per il diario di crescita."
        
        val formatter = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("it", "IT"))
        val currentDate = formatter.format(java.util.Date())

        val prompt = """
            Sei un botanico esperto di alto livello. Oggi è il $currentDate.
            Ti vengono fornite in input ${images.size} foto storiche dello sviluppo temporale di una pianta di specie: $species.
            Le foto sono ordinate cronologicamente e scattate in queste date: ${dates.joinToString(", ")}.
            
            Analizza attentamente la sequenza di immagini fornite (dal primo scatto all'ultimo in ordine cronologico) per valutare l'andamento della pianta nel tempo:
            1. **Crescita e Sviluppo**: Descrivi se la pianta mostra un aumento sano del volume fogliare, nuovi germogli, fioriture o se la crescita è ferma/stagnante.
            2. **Evoluzione della Salute**: Confronta lo stato di salute generale. Segnala se ci sono segni di ripresa (es. foglie gialle che spariscono, fusti più eretti) o se si nota un declino progressivo (es. nuove secchezze, perdita di vigore).
            3. **Consigli specifici futuri**: Fornisci 2-3 raccomandazioni mirate basandoti su questa evoluzione storica per migliorare le condizioni di luce, acqua o concime.
            
            Restituisci un resoconto ben formattato in ITALIANO, con uno stile elegante, discorsivo ma conciso, e strutturato con titoli chiari in maiuscolo. Evita blocchi markdown ```json o altro, restituisci solo il testo formattato.
        """.trimIndent()

        try {
            val inputContent = content {
                images.forEach { bitmap ->
                    image(bitmap)
                }
                text(prompt)
            }
            val response = generativeModel.generateContent(inputContent)
            response.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun analyzePhotoHealth(
        images: List<Bitmap>,
        speciesName: String? = null
    ): String? = withContext(Dispatchers.IO) {
        val speciesPrompt = if (!speciesName.isNullOrBlank()) " di specie $speciesName" else ""
        val prompt = """
            Sei un botanico esperto di altissimo livello. 
            Analizza attentamente le ${images.size} foto allegate raffiguranti una pianta$speciesPrompt.
            Fornisci un report diagnostico di salute estremamente dettagliato, organizzato nei seguenti punti:
            
            1. **STATO DI SALUTE GENERALE**: Valuta lo stato visivo (foglie, fusti, colore) e assegna un voto sintetico da 1 a 10.
            2. **CRITICITÀ RILEVATE**: Identifica eventuali anomalie visibili (macchie fogliari, secchezza, parassiti, marciumi, eziolatura/carenza di luce, scottature solari, accumulo di sali).
            3. **ANALISI DEI FATTORI AMBIENTALI**: Deduci, per quanto possibile dall'aspetto della pianta e del terreno visibile, se ci sono stati eccessi o carenze di acqua, luce inadeguata o vasi inadatti.
            4. **RACCOMANDAZIONI DI CURA IMMEDIATA**: Elenca passaggi pratici e dettagliati da compiere per ripristinare o migliorare il benessere della pianta (es. potatura di foglie rovinate, riposizionamento, frequenza e modalità di innaffiatura, concimazione consigliata).
            
            Restituisci il report in ITALIANO, formattato in modo elegante e leggibile in markdown. Usa titoli chiari in maiuscolo. Sii estremamente specifico ed evita risposte generiche.
        """.trimIndent()

        try {
            val inputContent = content {
                images.forEach { image ->
                    image(image)
                }
                text(prompt)
            }
            val response = generativeModel.generateContent(inputContent)
            response.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}


data class PlantBatchData(
    val id: Long,
    val name: String,
    val waterText: String,
    val fertilizerText: String
)

