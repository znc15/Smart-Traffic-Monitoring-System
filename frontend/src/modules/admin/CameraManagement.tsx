import { useEffect, useState } from "react";
import { Button } from "@/ui/button";
import { Input } from "@/ui/input";
import { Label } from "@/ui/label";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/ui/dialog";
import { adminConfig, authFetch } from "@/config";
import { Pencil, Plus, Trash2 } from "lucide-react";

type Camera = {
  id: number;
  name: string;
  location: string;
  stream_url: string | null;
  enabled: boolean;
};

export default function CameraManagement() {
  const [cameras, setCameras] = useState<Camera[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Camera | null>(null);
  const [form, setForm] = useState({ name: "", location: "", streamUrl: "" });

  const fetchCameras = async () => {
    try {
      const res = await authFetch(adminConfig.CAMERAS_URL);
      if (res.ok) setCameras(await res.json());
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchCameras(); }, []);

  const openAdd = () => {
    setEditing(null);
    setForm({ name: "", location: "", streamUrl: "" });
    setDialogOpen(true);
  };

  const openEdit = (c: Camera) => {
    setEditing(c);
    setForm({ name: c.name, location: c.location, streamUrl: c.stream_url ?? "" });
    setDialogOpen(true);
  };

  const handleSave = async () => {
    if (editing) {
      await authFetch(`${adminConfig.CAMERAS_URL}/${editing.id}`, {
        method: "PUT",
        body: JSON.stringify({ name: form.name, location: form.location, stream_url: form.streamUrl || null, enabled: editing.enabled }),
      });
    } else {
      await authFetch(adminConfig.CAMERAS_URL, {
        method: "POST",
        body: JSON.stringify({ name: form.name, location: form.location, stream_url: form.streamUrl || null }),
      });
    }
    setDialogOpen(false);
    fetchCameras();
  };

  const handleDelete = async (id: number) => {
    if (!confirm("确定删除该摄像头？")) return;
    await authFetch(`${adminConfig.CAMERAS_URL}/${id}`, { method: "DELETE" });
    fetchCameras();
  };

  const toggleEnabled = async (c: Camera) => {
    await authFetch(`${adminConfig.CAMERAS_URL}/${c.id}`, {
      method: "PUT",
      body: JSON.stringify({ name: c.name, location: c.location, stream_url: c.stream_url, enabled: !c.enabled }),
    });
    fetchCameras();
  };

  if (loading) return <p className="py-4 text-muted-foreground">加载中...</p>;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">摄像头列表</h3>
        <Button size="sm" onClick={openAdd}>
          <Plus className="h-4 w-4 mr-1" />添加
        </Button>
      </div>

      <div className="bg-card border border-border/40 rounded-lg overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border/40 bg-muted/50">
              <th className="text-left px-4 py-2.5 font-medium">名称</th>
              <th className="text-left px-4 py-2.5 font-medium">位置</th>
              <th className="text-left px-4 py-2.5 font-medium">接入地址</th>
              <th className="text-left px-4 py-2.5 font-medium">状态</th>
              <th className="text-right px-4 py-2.5 font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            {cameras.length === 0 ? (
              <tr><td colSpan={5} className="px-4 py-6 text-center text-muted-foreground">暂无摄像头</td></tr>
            ) : cameras.map((c) => (
              <tr key={c.id} className="border-b border-border/20 last:border-0">
                <td className="px-4 py-2.5">{c.name}</td>
                <td className="px-4 py-2.5 text-muted-foreground">{c.location}</td>
                <td className="px-4 py-2.5 text-muted-foreground text-xs font-mono">{c.stream_url || <span className="text-muted-foreground/50 italic">未配置</span>}</td>
                <td className="px-4 py-2.5">
                  <button
                    onClick={() => toggleEnabled(c)}
                    className={`text-xs px-2 py-0.5 rounded-full ${c.enabled ? "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400" : "bg-gray-100 text-gray-500 dark:bg-gray-800 dark:text-gray-400"}`}
                  >
                    {c.enabled ? "启用" : "禁用"}
                  </button>
                </td>
                <td className="px-4 py-2.5 text-right space-x-1">
                  <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => openEdit(c)}>
                    <Pencil className="h-3.5 w-3.5" />
                  </Button>
                  <Button variant="ghost" size="icon" className="h-7 w-7 text-destructive" onClick={() => handleDelete(c.id)}>
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editing ? "编辑摄像头" : "添加摄像头"}</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <div>
              <Label htmlFor="cam-name">名称</Label>
              <Input id="cam-name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="摄像头名称" />
            </div>
            <div>
              <Label htmlFor="cam-loc">位置</Label>
              <Input id="cam-loc" value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} placeholder="安装位置" />
            </div>
            <div>
              <Label htmlFor="cam-url">接入地址</Label>
              <Input id="cam-url" value={form.streamUrl} onChange={(e) => setForm({ ...form, streamUrl: e.target.value })} placeholder="http://192.168.1.100:8080" />
              <p className="text-xs text-muted-foreground mt-1">摄像头 HTTP API 地址，留空则使用模拟数据</p>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogOpen(false)}>取消</Button>
            <Button onClick={handleSave} disabled={!form.name.trim()}>保存</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
