package com.example.data

import org.json.JSONArray
import org.json.JSONObject

data class CountryDialOption(
    val dialCode: String,
    val label: String,
    val countryName: String
) {
    val flagEmoji: String
        get() = label.substringBefore(" ").ifBlank { "🌐" }

    val displayDropdownLabel: String
        get() = "$flagEmoji $countryName ($dialCode)"
}

data class Branch(
    val id: String,
    val name: String,
    val shortCode: String,
    val city: String,
    val subtitle: String,
    val defaultDepartments: List<String>,
    val countryCode: String = "+971",
    val facilityLocation: String = "",
    val logoUri: String? = null,
    val safetyGuidelines: String = DEFAULT_SAFETY_GUIDELINES,
    val safetyHelpline: String = DEFAULT_SAFETY_HELPLINE,
    val safetyContactName: String = DEFAULT_SAFETY_CONTACT_NAME,
    val emergencyExitDrawingUri: String? = null
) {
    val displayFacilityLocation: String
        get() = facilityLocation.trim().ifBlank {
            if (subtitle.isNotBlank() && city.isNotBlank()) "$subtitle, $city"
            else subtitle.ifBlank { city }
        }

    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("shortCode", shortCode)
        json.put("city", city)
        json.put("subtitle", subtitle)
        json.put("countryCode", countryCode)
        json.put("facilityLocation", facilityLocation)
        val deptArray = JSONArray()
        defaultDepartments.forEach { deptArray.put(it) }
        json.put("departments", deptArray)
        logoUri?.let { json.put("logoUri", it) }
        json.put("safetyGuidelines", safetyGuidelines)
        json.put("safetyHelpline", safetyHelpline)
        json.put("safetyContactName", safetyContactName)
        emergencyExitDrawingUri?.let { json.put("emergencyExitDrawingUri", it) }
        return json
    }

    companion object {
        const val DEFAULT_SAFETY_GUIDELINES =
            "1. Wear mandatory PPE (Safety Helmet, High-Vis Vest, Steel-Toe Safety Shoes) in all workshops, yards, and operational zones.\n" +
            "2. Stay strictly within designated yellow pedestrian walkways at all times.\n" +
            "3. Smoking is strictly prohibited except in designated external smoking shelters.\n" +
            "4. Photography and unauthorized video recording are strictly prohibited inside the premises.\n" +
            "5. In the event of a fire alarm or emergency, cease all work and proceed calmly to the nearest designated Emergency Assembly Point.\n" +
            "6. Always remain accompanied by your designated host employee."

        const val DEFAULT_SAFETY_HELPLINE = "+971 4 883 0000 / Security Control Ext. 101 / Emergency: 999"
        const val DEFAULT_SAFETY_CONTACT_NAME = "QHSE Officer / Security Desk"

        fun fromJsonObject(obj: JSONObject): Branch {
            val id = obj.optString("id", "")
            val name = obj.optString("name", "")
            val shortCode = obj.optString("shortCode", "")
            val city = obj.optString("city", "")
            val subtitle = obj.optString("subtitle", "")
            val countryCode = obj.optString("countryCode", "").ifBlank {
                BranchRegistry.inferCountryCode(city, name)
            }
            val facilityLocation = obj.optString("facilityLocation", "").ifBlank {
                if (subtitle.isNotBlank() && city.isNotBlank()) "$subtitle, $city" else subtitle
            }
            val deptList = mutableListOf<String>()
            val deptArray = obj.optJSONArray("departments")
            if (deptArray != null) {
                for (i in 0 until deptArray.length()) {
                    deptList.add(deptArray.getString(i))
                }
            }
            val logoUri = if (obj.has("logoUri") && !obj.isNull("logoUri")) obj.optString("logoUri").takeIf { it.isNotBlank() } else null
            val safetyGuidelines = obj.optString("safetyGuidelines", DEFAULT_SAFETY_GUIDELINES).ifBlank { DEFAULT_SAFETY_GUIDELINES }
            val safetyHelpline = obj.optString("safetyHelpline", DEFAULT_SAFETY_HELPLINE).ifBlank { DEFAULT_SAFETY_HELPLINE }
            val safetyContactName = obj.optString("safetyContactName", DEFAULT_SAFETY_CONTACT_NAME)
            val emergencyExit = if (obj.has("emergencyExitDrawingUri") && !obj.isNull("emergencyExitDrawingUri")) obj.optString("emergencyExitDrawingUri").takeIf { it.isNotBlank() } else null

            return Branch(
                id = id.ifBlank { name.lowercase().replace(" ", "_").replace("/", "_") },
                name = name,
                shortCode = shortCode,
                city = city,
                subtitle = subtitle,
                defaultDepartments = if (deptList.isNotEmpty()) deptList else listOf("PROJECTS", "QHSE", "MANAGEMENT"),
                countryCode = countryCode,
                facilityLocation = facilityLocation,
                logoUri = logoUri,
                safetyGuidelines = safetyGuidelines,
                safetyHelpline = safetyHelpline,
                safetyContactName = safetyContactName,
                emergencyExitDrawingUri = emergencyExit
            )
        }
    }
}

