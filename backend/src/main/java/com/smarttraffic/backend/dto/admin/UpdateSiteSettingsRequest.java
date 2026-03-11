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
}
