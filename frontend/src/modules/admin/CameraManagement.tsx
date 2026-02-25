import { useEffect, useRef, useState, useCallback } from "react";
import { Button } from "@/ui/button";
import { Input } from "@/ui/input";
import { Label } from "@/ui/label";
import { Badge } from "@/ui/badge";
import { Switch } from "@/ui/switch";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/ui/dialog";
import { adminConfig, authFetch } from "@/config";
import {
  Camera,
  Eye,
  MapPin,
  Pencil,
  Plus,
  Trash2,
  Video,
  X,
} from "lucide-react";

type CameraItem = {
  id: number;
  name: string;
  location: string;
  stream_url: string | null;
  road_name: string | null;
  enabled: boolean;
};

type FormState = {
  name: string;
  location: string;
  streamUrl: string;
  roadName: string;
  enabled: boolean;
};

const emptyForm: FormState = {
  name: "",
  location: "",
  streamUrl: "",
  roadName: "",
  enabled: true,
};

export default function CameraManagement() {
  const [cameras, setCameras] = useState<CameraItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewCamera, setPreviewCamera] = useState<CameraItem | null>(null);
  const [videoError, setVideoError] = useState(false);
  const [editing, setEditing] = useState<CameraItem | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm);
  const videoRef = useRef<HTMLVideoElement>(null);

  const fetchCameras = async () => {
    try {
      const res = await authFetch(adminConfig.CAMERAS_URL);
      if (res.ok) setCameras(await res.json());
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCameras();
  }, []);

  // --- Dialog handlers ---
  const openAdd = () => {
    setEditing(null);
    setForm(emptyForm);
    setDialogOpen(true);
  };

  const openEdit = (c: CameraItem) => {
    setEditing(c);
    setForm({
      name: c.name,
      location: c.location,
      streamUrl: c.stream_url ?? "",
      roadName: c.road_name ?? "",
      enabled: c.enabled,
    });
    setDialogOpen(true);
  };

  const handleSave = async () => {
    const body = {
      name: form.name,
      location: form.location,
      stream_url: form.streamUrl || null,
      road_name: form.roadName || null,
      enabled: editing ? form.enabled : true,
    };
    if (editing) {
      await authFetch(`${adminConfig.CAMERAS_URL}/${editing.id}`, {
        method: "PUT",
        body: JSON.stringify(body),
      });
    } else {
      await authFetch(adminConfig.CAMERAS_URL, {
        method: "POST",
        body: JSON.stringify(body),
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

  const toggleEnabled = async (c: CameraItem) => {
    await authFetch(`${adminConfig.CAMERAS_URL}/${c.id}`, {
      method: "PUT",
      body: JSON.stringify({
        name: c.name,
        location: c.location,
        stream_url: c.stream_url,
        road_name: c.road_name,
        enabled: !c.enabled,
      }),
    });
    fetchCameras();
  };

  // --- Preview handlers ---
  const openPreview = (c: CameraItem) => {
    setPreviewCamera(c);
    setVideoError(false);
    setPreviewOpen(true);
  };

  const closePreview = useCallback(() => {
    // Clean up video connection on close
    if (videoRef.current) {
      videoRef.current.pause();
      videoRef.current.removeAttribute("src");
      videoRef.current.load();
    }
    setPreviewOpen(false);
    setPreviewCamera(null);
    setVideoError(false);
  }, []);

  const handleVideoError = () => {
    setVideoError(true);
  };

  if (loading)
    return <p className="py-4 text-muted-foreground">加载中...</p>;

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">摄像头列表</h3>
        <Button size="sm" onClick={openAdd}>
          <Plus className="h-4 w-4 mr-1" />
          添加
        </Button>
      </div>

      {/* Card Grid */}
      {cameras.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 text-muted-foreground">
          <Camera className="h-12 w-12 mb-3 opacity-30" />
          <p>暂无摄像头</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {cameras.map((c) => (
            <CameraCard
              key={c.id}
              camera={c}
              onEdit={openEdit}
              onDelete={handleDelete}
              onPreview={openPreview}
              onToggle={toggleEnabled}
            />
          ))}
        </div>
      )}

      {/* Add / Edit Dialog */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {editing ? "编辑摄像头" : "添加摄像头"}
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <div>
              <Label htmlFor="cam-name">
                名称 <span className="text-destructive">*</span>
              </Label>
              <Input
                id="cam-name"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="摄像头名称"
              />
            </div>
            <div>
              <Label htmlFor="cam-road">道路名称</Label>
              <Input
                id="cam-road"
                value={form.roadName}
                onChange={(e) =>
                  setForm({ ...form, roadName: e.target.value })
                }
                placeholder="所在道路名称"
              />
            </div>
            <div>
              <Label htmlFor="cam-loc">位置</Label>
              <Input
                id="cam-loc"
                value={form.location}
                onChange={(e) =>
                  setForm({ ...form, location: e.target.value })
                }
                placeholder="安装位置"
              />
            </div>
            <div>
              <Label htmlFor="cam-url">接入地址</Label>
              <Input
                id="cam-url"
                value={form.streamUrl}
                onChange={(e) =>
                  setForm({ ...form, streamUrl: e.target.value })
                }
                placeholder="http://192.168.1.100:8080"
              />
              <p className="text-xs text-muted-foreground mt-1">
                边缘节点 HTTP API 地址，留空则使用模拟数据
              </p>
            </div>
            {editing && (
              <div className="flex items-center justify-between">
                <Label htmlFor="cam-enabled">启用状态</Label>
                <Switch
                  id="cam-enabled"
                  checked={form.enabled}
                  onCheckedChange={(v) =>
                    setForm({ ...form, enabled: v })
                  }
                />
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogOpen(false)}>
              取消
            </Button>
            <Button onClick={handleSave} disabled={!form.name.trim()}>
              保存
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Video Preview Dialog */}
      <Dialog open={previewOpen} onOpenChange={(open) => !open && closePreview()}>
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Video className="h-4 w-4" />
              {previewCamera?.name} - 实时预览
            </DialogTitle>
          </DialogHeader>
          <div className="relative bg-black rounded-lg overflow-hidden aspect-video">
            {previewCamera?.stream_url && !videoError ? (
              <video
                ref={videoRef}
                src={`${previewCamera.stream_url}/api/video`}
                autoPlay
                muted
                playsInline
                onError={handleVideoError}
                className="w-full h-full object-contain"
              />
            ) : previewCamera?.stream_url && videoError ? (
              <img
                src={`${previewCamera.stream_url}/api/stream`}
                alt={`${previewCamera.name} 视频流`}
                className="w-full h-full object-contain"
              />
            ) : (
              <div className="flex items-center justify-center h-full text-white/50">
                <Camera className="h-16 w-16" />
              </div>
            )}
          </div>
          <div className="flex items-center justify-between text-sm text-muted-foreground">
            <span>
              {videoError
                ? "已降级为 MJPEG 流"
                : "WebM 视频流"}
            </span>
            <span className="font-mono text-xs">
              {previewCamera?.stream_url}
            </span>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}

// ─── Camera Card Component ───────────────────────────────────────────

function CameraCard({
  camera,
  onEdit,
  onDelete,
  onPreview,
  onToggle,
}: {
  camera: CameraItem;
  onEdit: (c: CameraItem) => void;
  onDelete: (id: number) => void;
  onPreview: (c: CameraItem) => void;
  onToggle: (c: CameraItem) => void;
}) {
  return (
    <div className="group relative rounded-lg border border-border/40 bg-card shadow-sm hover:shadow-md transition-all duration-200 overflow-hidden">
      {/* Thumbnail area */}
      <div className="relative h-36 bg-muted/30 flex items-center justify-center overflow-hidden">
        {camera.stream_url ? (
          <img
            src={`${camera.stream_url}/api/stream`}
            alt={camera.name}
            className="w-full h-full object-cover"
            onError={(e) => {
              // On thumbnail load error, show fallback icon
              const target = e.currentTarget;
              target.style.display = "none";
              target.nextElementSibling?.classList.remove("hidden");
            }}
          />
        ) : null}
        <div
          className={`flex flex-col items-center justify-center text-muted-foreground/40 ${camera.stream_url ? "hidden" : ""}`}
        >
          <Camera className="h-10 w-10" />
          <span className="text-xs mt-1">无视频源</span>
        </div>

        {/* Hover overlay with action buttons */}
        <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity duration-200 flex items-center justify-center gap-2">
          <Button
            variant="secondary"
            size="icon"
            className="h-8 w-8 rounded-full"
            onClick={() => onPreview(camera)}
            disabled={!camera.stream_url}
            title="预览"
          >
            <Eye className="h-4 w-4" />
          </Button>
          <Button
            variant="secondary"
            size="icon"
            className="h-8 w-8 rounded-full"
            onClick={() => onEdit(camera)}
            title="编辑"
          >
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          <Button
            variant="destructive"
            size="icon"
            className="h-8 w-8 rounded-full"
            onClick={() => onDelete(camera.id)}
            title="删除"
          >
            <Trash2 className="h-3.5 w-3.5" />
          </Button>
        </div>
      </div>

      {/* Card body */}
      <div className="p-3 space-y-2">
        <div className="flex items-start justify-between gap-2">
          <h4 className="font-medium text-sm truncate">{camera.name}</h4>
          <Badge
            variant="outline"
            className={`text-[10px] shrink-0 cursor-pointer ${
              camera.enabled
                ? "border-emerald-500/30 bg-emerald-50 text-emerald-700 dark:bg-emerald-900/20 dark:text-emerald-400"
                : "border-gray-300/30 bg-gray-50 text-gray-500 dark:bg-gray-800/30 dark:text-gray-400"
            }`}
            onClick={() => onToggle(camera)}
          >
            {camera.enabled ? "启用" : "禁用"}
          </Badge>
        </div>

        {camera.road_name && (
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <MapPin className="h-3 w-3 shrink-0" />
            <span className="truncate">{camera.road_name}</span>
          </div>
        )}

        {camera.location && (
          <p className="text-xs text-muted-foreground/70 truncate">
            {camera.location}
          </p>
        )}
      </div>
    </div>
  );
}