object BranchRegistry {
    val DEFAULT_VISIT_PURPOSES = listOf(
        "Meeting",
        "Inspection",
        "Resume Submission",
        "Interview",
        "Factory Visit",
        "Material Delivery",
        "Other"
    )

    val COUNTRY_DIAL_OPTIONS: List<CountryDialOption> = listOf(
        // Priority countries at the top of the country selector
        CountryDialOption("+971", "🇦🇪 UAE", "United Arab Emirates"),
        CountryDialOption("+966", "🇸🇦 KSA", "Saudi Arabia"),
        CountryDialOption("+974", "🇶🇦 Qatar", "Qatar"),
        CountryDialOption("+965", "🇰🇼 Kuwait", "Kuwait"),
        CountryDialOption("+968", "🇴🇲 Oman", "Oman"),
        CountryDialOption("+973", "🇧🇭 Bahrain", "Bahrain"),
        CountryDialOption("+91", "🇮🇳 India", "India"),
        CountryDialOption("+32", "🇧🇪 Belgium", "Belgium"),
        CountryDialOption("+44", "🇬🇧 UK", "United Kingdom"),
        CountryDialOption("+213", "🇩🇿 Algeria", "Algeria"),
        CountryDialOption("+1", "🇨🇦 Canada", "Canada"),
        CountryDialOption("+1", "🇺🇸 USA", "United States"),
        // All other countries alphabetically
        CountryDialOption("+93", "🇦🇫 Afghanistan", "Afghanistan"),
        CountryDialOption("+355", "🇦🇱 Albania", "Albania"),
        CountryDialOption("+376", "🇦🇩 Andorra", "Andorra"),
        CountryDialOption("+244", "🇦🇴 Angola", "Angola"),
        CountryDialOption("+54", "🇦🇷 Argentina", "Argentina"),
        CountryDialOption("+374", "🇦🇲 Armenia", "Armenia"),
        CountryDialOption("+61", "🇦🇺 Australia", "Australia"),
        CountryDialOption("+43", "🇦🇹 Austria", "Austria"),
        CountryDialOption("+994", "🇦🇿 Azerbaijan", "Azerbaijan"),
        CountryDialOption("+1-242", "🇧🇸 Bahamas", "Bahamas"),
        CountryDialOption("+880", "🇧🇩 Bangladesh", "Bangladesh"),
        CountryDialOption("+375", "🇧🇾 Belarus", "Belarus"),
        CountryDialOption("+501", "🇧🇿 Belize", "Belize"),
        CountryDialOption("+229", "🇧🇯 Benin", "Benin"),
        CountryDialOption("+975", "🇧🇹 Bhutan", "Bhutan"),
        CountryDialOption("+591", "🇧🇴 Bolivia", "Bolivia"),
        CountryDialOption("+387", "🇧🇦 Bosnia", "Bosnia and Herzegovina"),
        CountryDialOption("+267", "🇧🇼 Botswana", "Botswana"),
        CountryDialOption("+55", "🇧🇷 Brazil", "Brazil"),
        CountryDialOption("+673", "🇧🇳 Brunei", "Brunei"),
        CountryDialOption("+359", "🇧🇬 Bulgaria", "Bulgaria"),
        CountryDialOption("+226", "🇧🇫 Burkina Faso", "Burkina Faso"),
        CountryDialOption("+257", "🇧🇮 Burundi", "Burundi"),
        CountryDialOption("+855", "🇰🇭 Cambodia", "Cambodia"),
        CountryDialOption("+237", "🇨🇲 Cameroon", "Cameroon"),
        CountryDialOption("+238", "🇨🇻 Cape Verde", "Cape Verde"),
        CountryDialOption("+236", "🇨🇫 Central African Rep.", "Central African Republic"),
        CountryDialOption("+235", "🇹🇩 Chad", "Chad"),
        CountryDialOption("+56", "🇨🇱 Chile", "Chile"),
        CountryDialOption("+86", "🇨🇳 China", "China"),
        CountryDialOption("+57", "🇨🇴 Colombia", "Colombia"),
        CountryDialOption("+269", "🇰🇲 Comoros", "Comoros"),
        CountryDialOption("+242", "🇨🇬 Congo", "Congo"),
        CountryDialOption("+506", "🇨🇷 Costa Rica", "Costa Rica"),
        CountryDialOption("+385", "🇭🇷 Croatia", "Croatia"),
        CountryDialOption("+53", "🇨🇺 Cuba", "Cuba"),
        CountryDialOption("+357", "🇨🇾 Cyprus", "Cyprus"),
        CountryDialOption("+420", "🇨🇿 Czechia", "Czech Republic"),
        CountryDialOption("+45", "🇩🇰 Denmark", "Denmark"),
        CountryDialOption("+253", "🇩🇯 Djibouti", "Djibouti"),
        CountryDialOption("+1-809", "🇩🇴 Dominican Rep.", "Dominican Republic"),
        CountryDialOption("+593", "🇪🇨 Ecuador", "Ecuador"),
        CountryDialOption("+20", "🇪🇬 Egypt", "Egypt"),
        CountryDialOption("+503", "🇸🇻 El Salvador", "El Salvador"),
        CountryDialOption("+240", "🇬🇶 Equatorial Guinea", "Equatorial Guinea"),
        CountryDialOption("+291", "🇪🇷 Eritrea", "Eritrea"),
        CountryDialOption("+372", "🇪🇪 Estonia", "Estonia"),
        CountryDialOption("+251", "🇪🇹 Ethiopia", "Ethiopia"),
        CountryDialOption("+679", "🇫🇯 Fiji", "Fiji"),
        CountryDialOption("+358", "🇫🇮 Finland", "Finland"),
        CountryDialOption("+33", "🇫🇷 France", "France"),
        CountryDialOption("+241", "🇬🇦 Gabon", "Gabon"),
        CountryDialOption("+220", "🇬🇲 Gambia", "Gambia"),
        CountryDialOption("+995", "🇬🇪 Georgia", "Georgia"),
        CountryDialOption("+49", "🇩🇪 Germany", "Germany"),
        CountryDialOption("+233", "🇬🇭 Ghana", "Ghana"),
        CountryDialOption("+30", "🇬🇷 Greece", "Greece"),
        CountryDialOption("+502", "🇬🇹 Guatemala", "Guatemala"),
        CountryDialOption("+224", "🇬🇳 Guinea", "Guinea"),
        CountryDialOption("+592", "🇬🇾 Guyana", "Guyana"),
        CountryDialOption("+509", "🇭🇹 Haiti", "Haiti"),
        CountryDialOption("+504", "🇭🇳 Honduras", "Honduras"),
        CountryDialOption("+852", "🇭🇰 Hong Kong", "Hong Kong"),
        CountryDialOption("+36", "🇭🇺 Hungary", "Hungary"),
        CountryDialOption("+354", "🇮🇸 Iceland", "Iceland"),
        CountryDialOption("+62", "🇮🇩 Indonesia", "Indonesia"),
        CountryDialOption("+98", "🇮🇷 Iran", "Iran"),
        CountryDialOption("+964", "🇮🇶 Iraq", "Iraq"),
        CountryDialOption("+353", "🇮🇪 Ireland", "Ireland"),
        CountryDialOption("+39", "🇮🇹 Italy", "Italy"),
        CountryDialOption("+225", "🇨🇮 Ivory Coast", "Ivory Coast"),
        CountryDialOption("+1-876", "🇯🇲 Jamaica", "Jamaica"),
        CountryDialOption("+81", "🇯🇵 Japan", "Japan"),
        CountryDialOption("+962", "🇯🇴 Jordan", "Jordan"),
        CountryDialOption("+7", "🇰🇿 Kazakhstan", "Kazakhstan"),
        CountryDialOption("+254", "🇰🇪 Kenya", "Kenya"),
        CountryDialOption("+82", "🇰🇷 South Korea", "South Korea"),
        CountryDialOption("+996", "🇰🇬 Kyrgyzstan", "Kyrgyzstan"),
        CountryDialOption("+856", "🇱🇦 Laos", "Laos"),
        CountryDialOption("+371", "🇱🇻 Latvia", "Latvia"),
        CountryDialOption("+961", "🇱🇧 Lebanon", "Lebanon"),
        CountryDialOption("+266", "🇱🇸 Lesotho", "Lesotho"),
        CountryDialOption("+231", "🇱🇷 Liberia", "Liberia"),
        CountryDialOption("+218", "🇱🇾 Libya", "Libya"),
        CountryDialOption("+423", "🇱🇮 Liechtenstein", "Liechtenstein"),
        CountryDialOption("+370", "🇱🇹 Lithuania", "Lithuania"),
        CountryDialOption("+352", "🇱🇺 Luxembourg", "Luxembourg"),
        CountryDialOption("+853", "🇲🇴 Macau", "Macau"),
        CountryDialOption("+261", "🇲🇬 Madagascar", "Madagascar"),
        CountryDialOption("+265", "🇲🇼 Malawi", "Malawi"),
        CountryDialOption("+60", "🇲🇾 Malaysia", "Malaysia"),
        CountryDialOption("+960", "🇲🇻 Maldives", "Maldives"),
        CountryDialOption("+223", "🇲🇱 Mali", "Mali"),
        CountryDialOption("+356", "🇲🇹 Malta", "Malta"),
        CountryDialOption("+222", "🇲🇷 Mauritania", "Mauritania"),
        CountryDialOption("+230", "🇲🇺 Mauritius", "Mauritius"),
        CountryDialOption("+52", "🇲🇽 Mexico", "Mexico"),
        CountryDialOption("+373", "🇲🇩 Moldova", "Moldova"),
        CountryDialOption("+377", "🇲🇨 Monaco", "Monaco"),
        CountryDialOption("+976", "🇲🇳 Mongolia", "Mongolia"),
        CountryDialOption("+382", "🇲🇪 Montenegro", "Montenegro"),
        CountryDialOption("+212", "🇲🇦 Morocco", "Morocco"),
        CountryDialOption("+258", "🇲🇿 Mozambique", "Mozambique"),
        CountryDialOption("+95", "🇲🇲 Myanmar", "Myanmar"),
        CountryDialOption("+264", "🇳🇦 Namibia", "Namibia"),
        CountryDialOption("+977", "🇳🇵 Nepal", "Nepal"),
        CountryDialOption("+31", "🇳🇱 Netherlands", "Netherlands"),
        CountryDialOption("+64", "🇳🇿 New Zealand", "New Zealand"),
        CountryDialOption("+505", "🇳🇮 Nicaragua", "Nicaragua"),
        CountryDialOption("+227", "🇳🇪 Niger", "Niger"),
        CountryDialOption("+234", "🇳🇬 Nigeria", "Nigeria"),
        CountryDialOption("+389", "🇲🇰 North Macedonia", "North Macedonia"),
        CountryDialOption("+47", "🇳🇴 Norway", "Norway"),
        CountryDialOption("+92", "🇵🇰 Pakistan", "Pakistan"),
        CountryDialOption("+970", "🇵🇸 Palestine", "Palestine"),
        CountryDialOption("+507", "🇵🇦 Panama", "Panama"),
        CountryDialOption("+675", "🇵🇬 Papua New Guinea", "Papua New Guinea"),
        CountryDialOption("+595", "🇵🇾 Paraguay", "Paraguay"),
        CountryDialOption("+51", "🇵🇪 Peru", "Peru"),
        CountryDialOption("+63", "🇵🇭 Philippines", "Philippines"),
        CountryDialOption("+48", "🇵🇱 Poland", "Poland"),
        CountryDialOption("+351", "🇵🇹 Portugal", "Portugal"),
        CountryDialOption("+40", "🇷🇴 Romania", "Romania"),
        CountryDialOption("+7", "🇷🇺 Russia", "Russia"),
        CountryDialOption("+250", "🇷🇼 Rwanda", "Rwanda"),
        CountryDialOption("+221", "🇸🇳 Senegal", "Senegal"),
        CountryDialOption("+381", "🇷🇸 Serbia", "Serbia"),
        CountryDialOption("+248", "🇸🇨 Seychelles", "Seychelles"),
        CountryDialOption("+232", "🇸🇱 Sierra Leone", "Sierra Leone"),
        CountryDialOption("+65", "🇸🇬 Singapore", "Singapore"),
        CountryDialOption("+421", "🇸🇰 Slovakia", "Slovakia"),
        CountryDialOption("+386", "🇸🇮 Slovenia", "Slovenia"),
        CountryDialOption("+252", "🇸🇴 Somalia", "Somalia"),
        CountryDialOption("+27", "🇿🇦 South Africa", "South Africa"),
        CountryDialOption("+211", "🇸🇸 South Sudan", "South Sudan"),
        CountryDialOption("+34", "🇪🇸 Spain", "Spain"),
        CountryDialOption("+94", "🇱🇰 Sri Lanka", "Sri Lanka"),
        CountryDialOption("+249", "🇸🇩 Sudan", "Sudan"),
        CountryDialOption("+597", "🇸🇷 Suriname", "Suriname"),
        CountryDialOption("+46", "🇸🇪 Sweden", "Sweden"),
        CountryDialOption("+41", "🇨🇭 Switzerland", "Switzerland"),
        CountryDialOption("+963", "🇸🇾 Syria", "Syria"),
        CountryDialOption("+886", "🇹🇼 Taiwan", "Taiwan"),
        CountryDialOption("+992", "🇹🇯 Tajikistan", "Tajikistan"),
        CountryDialOption("+255", "🇹🇿 Tanzania", "Tanzania"),
        CountryDialOption("+66", "🇹🇭 Thailand", "Thailand"),
        CountryDialOption("+228", "🇹🇬 Togo", "Togo"),
        CountryDialOption("+1-868", "🇹🇹 Trinidad & Tobago", "Trinidad and Tobago"),
        CountryDialOption("+216", "🇹🇳 Tunisia", "Tunisia"),
        CountryDialOption("+90", "🇹🇷 Turkey", "Turkey"),
        CountryDialOption("+993", "🇹🇲 Turkmenistan", "Turkmenistan"),
        CountryDialOption("+256", "🇺🇬 Uganda", "Uganda"),
        CountryDialOption("+380", "🇺🇦 Ukraine", "Ukraine"),
        CountryDialOption("+598", "🇺🇾 Uruguay", "Uruguay"),
        CountryDialOption("+998", "🇺🇿 Uzbekistan", "Uzbekistan"),
        CountryDialOption("+58", "🇻🇪 Venezuela", "Venezuela"),
        CountryDialOption("+84", "🇻🇳 Vietnam", "Vietnam"),
        CountryDialOption("+967", "🇾🇪 Yemen", "Yemen"),
        CountryDialOption("+260", "🇿🇲 Zambia", "Zambia"),
        CountryDialOption("+263", "🇿🇼 Zimbabwe", "Zimbabwe")
    )

