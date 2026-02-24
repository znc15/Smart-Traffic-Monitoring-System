import { useEffect, useMemo, useState } from "react";
import { Button } from "@/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/ui/tabs";
import { authConfig } from "@/config";
import { getApiUrl } from "@/config/settings";
import { useNavigate } from "react-router-dom";
import CameraManagement from "@/modules/admin/CameraManagement";
import SystemMonitor from "@/modules/admin/SystemMonitor";
import UserManagement from "@/modules/admin/UserManagement";
import SiteSettings from "@/modules/admin/SiteSettings";

export default function AdminPage() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isAdmin, setIsAdmin] = useState<boolean | null>(null);
  const navigate = useNavigate();

  const token = useMemo(
    () => (typeof window !== "undefined" ? localStorage.getItem(authConfig.TOKEN_KEY) : null),
    []
  );

  useEffect(() => {
    let cancelled = false;
    const checkRole = async () => {
      try {
        if (!token) { setIsAdmin(false); setError("未登录"); setLoading(false); return; }
        const res = await fetch(getApiUrl("/auth/me"), {
          headers: { Authorization: `Bearer ${token}` },
          credentials: "include",
        });
        if (!res.ok) {
          setIsAdmin(false);
          setError(res.status === 401 ? "无访问权限" : "无法验证用户");
          setLoading(false);
          return;
        }
        const me = await res.json();
        if (!cancelled) {
          const admin = me?.role_id === 0;
          setIsAdmin(admin);
          if (!admin) setError("你无权访问此页面");
        }
      } catch {
        setIsAdmin(false);
        setError("无法连接服务器");
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    checkRole();
    return () => { cancelled = true; };
  }, [token]);

  if (loading) return <div className="p-6"><p>加载中...</p></div>;

  if (!isAdmin) {
    return (
      <div className="p-6">
        <Card className="max-w-xl">
          <CardHeader><CardTitle>访问被拒绝</CardTitle></CardHeader>
          <CardContent>
            <p className="mb-4">{error || "你无权访问管理员页面。"}</p>
            <Button onClick={() => navigate("/home")}>返回首页</Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background p-4 sm:p-6 space-y-6">
      <h2 className="text-2xl font-bold">后台管理</h2>
      <Tabs defaultValue="cameras">
        <TabsList>
          <TabsTrigger value="cameras">摄像头管理</TabsTrigger>
          <TabsTrigger value="monitor">系统监控</TabsTrigger>
          <TabsTrigger value="users">用户管理</TabsTrigger>
          <TabsTrigger value="settings">网站设置</TabsTrigger>
        </TabsList>
        <TabsContent value="cameras"><CameraManagement /></TabsContent>
        <TabsContent value="monitor"><SystemMonitor /></TabsContent>
        <TabsContent value="users"><UserManagement /></TabsContent>
        <TabsContent value="settings"><SiteSettings /></TabsContent>
      </Tabs>
    </div>
  );
}
