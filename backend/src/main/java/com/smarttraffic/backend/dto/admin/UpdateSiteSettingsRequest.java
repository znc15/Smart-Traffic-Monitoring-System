package com.smarttraffic.backend.dto.admin;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

public class UpdateSiteSettingsRequest {

    @JsonAlias("site_name")
    @Size(max = 200, message = "站点名称不能超过200个字符")
    private String siteName;

    @Size(max = 1000, message = "公告内容不能超过1000个字符")
    private String announcement;

    @JsonAlias("logo_url")
    private String logoUrl;

    @JsonAlias("footer_text")
    @Size(max = 1000, message = "页脚文本不能超过1000个字符")
    private String footerText;

    @JsonAlias("amap_key")
    @Size(max = 255, message = "高德地图 Key 不能超过255个字符")
    private String amapKey;

    @JsonAlias("amap_security_js_code")
    @Size(max = 255, message = "高德地图 Security JS Code 不能超过255个字符")
    private String amapSecurityJsCode;

    @JsonAlias("amap_service_host")
    @Size(max = 255, message = "高德地图 Service Host 不能超过255个字符")
    private String amapServiceHost;

    @JsonAlias("congestion_threshold")
    private Double congestionThreshold;

    @JsonAlias("llm_provider")
    @Size(max = 32, message = "LLM 提供商不能超过32个字符")
    private String llmProvider;

    @JsonAlias("llm_api_base_url")
    @Size(max = 512, message = "LLM API Base URL 不能超过512个字符")
    private String llmApiBaseUrl;

    @JsonAlias("llm_api_key")
    @Size(max = 512, message = "LLM API Key 不能超过512个字符")
    private String llmApiKey;

    @JsonAlias("llm_model_name")
    @Size(max = 128, message = "LLM 模型名称不能超过128个字符")
    private String llmModelName;

    @JsonAlias("ai_float_visible_pages")
    private String aiFloatVisiblePages;

    @JsonAlias("llm_title_model_name")
    @Size(max = 255, message = "标题生成模型名称不能超过255个字符")
    private String llmTitleModelName;

    @JsonAlias("llm_title_prompt")
    private String llmTitlePrompt;

    @JsonIgnore
    private final Set<String> setFields = new HashSet<>();

    @JsonIgnore
    public boolean hasField(String field) {
        return setFields.contains(field);
    }

    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; setFields.add("siteName"); }

    public String getAnnouncement() { return announcement; }
    public void setAnnouncement(String announcement) { this.announcement = announcement; setFields.add("announcement"); }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; setFields.add("logoUrl"); }

    public String getFooterText() { return footerText; }
    public void setFooterText(String footerText) { this.footerText = footerText; setFields.add("footerText"); }

    public String getAmapKey() { return amapKey; }
    public void setAmapKey(String amapKey) { this.amapKey = amapKey; setFields.add("amapKey"); }

    public String getAmapSecurityJsCode() { return amapSecurityJsCode; }
    public void setAmapSecurityJsCode(String amapSecurityJsCode) { this.amapSecurityJsCode = amapSecurityJsCode; setFields.add("amapSecurityJsCode"); }

    public String getAmapServiceHost() { return amapServiceHost; }
    public void setAmapServiceHost(String amapServiceHost) { this.amapServiceHost = amapServiceHost; setFields.add("amapServiceHost"); }

    public Double getCongestionThreshold() { return congestionThreshold; }
    public void setCongestionThreshold(Double congestionThreshold) { this.congestionThreshold = congestionThreshold; setFields.add("congestionThreshold"); }

    public String getLlmProvider() { return llmProvider; }
    public void setLlmProvider(String llmProvider) { this.llmProvider = llmProvider; setFields.add("llmProvider"); }

    public String getLlmApiBaseUrl() { return llmApiBaseUrl; }
    public void setLlmApiBaseUrl(String llmApiBaseUrl) { this.llmApiBaseUrl = llmApiBaseUrl; setFields.add("llmApiBaseUrl"); }

    public String getLlmApiKey() { return llmApiKey; }
    public void setLlmApiKey(String llmApiKey) { this.llmApiKey = llmApiKey; setFields.add("llmApiKey"); }

    public String getLlmModelName() { return llmModelName; }
    public void setLlmModelName(String llmModelName) { this.llmModelName = llmModelName; setFields.add("llmModelName"); }

    public String getAiFloatVisiblePages() { return aiFloatVisiblePages; }
    public void setAiFloatVisiblePages(String aiFloatVisiblePages) { this.aiFloatVisiblePages = aiFloatVisiblePages; setFields.add("aiFloatVisiblePages"); }

    public String getLlmTitleModelName() { return llmTitleModelName; }
    public void setLlmTitleModelName(String llmTitleModelName) { this.llmTitleModelName = llmTitleModelName; setFields.add("llmTitleModelName"); }

    public String getLlmTitlePrompt() { return llmTitlePrompt; }
    public void setLlmTitlePrompt(String llmTitlePrompt) { this.llmTitlePrompt = llmTitlePrompt; setFields.add("llmTitlePrompt"); }
}