    fun inferCountryCode(city: String, branchName: String = ""): String {
        val combined = "$city $branchName".lowercase()
        val matched = COUNTRY_DIAL_OPTIONS.find { opt ->
            combined.contains(opt.countryName.lowercase())
        }
        if (matched != null) return matched.dialCode

        return when {
            combined.contains("saudi") || combined.contains("ksa") || combined.contains("riyadh") || combined.contains("dammam") || combined.contains("jeddah") || combined.contains("khobar") -> "+966"
            combined.contains("qatar") || combined.contains("doha") -> "+974"
            combined.contains("kuwait") -> "+965"
            combined.contains("oman") || combined.contains("muscat") || combined.contains("sohar") -> "+968"
            combined.contains("bahrain") || combined.contains("manama") -> "+973"
            combined.contains("india") || combined.contains("mumbai") || combined.contains("chennai") || combined.contains("delhi") || combined.contains("bangalore") || combined.contains("kochi") -> "+91"
            combined.contains("belgium") || combined.contains("brussels") || combined.contains("antwerp") -> "+32"
            combined.contains("uk") || combined.contains("london") || combined.contains("britain") || combined.contains("aberdeen") -> "+44"
            combined.contains("algeria") || combined.contains("algiers") || combined.contains("oran") -> "+213"
            combined.contains("canada") || combined.contains("toronto") || combined.contains("calgary") || combined.contains("vancouver") || combined.contains("montreal") -> "+1"
            combined.contains("usa") || combined.contains("houston") || combined.contains("new york") || combined.contains("texas") -> "+1"
            combined.contains("egypt") || combined.contains("cairo") || combined.contains("alexandria") -> "+20"
            combined.contains("jordan") || combined.contains("amman") -> "+962"
            combined.contains("singapore") -> "+65"
            combined.contains("germany") || combined.contains("frankfurt") || combined.contains("munich") -> "+49"
            combined.contains("france") || combined.contains("paris") -> "+33"
            combined.contains("china") || combined.contains("shanghai") || combined.contains("beijing") -> "+86"
            else -> "+971"
        }
    }

