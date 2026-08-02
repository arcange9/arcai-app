package com.example.model

enum class ProviderCategory(val displayName: String) {
    CHAT("Chat & Reasoning"),
    CODE("Coding Assistant"),
    VISION("Vision & Multimodal"),
    IMAGE("Image Generation"),
    AUDIO("Voice & Audio"),
    LOCAL("Local & Routing")
}

enum class VerificationAuthType {
    BEARER,
    X_API_KEY_ANTHROPIC,
    GEMINI_QUERY_PARAM,
    TOKEN_HEADER,
    ELEVEN_LABS_HEADER,
    CUSTOM_SYNTAX,
    LOCAL_URL
}

data class ProviderModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val isVisionSupported: Boolean = false,
    val isCodeSpecialized: Boolean = false
)

enum class AiProvider(
    val id: String,
    val displayName: String,
    val category: ProviderCategory,
    val description: String,
    val defaultModel: String,
    val availableModels: List<ProviderModelInfo>,
    val keyPlaceholder: String,
    val docsUrl: String,
    val verifyEndpoint: String,
    val authType: VerificationAuthType,
    val isLocal: Boolean = false
) {
    OPENAI(
        id = "openai",
        displayName = "OpenAI",
        category = ProviderCategory.CHAT,
        description = "GPT-4o, o1-preview, and Whisper models for reasoning, vision, and code.",
        defaultModel = "gpt-4o",
        availableModels = listOf(
            ProviderModelInfo("gpt-4o", "GPT-4o", "Flagship multimodal reasoning", isVisionSupported = true),
            ProviderModelInfo("gpt-4o-mini", "GPT-4o Mini", "Fast and affordable multimodal model", isVisionSupported = true),
            ProviderModelInfo("o1-preview", "o1 Preview", "Advanced STEM and math reasoning"),
            ProviderModelInfo("o1-mini", "o1 Mini", "Fast code & reasoning specialist", isCodeSpecialized = true)
        ),
        keyPlaceholder = "sk-...",
        docsUrl = "https://platform.openai.com/api-keys",
        verifyEndpoint = "https://api.openai.com/v1/models",
        authType = VerificationAuthType.BEARER
    ),
    ANTHROPIC(
        id = "anthropic",
        displayName = "Anthropic Claude",
        category = ProviderCategory.CHAT,
        description = "Claude 3.5 Sonnet, Haiku, and Opus with nuanced writing and coding.",
        defaultModel = "claude-3-5-sonnet-20241022",
        availableModels = listOf(
            ProviderModelInfo("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", "Top coding, reasoning & vision model", isVisionSupported = true, isCodeSpecialized = true),
            ProviderModelInfo("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", "Fast response latency"),
            ProviderModelInfo("claude-3-opus-20240229", "Claude 3 Opus", "Deep analytical reasoning")
        ),
        keyPlaceholder = "sk-ant-...",
        docsUrl = "https://console.anthropic.com/settings/keys",
        verifyEndpoint = "https://api.anthropic.com/v1/models",
        authType = VerificationAuthType.X_API_KEY_ANTHROPIC
    ),
    GOOGLE_AI_STUDIO(
        id = "google_ai_studio",
        displayName = "Google AI Studio",
        category = ProviderCategory.CHAT,
        description = "Gemini 1.5 Pro and Flash with 2M token context window and vision.",
        defaultModel = "gemini-1.5-pro",
        availableModels = listOf(
            ProviderModelInfo("gemini-1.5-pro", "Gemini 1.5 Pro", "2M token context & advanced reasoning", isVisionSupported = true),
            ProviderModelInfo("gemini-1.5-flash", "Gemini 1.5 Flash", "Ultra-fast multimodal model", isVisionSupported = true),
            ProviderModelInfo("gemini-1.5-flash-8b", "Gemini 1.5 Flash-8B", "High throughput lightweight model", isVisionSupported = true)
        ),
        keyPlaceholder = "AIzaSy...",
        docsUrl = "https://aistudio.google.com/app/apikey",
        verifyEndpoint = "https://generativelanguage.googleapis.com/v1beta/models",
        authType = VerificationAuthType.GEMINI_QUERY_PARAM
    ),
    MISTRAL_AI(
        id = "mistral",
        displayName = "Mistral AI",
        category = ProviderCategory.CODE,
        description = "Mistral Large 2, Codestral, and Pixtral open weights leaders.",
        defaultModel = "mistral-large-latest",
        availableModels = listOf(
            ProviderModelInfo("mistral-large-latest", "Mistral Large 2", "High reasoning & multilingual capability"),
            ProviderModelInfo("codestral-latest", "Codestral", "Specialized coding & refactoring model", isCodeSpecialized = true),
            ProviderModelInfo("pixtral-12b-2409", "Pixtral 12B", "Vision & multimodal capabilities", isVisionSupported = true)
        ),
        keyPlaceholder = "mistral_...",
        docsUrl = "https://console.mistral.ai/api-keys",
        verifyEndpoint = "https://api.mistral.ai/v1/models",
        authType = VerificationAuthType.BEARER
    ),
    COHERE(
        id = "cohere",
        displayName = "Cohere",
        category = ProviderCategory.CHAT,
        description = "Command R+ and R series optimized for RAG and citation accuracy.",
        defaultModel = "command-r-plus",
        availableModels = listOf(
            ProviderModelInfo("command-r-plus", "Command R+", "Enterprise RAG & analytical reasoning"),
            ProviderModelInfo("command-r", "Command R", "Fast business intelligence model")
        ),
        keyPlaceholder = "cohere_key_...",
        docsUrl = "https://dashboard.cohere.com/api-keys",
        verifyEndpoint = "https://api.cohere.ai/v1/models",
        authType = VerificationAuthType.BEARER
    ),
    GROQ(
        id = "groq",
        displayName = "Groq",
        category = ProviderCategory.CHAT,
        description = "Ultra-fast LPU inference for Llama 3.3 70B and Mixtral.",
        defaultModel = "llama-3.3-70b-versatile",
        availableModels = listOf(
            ProviderModelInfo("llama-3.3-70b-versatile", "Llama 3.3 70B", "70B versatile reasoning at ~300 T/s", isCodeSpecialized = true),
            ProviderModelInfo("llama-3.1-8b-instant", "Llama 3.1 8B Instant", "Ultra-fast conversational latency"),
            ProviderModelInfo("mixtral-8x7b-32768", "Mixtral 8x7B", "Fast MoE code & text generation")
        ),
        keyPlaceholder = "gsk_...",
        docsUrl = "https://console.groq.com/keys",
        verifyEndpoint = "https://api.groq.com/openai/v1/models",
        authType = VerificationAuthType.BEARER
    ),
    DEEPSEEK(
        id = "deepseek",
        displayName = "DeepSeek",
        category = ProviderCategory.CODE,
        description = "DeepSeek V3 and R1 reasoning & coding specialist.",
        defaultModel = "deepseek-chat",
        availableModels = listOf(
            ProviderModelInfo("deepseek-chat", "DeepSeek V3", "Versatile high-accuracy chat & code", isCodeSpecialized = true),
            ProviderModelInfo("deepseek-reasoner", "DeepSeek R1", "Chain-of-thought mathematical & code reasoning", isCodeSpecialized = true)
        ),
        keyPlaceholder = "sk-...",
        docsUrl = "https://platform.deepseek.com/api_keys",
        verifyEndpoint = "https://api.deepseek.com/models",
        authType = VerificationAuthType.BEARER
    ),
    OPENROUTER(
        id = "openrouter",
        displayName = "OpenRouter",
        category = ProviderCategory.LOCAL,
        description = "Unified gateway to 100+ AI models from all labs in one API.",
        defaultModel = "openrouter/auto",
        availableModels = listOf(
            ProviderModelInfo("openrouter/auto", "Auto Router", "Automatically routes to optimal model"),
            ProviderModelInfo("meta-llama/llama-3.3-70b-instruct", "Llama 3.3 70B (OpenRouter)", "Open weights 70B model"),
            ProviderModelInfo("deepseek/deepseek-r1", "DeepSeek R1 via OpenRouter", "Advanced reasoning model")
        ),
        keyPlaceholder = "sk-or-v1-...",
        docsUrl = "https://openrouter.ai/keys",
        verifyEndpoint = "https://openrouter.ai/api/v1/models",
        authType = VerificationAuthType.BEARER
    ),
    HUGGING_FACE(
        id = "huggingface",
        displayName = "Hugging Face",
        category = ProviderCategory.LOCAL,
        description = "Access thousands of open models via Inference API & Serverless Endpoints.",
        defaultModel = "meta-llama/Llama-3.3-70B-Instruct",
        availableModels = listOf(
            ProviderModelInfo("meta-llama/Llama-3.3-70B-Instruct", "Llama 3.3 70B", "Hugging Face Serverless Inference"),
            ProviderModelInfo("mistralai/Mistral-7B-Instruct-v0.3", "Mistral 7B v0.3", "Fast open instruction model"),
            ProviderModelInfo("bigcode/starcoder2-15b", "StarCoder 2 15B", "Open source code specialist", isCodeSpecialized = true)
        ),
        keyPlaceholder = "hf_...",
        docsUrl = "https://huggingface.co/settings/tokens",
        verifyEndpoint = "https://huggingface.co/api/whoami-v2",
        authType = VerificationAuthType.BEARER
    ),
    TOGETHER_AI(
        id = "together",
        displayName = "Together AI",
        category = ProviderCategory.LOCAL,
        description = "High-performance decentralized GPU inference for open source models.",
        defaultModel = "meta-llama/Llama-3.3-70B-Instruct-Turbo",
        availableModels = listOf(
            ProviderModelInfo("meta-llama/Llama-3.3-70B-Instruct-Turbo", "Llama 3.3 70B Turbo", "Fast open weights inference"),
            ProviderModelInfo("Qwen/Qwen2.5-Coder-32B-Instruct", "Qwen 2.5 Coder 32B", "World-class open coding model", isCodeSpecialized = true)
        ),
        keyPlaceholder = "together_...",
        docsUrl = "https://api.together.ai/settings/api-keys",
        verifyEndpoint = "https://api.together.xyz/v1/models",
        authType = VerificationAuthType.BEARER
    ),
    AI21_LABS(
        id = "ai21",
        displayName = "AI21 Labs",
        category = ProviderCategory.CHAT,
        description = "Jamba 1.5 Hybrid Transformer-Mamba models with 256k context.",
        defaultModel = "jamba-1.5-large",
        availableModels = listOf(
            ProviderModelInfo("jamba-1.5-large", "Jamba 1.5 Large", "Enterprise SSM-Transformer hybrid"),
            ProviderModelInfo("jamba-1.5-mini", "Jamba 1.5 Mini", "Fast lightweight Jamba model")
        ),
        keyPlaceholder = "ai21-key-...",
        docsUrl = "https://studio.ai21.com/account/api-key",
        verifyEndpoint = "https://api.ai21.com/studio/v1/models",
        authType = VerificationAuthType.BEARER
    ),
    ANYSCALE(
        id = "anyscale",
        displayName = "Anyscale / Ray",
        category = ProviderCategory.CODE,
        description = "Ray-powered serverless endpoints for Llama and CodeLlama.",
        defaultModel = "meta-llama/Llama-3-70b-chat-hf",
        availableModels = listOf(
            ProviderModelInfo("meta-llama/Llama-3-70b-chat-hf", "Llama 3 70B", "Distributed high-speed Llama"),
            ProviderModelInfo("codellama/CodeLlama-70b-Instruct-hf", "CodeLlama 70B", "Dedicated coding endpoint", isCodeSpecialized = true)
        ),
        keyPlaceholder = "esecrety_...",
        docsUrl = "https://app.endpoints.anyscale.com/",
        verifyEndpoint = "https://api.endpoints.anyscale.com/v1/models",
        authType = VerificationAuthType.BEARER
    ),
    REPLICATE(
        id = "replicate",
        displayName = "Replicate",
        category = ProviderCategory.IMAGE,
        description = "Run SDXL, Flux, Whisper, and thousands of custom ML models.",
        defaultModel = "black-forest-labs/flux-schnell",
        availableModels = listOf(
            ProviderModelInfo("black-forest-labs/flux-schnell", "FLUX.1 Schnell", "Fast state-of-the-art image generation"),
            ProviderModelInfo("meta/meta-llama-3-70b-instruct", "Llama 3 70B Instruct", "Replicate hosted LLM")
        ),
        keyPlaceholder = "r8_...",
        docsUrl = "https://replicate.com/account/api-tokens",
        verifyEndpoint = "https://api.replicate.com/v1/account",
        authType = VerificationAuthType.TOKEN_HEADER
    ),
    DEEPINFRA(
        id = "deepinfra",
        displayName = "DeepInfra",
        category = ProviderCategory.LOCAL,
        description = "Cost-effective serverless inference for Llama, Qwen, and Whisper.",
        defaultModel = "meta-llama/Meta-Llama-3.1-70B-Instruct",
        availableModels = listOf(
            ProviderModelInfo("meta-llama/Meta-Llama-3.1-70B-Instruct", "Llama 3.1 70B", "High-throughput serverless API"),
            ProviderModelInfo("Qwen/Qwen2.5-Coder-32B-Instruct", "Qwen 2.5 Coder 32B", "Code generation specialist", isCodeSpecialized = true)
        ),
        keyPlaceholder = "deepinfra-...",
        docsUrl = "https://deepinfra.com/dash/api_keys",
        verifyEndpoint = "https://api.deepinfra.com/v1/openai/models",
        authType = VerificationAuthType.BEARER
    ),
    FIREWORKS_AI(
        id = "fireworks",
        displayName = "Fireworks AI",
        category = ProviderCategory.CODE,
        description = "Ultra-fast developer API for FireFunction and open models.",
        defaultModel = "accounts/fireworks/models/llama-v3p3-70b-instruct",
        availableModels = listOf(
            ProviderModelInfo("accounts/fireworks/models/llama-v3p3-70b-instruct", "Llama 3.3 70B", "Optimized speculative decoding"),
            ProviderModelInfo("accounts/fireworks/models/qwen2p5-coder-32b-instruct", "Qwen 2.5 Coder 32B", "High performance coding model", isCodeSpecialized = true)
        ),
        keyPlaceholder = "fw_...",
        docsUrl = "https://fireworks.ai/account/api-keys",
        verifyEndpoint = "https://api.fireworks.ai/inference/v1/models",
        authType = VerificationAuthType.BEARER
    ),
    AMAZON_BEDROCK(
        id = "bedrock",
        displayName = "Amazon Bedrock",
        category = ProviderCategory.CHAT,
        description = "AWS managed access to Claude, Titan, Llama, and Cohere models.",
        defaultModel = "anthropic.claude-3-5-sonnet-20241022-v2:0",
        availableModels = listOf(
            ProviderModelInfo("anthropic.claude-3-5-sonnet-20241022-v2:0", "Claude 3.5 Sonnet (AWS)", "AWS Bedrock endpoint", isVisionSupported = true, isCodeSpecialized = true),
            ProviderModelInfo("amazon.titan-text-premier-v1:0", "Amazon Titan Premier", "AWS native foundational LLM")
        ),
        keyPlaceholder = "AKIA... / AWS Secret Key",
        docsUrl = "https://aws.amazon.com/bedrock/",
        verifyEndpoint = "https://bedrock.us-east-1.amazonaws.com",
        authType = VerificationAuthType.CUSTOM_SYNTAX
    ),
    MICROSOFT_AZURE_AI(
        id = "azure_ai",
        displayName = "Microsoft Azure AI",
        category = ProviderCategory.CHAT,
        description = "Enterprise Azure OpenAI & Foundry model endpoints.",
        defaultModel = "gpt-4o-azure",
        availableModels = listOf(
            ProviderModelInfo("gpt-4o-azure", "GPT-4o (Azure)", "Azure OpenAI deployment", isVisionSupported = true),
            ProviderModelInfo("phi-3-medium-4k-instruct", "Microsoft Phi-3 Medium", "Lightweight reasoning specialist")
        ),
        keyPlaceholder = "Azure API Key / Endpoint URL",
        docsUrl = "https://portal.azure.com/",
        verifyEndpoint = "https://management.azure.com",
        authType = VerificationAuthType.CUSTOM_SYNTAX
    ),
    STABILITY_AI(
        id = "stability",
        displayName = "Stability AI",
        category = ProviderCategory.IMAGE,
        description = "Stable Diffusion 3.5, Stable Video, and Stable Audio generation.",
        defaultModel = "sd3.5-large",
        availableModels = listOf(
            ProviderModelInfo("sd3.5-large", "Stable Diffusion 3.5 Large", "Highest fidelity image generation"),
            ProviderModelInfo("sd3.5-large-turbo", "SD 3.5 Turbo", "Fast 4-step image synthesis")
        ),
        keyPlaceholder = "sk-...",
        docsUrl = "https://platform.stability.ai/account/keys",
        verifyEndpoint = "https://api.stability.ai/v1/user/account",
        authType = VerificationAuthType.BEARER
    ),
    MIDJOURNEY(
        id = "midjourney",
        displayName = "Midjourney (API/Proxy)",
        category = ProviderCategory.IMAGE,
        description = "Cinematic image generation via Midjourney API bridge.",
        defaultModel = "mj-v6.1",
        availableModels = listOf(
            ProviderModelInfo("mj-v6.1", "Midjourney v6.1", "Photorealistic & artistic image generation"),
            ProviderModelInfo("niji-6", "Niji v6", "Anime & illustration style specialist")
        ),
        keyPlaceholder = "mj_api_token_...",
        docsUrl = "https://www.midjourney.com/",
        verifyEndpoint = "https://api.midjourney.com",
        authType = VerificationAuthType.CUSTOM_SYNTAX
    ),
    ELEVENLABS(
        id = "elevenlabs",
        displayName = "ElevenLabs",
        category = ProviderCategory.AUDIO,
        description = "Natural voice synthesis, voice cloning, and multilingual TTS.",
        defaultModel = "eleven_multilingual_v2",
        availableModels = listOf(
            ProviderModelInfo("eleven_multilingual_v2", "Multilingual v2", "Natural emotion and 29 languages TTS"),
            ProviderModelInfo("eleven_turbo_v2_5", "Turbo v2.5", "Low-latency voice assistant TTS")
        ),
        keyPlaceholder = "xi-api-key...",
        docsUrl = "https://elevenlabs.io/app/settings/api-keys",
        verifyEndpoint = "https://api.elevenlabs.io/v1/user",
        authType = VerificationAuthType.ELEVEN_LABS_HEADER
    ),
    DEEPGRAM(
        id = "deepgram",
        displayName = "Deepgram",
        category = ProviderCategory.AUDIO,
        description = "Nova-2 Speech-to-Text and Aura TTS with sub-300ms latency.",
        defaultModel = "nova-2",
        availableModels = listOf(
            ProviderModelInfo("nova-2", "Nova-2 Speech-to-Text", "Real-time accuracy & speaker diarization"),
            ProviderModelInfo("aura-asteria-en", "Aura TTS Asteria", "Natural conversational voice")
        ),
        keyPlaceholder = "Token ...",
        docsUrl = "https://console.deepgram.com/",
        verifyEndpoint = "https://api.deepgram.com/v1/projects",
        authType = VerificationAuthType.TOKEN_HEADER
    ),
    ASSEMBLY_AI(
        id = "assemblyai",
        displayName = "AssemblyAI",
        category = ProviderCategory.AUDIO,
        description = "Universal-1 speech recognition, audio intelligence & summaries.",
        defaultModel = "universal-1",
        availableModels = listOf(
            ProviderModelInfo("universal-1", "Universal-1 STT", "High-accuracy transcription & punctuation"),
            ProviderModelInfo("lemur", "LeMUR Audio LLM", "Question answering over audio transcripts")
        ),
        keyPlaceholder = "assembly_...",
        docsUrl = "https://www.assemblyai.com/app/account",
        verifyEndpoint = "https://api.assemblyai.com/v2/transcript",
        authType = VerificationAuthType.TOKEN_HEADER
    ),
    RUNWAY(
        id = "runway",
        displayName = "Runway",
        category = ProviderCategory.IMAGE,
        description = "Gen-3 Alpha video generation and AI filmmaking suite.",
        defaultModel = "gen3a_turbo",
        availableModels = listOf(
            ProviderModelInfo("gen3a_turbo", "Gen-3 Alpha Turbo", "High quality text & image to video"),
            ProviderModelInfo("gen2", "Gen-2 Video", "Versatile video generation")
        ),
        keyPlaceholder = "rw_...",
        docsUrl = "https://app.runwayml.com/account",
        verifyEndpoint = "https://api.runwayml.com/v1/user",
        authType = VerificationAuthType.BEARER
    ),
    MARTIAN(
        id = "martian",
        displayName = "Martian Model Router",
        category = ProviderCategory.LOCAL,
        description = "Dynamic router that optimizes cost and accuracy per prompt.",
        defaultModel = "router-v1",
        availableModels = listOf(
            ProviderModelInfo("router-v1", "Martian Dynamic Router", "Auto-selects GPT-4, Claude, or open source"),
            ProviderModelInfo("cost-optimized", "Cost Optimizer", "Lowest cost model meeting accuracy score")
        ),
        keyPlaceholder = "mr_...",
        docsUrl = "https://withmartian.com/",
        verifyEndpoint = "https://api.martian.ai/v1/models",
        authType = VerificationAuthType.BEARER
    ),
    OLLAMA(
        id = "ollama",
        displayName = "Ollama (Local / Custom URL)",
        category = ProviderCategory.LOCAL,
        description = "Run Llama 3, DeepSeek R1, Mistral, and Qwen locally on your machine.",
        defaultModel = "llama3.2:3b",
        availableModels = listOf(
            ProviderModelInfo("llama3.2:3b", "Llama 3.2 3B", "Local lightweight reasoning model", isCodeSpecialized = true),
            ProviderModelInfo("deepseek-r1:7b", "DeepSeek R1 7B", "Local reasoning & math model", isCodeSpecialized = true),
            ProviderModelInfo("qwen2.5-coder:7b", "Qwen 2.5 Coder 7B", "Local coding assistant", isCodeSpecialized = true)
        ),
        keyPlaceholder = "http://localhost:11434 (Host URL)",
        docsUrl = "https://ollama.com/",
        verifyEndpoint = "http://localhost:11434/api/tags",
        authType = VerificationAuthType.LOCAL_URL,
        isLocal = true
    ),
    GROK(
        id = "grok",
        displayName = "xAI Grok",
        category = ProviderCategory.CHAT,
        description = "Grok-2 and Grok-2 Vision with real-time knowledge and reasoning.",
        defaultModel = "grok-2-latest",
        availableModels = listOf(
            ProviderModelInfo("grok-2-latest", "Grok 2 Latest", "High-accuracy reasoning & vision", isVisionSupported = true),
            ProviderModelInfo("grok-2-mini", "Grok 2 Mini", "Low-latency conversational model")
        ),
        keyPlaceholder = "xai-...",
        docsUrl = "https://console.x.ai/",
        verifyEndpoint = "https://api.x.ai/v1/models",
        authType = VerificationAuthType.BEARER
    );

    companion object {
        fun fromId(id: String): AiProvider = entries.find { it.id == id } ?: OPENAI
    }
}
