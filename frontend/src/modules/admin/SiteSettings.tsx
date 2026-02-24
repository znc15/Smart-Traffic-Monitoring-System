import { useEffect, useState } from "react";
import { Button } from "@/ui/button";
import { Input } from "@/ui/input";
import { Textarea } from "@/ui/textarea";
import { Card, CardContent, CardHeader, CardTitle } from "@/ui/card";
import { authFetch, endpoints, adminConfig } from "@/config";

export default function SiteSettings() {
  const [siteName, setSiteName] = useState("");
  const [announcement, setAnnouncement] = useState("");
  const [msg, setMsg] = useState<{ text: string; ok: boolean } | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    fetch(endpoints.siteSettings)
      .then((r) => r.ok ? r.json() : Promise.reject())
      .then((d) => {
        setSiteName(d.siteName ?? "");
        setAnnouncement(d.announcement ?? "");
      })
      .catch(() => setMsg({ text: "加载设置失败", ok: false }));
  }, []);

  const handleSave = async () => {
    setSaving(true);
    setMsg(null);
    try {
      const res = await authFetch(adminConfig.SITE_SETTINGS_URL, {
        method: "PUT",
        body: JSON.stringify({ siteName, announcement }),
      });
      setMsg(res.ok ? { text: "保存成功", ok: true } : { text: "保存失败", ok: false });
    } catch {
      setMsg({ text: "网络错误", ok: false });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Card className="max-w-xl mt-4">
      <CardHeader><CardTitle>网站设置</CardTitle></CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-1">
          <label className="text-sm font-medium">站点名称</label>
          <Input value={siteName} onChange={(e) => setSiteName(e.target.value)} />
        </div>
        <div className="space-y-1">
          <label className="text-sm font-medium">公告</label>
          <Textarea value={announcement} onChange={(e) => setAnnouncement(e.target.value)} rows={3} />
        </div>
        {msg && <p className={msg.ok ? "text-green-600 text-sm" : "text-red-600 text-sm"}>{msg.text}</p>}
        <Button onClick={handleSave} disabled={saving}>{saving ? "保存中..." : "保存"}</Button>
      </CardContent>
    </Card>
  );
}