    val INITIAL_BRANCHES = listOf(
        Branch(
            id = "bss_dxb",
            name = "BSS Tech / BSMC - Dubai",
            shortCode = "BSST-DXB",
            city = "Dubai",
            subtitle = "Engineering & Corrosion Control",
            defaultDepartments = listOf("PROJECTS", "QHSE", "SALES & MARKETING", "FINANCE", "MANAGEMENT", "OPERATIONS"),
            countryCode = "+971",
            facilityLocation = "Engineering & Corrosion Control, Dubai",
            safetyGuidelines = Branch.DEFAULT_SAFETY_GUIDELINES,
            safetyHelpline = "+971 4 883 1234 / QHSE Ext. 201",
            safetyContactName = "QHSE Officer / Security Desk"
        ),
        Branch(
            id = "sait_dxb",
            name = "Sait FZCO - Dubai",
            shortCode = "SAIT-DXB",
            city = "Dubai",
            subtitle = "Industrial Free Zone Facility",
            defaultDepartments = listOf("PROJECTS", "QHSE", "SALES & MARKETING", "FINANCE", "MANAGEMENT", "MAINTENANCE"),
            countryCode = "+971",
            facilityLocation = "Industrial Free Zone Facility, Dubai",
            safetyGuidelines = Branch.DEFAULT_SAFETY_GUIDELINES,
            safetyHelpline = "+971 4 883 0000 / Security Ext. 101",
            safetyContactName = "QHSE Officer / Security Desk"
        ),
        Branch(
            id = "bss_auh",
            name = "BSS Tech - Abu Dhabi",
            shortCode = "BSST-AUH",
            city = "Abu Dhabi",
            subtitle = "Cathodic Protection & Inspection",
            defaultDepartments = listOf("PROJECTS", "QHSE", "COMMERCIAL", "FINANCE", "MANAGEMENT", "ENGINEERING"),
            countryCode = "+971",
            facilityLocation = "Cathodic Protection & Inspection, Abu Dhabi",
            safetyGuidelines = Branch.DEFAULT_SAFETY_GUIDELINES,
            safetyHelpline = "+971 2 555 8899 / Safety Desk",
            safetyContactName = "QHSE Officer / Safety Desk"
        ),
        Branch(
            id = "ssec_auh",
            name = "SSEC - Abu Dhabi",
            shortCode = "SSEC-AUH",
            city = "Abu Dhabi",
            subtitle = "Specialized Engineering Services",
            defaultDepartments = listOf("OPERATIONS", "QHSE", "PROJECTS", "PROCUREMENT", "MANAGEMENT", "MAINTENANCE"),
            countryCode = "+971",
            facilityLocation = "Specialized Engineering Services, Abu Dhabi",
            safetyGuidelines = Branch.DEFAULT_SAFETY_GUIDELINES,
            safetyHelpline = "+971 2 666 4422 / Control Room",
            safetyContactName = "Operations Control / QHSE Officer"
        ),
        Branch(
            id = "ssmi_auh",
            name = "SSMI - Abu Dhabi",
            shortCode = "SSMI-AUH",
            city = "Abu Dhabi",
            subtitle = "Manufacturing & Industrial Support",
            defaultDepartments = listOf("PRODUCTION", "QHSE", "LOGISTICS", "FINANCE", "MANAGEMENT", "QUALITY CONTROL"),
            countryCode = "+971",
            facilityLocation = "Manufacturing & Industrial Support, Abu Dhabi",
            safetyGuidelines = Branch.DEFAULT_SAFETY_GUIDELINES,
            safetyHelpline = "+971 2 777 3311 / Plant Safety",
            safetyContactName = "Plant Safety Officer"
        ),
        Branch(
            id = "3e_rak",
            name = "3E - RAK",
            shortCode = "3E-RAK",
            city = "Ras Al Khaimah",
            subtitle = "Northern Emirates Engineering Branch",
            defaultDepartments = listOf("PROJECTS", "QHSE", "TECHNICAL SERVICES", "FINANCE", "MANAGEMENT", "WORKSHOP"),
            countryCode = "+971",
            facilityLocation = "Northern Emirates Engineering Branch, Ras Al Khaimah",
            safetyGuidelines = Branch.DEFAULT_SAFETY_GUIDELINES,
            safetyHelpline = "+971 7 244 5566 / Workshop Control",
            safetyContactName = "Workshop Safety Supervisor"
        )
    )

    // Backward-compatible reference
    val ALL_BRANCHES: List<Branch>
        get() = INITIAL_BRANCHES

    fun getBranchByName(name: String, customBranches: List<Branch>? = null): Branch {
        val list = if (!customBranches.isNullOrEmpty()) customBranches else INITIAL_BRANCHES
        return list.find { it.name.equals(name, ignoreCase = true) }
            ?: list.find { it.shortCode.equals(name, ignoreCase = true) }
            ?: list.firstOrNull()
            ?: INITIAL_BRANCHES[1]
    }

    fun branchesToJson(branches: List<Branch>): String {
        val array = JSONArray()
        branches.forEach { array.put(it.toJsonObject()) }
        return array.toString()
    }

    fun branchesFromJson(jsonStr: String): List<Branch> {
        if (jsonStr.isBlank()) return INITIAL_BRANCHES
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<Branch>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(Branch.fromJsonObject(obj))
            }
            if (list.isEmpty()) INITIAL_BRANCHES else list
        } catch (e: Exception) {
            INITIAL_BRANCHES
        }
    }
}
