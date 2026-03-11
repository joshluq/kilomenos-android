interface Config {
    val applicationNamespace: String
    val versionCode: Int
    val versionName: String
}

interface Environment {
    val name: String
    val serverUrl: String
    val storageUrl: String
    val termsUrl: String
    val privacyUrl: String
    val premiumSku: String
    val applicationIdSuffix: String?
}

object AppConfig : Config {
    override val applicationNamespace: String = "es.joshluq.kmsafe"
    override val versionCode: Int = 1
    override val versionName: String = "1.0.0"

    object Environments {
        object Development : Environment {
            override val name: String = "Dev"
            override val serverUrl: String = "https://pfbdokxsmmrhrltfsnox.supabase.co/functions/v1/api/"
            override val storageUrl: String = "https://pfbdokxsmmrhrltfsnox.supabase.co/storage/v1/object/public/vehicle-images/contratos/"
            override val termsUrl: String = "https://kilomenos-dev.web.app/terms.html"
            override val privacyUrl: String = "https://kilomenos-dev.web.app/privacy.html"
            override val premiumSku: String = "subscription_premium_monthly"
            override val applicationIdSuffix: String = ".dev"
        }

        object Production : Environment {
            override val name: String = "Pro"
            override val serverUrl: String = "https://gist.githubusercontent.com/palcalde/6c19259bd32dd6aafa327fa557859c2f/raw/ba51779474a150ee4367cda4f4ffacdcca479887/"
            override val storageUrl: String = ""
            override val termsUrl: String = "https://kilomenos.web.app/terms.html"
            override val privacyUrl: String = "https://kilomenos.web.app/privacy.html"
            override val premiumSku: String = "subscription_premium_monthly"
            override val applicationIdSuffix: String? = null
        }

        val availableEnvironments = listOf(Development, Production)
    }
}
