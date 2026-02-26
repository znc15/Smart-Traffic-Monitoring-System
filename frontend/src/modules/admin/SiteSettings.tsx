import { useCallback, useEffect, useRef, useState } from "react";
import { Button } from "@/ui/button";
import { Input } from "@/ui/input";
import { Textarea } from "@/ui/textarea";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/ui/card";
import { Label } from "@/ui/label";
import { authFetch, endpoints, adminConfig } from "@/config";
import { ImageIcon, Megaphone, Palette, Save, Loader2, AlertCircle } from "lucide-react";
import { normalizeSiteSettings } from "@/utils/normalize";

interface Settings {
  siteName: string;
  logoUrl: string;
  announcement: string;
  footerText: string;
}

const defaultSettings: Settings = {
  siteName: "",
  logoUrl: "",
  announcement: "",
  footerText: "",
};

export default function SiteSettings() {
  const [form, setForm] = useState<Settings>(defaultSettings);
  const [saved, setSaved] = useState<Settings>(defaultSettings);
  const [msg, setMsg] = useState<{ text: string; ok: boolean } | null>(null);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const [logoError, setLogoError] = useState(false);
  const [debouncedLogoUrl, setDebouncedLogoUrl] = useState("");
  const msgTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const logoDebounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Cleanup msgTimer on unmount (I4)
  useEffect(() => {
    return () => {
      if (msgTimer.current) clearTimeout(msgTimer.current);
      if (logoDebounceTimer.current) clearTimeout(logoDebounceTimer.current);
    };
  }, []);

  // Debounce logo preview URL (W1) — only render preview after 500ms idle
  useEffect(() => {
    if (logoDebounceTimer.current) clearTimeout(logoDebounceTimer.current);
    logoDebounceTimer.current = setTimeout(() => {
      setDebouncedLogoUrl(form.logoUrl);
    }, 500);
  }, [form.logoUrl]);

  // Dirty check
  const isDirty =
    form.siteName !== saved.siteName ||
    form.logoUrl !== saved.logoUrl ||
    form.announcement !== saved.announcement ||
    form.footerText !== saved.footerText;

  const showMsg = useCallback((text: string, ok: boolean) => {
    setMsg({ text, ok });
    if (msgTimer.current) clearTimeout(msgTimer.current);
    msgTimer.current = setTimeout(() => setMsg(null), 3000);
  }, []);

  useEffect(() => {
    fetch(endpoints.siteSettings)
      .then((r) => (r.ok ? r.json() : Promise.reject()))
      .then((d) => {
        const normalized = normalizeSiteSettings(d);
        const loaded: Settings = {
          siteName: normalized.site_name,
          logoUrl: normalized.logo_url,
          announcement: normalized.announcement,
          footerText: normalized.footer_text,
        };
        setForm(loaded);
        setSaved(loaded);
      })
      .catch(() => showMsg("加载设置失败", false))
      .finally(() => setLoading(false));
  }, [showMsg]);

  const update = (key: keyof Settings, value: string) => {
    setForm((prev) => ({ ...prev, [key]: value }));
    if (key === "logoUrl") setLogoError(false);
  };

  const handleSave = async () => {
    setSaving(true);
    setMsg(null);
    try {
      const res = await authFetch(adminConfig.SITE_SETTINGS_URL, {
        method: "PUT",
        body: JSON.stringify({
          site_name: form.siteName,
          logo_url: form.logoUrl,
          announcement: form.announcement,
          footer_text: form.footerText,
        }),
      });
      if (res.ok) {
        setSaved({ ...form });
        showMsg("保存成功", true);
      } else {
        showMsg("保存失败", false);
      }
    } catch {
      showMsg("网络错误", false);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12 text-muted-foreground">
        <Loader2 className="h-5 w-5 animate-spin mr-2" />
        加载设置中...
      </div>
    );
  }

  return (
    <div className="max-w-2xl space-y-6">
      {/* Unsaved banner */}
      {isDirty && (
        <div className="flex items-center gap-2 rounded-lg border border-amber-300 bg-amber-50 dark:border-amber-700 dark:bg-amber-950/40 px-4 py-2.5 text-sm text-amber-700 dark:text-amber-400">
          <AlertCircle className="h-4 w-4 flex-shrink-0" />
          有未保存的更改
        </div>
      )}

      {/* Card 1: Basic Info */}
      <Card className="rounded-lg border p-6 space-y-4">
        <CardHeader className="p-0">
          <div className="flex items-center gap-2">
            <ImageIcon className="h-5 w-5 text-primary" />
            <CardTitle className="text-base">基本信息</CardTitle>
          </div>
          <CardDescription>设置站点名称和 Logo</CardDescription>
        </CardHeader>
        <CardContent className="p-0 space-y-4">
          <div className="space-y-2">
            <Label htmlFor="siteName">站点名称</Label>
            <Input
              id="siteName"
              placeholder="输入站点名称"
              value={form.siteName}
              onChange={(e) => update("siteName", e.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="logoUrl">Logo URL</Label>
            <Input
              id="logoUrl"
              placeholder="https://example.com/logo.png"
              value={form.logoUrl}
              onChange={(e) => update("logoUrl", e.target.value)}
            />
            {debouncedLogoUrl && !logoError && (
              <div className="mt-2 rounded-md border bg-muted/30 p-3 flex items-center justify-center">
                <img
                  src={debouncedLogoUrl}
                  alt="Logo 预览"
                  className="max-h-20 object-contain"
                  onError={() => setLogoError(true)}
                />
              </div>
            )}
            {debouncedLogoUrl && logoError && (
              <p className="text-xs text-muted-foreground">无法加载图片，请检查 URL 是否正确</p>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Card 2: Announcement */}
      <Card className="rounded-lg border p-6 space-y-4">
        <CardHeader className="p-0">
          <div className="flex items-center gap-2">
            <Megaphone className="h-5 w-5 text-primary" />
            <CardTitle className="text-base">公告管理</CardTitle>
          </div>
          <CardDescription>设置站点公告内容，留空则不显示</CardDescription>
        </CardHeader>
        <CardContent className="p-0 space-y-4">
          <div className="space-y-2">
            <Label htmlFor="announcement">公告内容</Label>
            <Textarea
              id="announcement"
              placeholder="输入公告内容..."
              value={form.announcement}
              onChange={(e) => update("announcement", e.target.value)}
              rows={4}
            />
          </div>
        </CardContent>
      </Card>

      {/* Card 3: Appearance */}
      <Card className="rounded-lg border p-6 space-y-4">
        <CardHeader className="p-0">
          <div className="flex items-center gap-2">
            <Palette className="h-5 w-5 text-primary" />
            <CardTitle className="text-base">外观设置</CardTitle>
          </div>
          <CardDescription>自定义页面外观元素</CardDescription>
        </CardHeader>
        <CardContent className="p-0 space-y-4">
          <div className="space-y-2">
            <Label htmlFor="footerText">页脚文字</Label>
            <Input
              id="footerText"
              placeholder="例如: (c) 2026 智能交通监控系统"
              value={form.footerText}
              onChange={(e) => update("footerText", e.target.value)}
            />
          </div>
        </CardContent>
      </Card>

      {/* Save area */}
      <div className="flex items-center gap-3">
        <Button onClick={handleSave} disabled={saving || !isDirty}>
          {saving ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin mr-1.5" />
              保存中...
            </>
          ) : (
            <>
              <Save className="h-4 w-4 mr-1.5" />
              保存设置
            </>
          )}
        </Button>
        {msg && (
          <span className={msg.ok ? "text-green-600 text-sm" : "text-red-600 text-sm"}>
            {msg.text}
          </span>
        )}
      </div>
    </div>
  );
}
