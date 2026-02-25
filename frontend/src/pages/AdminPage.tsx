import { useEffect, useMemo, useState } from "react";
import { Button } from "@/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/ui/card";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/ui/tooltip";
import { authConfig } from "@/config";
import { getApiUrl } from "@/config/settings";
import { useNavigate } from "react-router-dom";
import {
  Camera,
  Activity,
  Users,
  Settings,
  ChevronLeft,
  ChevronRight,
  Menu,
  X,
  UserCircle,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { motion, AnimatePresence } from "framer-motion";
import CameraManagement from "@/modules/admin/CameraManagement";
import SystemMonitor from "@/modules/admin/SystemMonitor";
import UserManagement from "@/modules/admin/UserManagement";
import SiteSettings from "@/modules/admin/SiteSettings";

// 侧边栏菜单配置
const menuItems = [
  { key: "cameras", label: "摄像头管理", icon: Camera },
  { key: "monitor", label: "系统监控", icon: Activity },
  { key: "users", label: "用户管理", icon: Users },
  { key: "settings", label: "网站设置", icon: Settings },
] as const;

type MenuKey = (typeof menuItems)[number]["key"];

// 页面切换动画配置
const pageVariants = {
  initial: { opacity: 0, x: 20 },
  animate: { opacity: 1, x: 0 },
  exit: { opacity: 0, x: -20 },
};

const pageTransition = {
  type: "tween" as const,
  ease: "easeInOut",
  duration: 0.2,
};

export default function AdminPage() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isAdmin, setIsAdmin] = useState<boolean | null>(null);
  const [username, setUsername] = useState("管理员");
  const [activeTab, setActiveTab] = useState<MenuKey>("cameras");
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
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
          if (me?.username) setUsername(me.username);
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

  // 当前选中菜单项
  const activeItem = menuItems.find((m) => m.key === activeTab);

  // 渲染当前激活的内容面板
  const renderContent = () => {
    switch (activeTab) {
      case "cameras": return <CameraManagement />;
      case "monitor": return <SystemMonitor />;
      case "users":   return <UserManagement />;
      case "settings": return <SiteSettings />;
    }
  };

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
    <div className="flex h-full bg-background">
      {/* 移动端遮罩 */}
      <AnimatePresence>
        {mobileOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-40 bg-black/40 backdrop-blur-sm md:hidden"
            onClick={() => setMobileOpen(false)}
          />
        )}
      </AnimatePresence>

      {/* 侧边栏 */}
      <aside
        className={cn(
          "flex flex-col border-r border-border/60 bg-background/80 backdrop-blur-xl transition-all duration-300 ease-in-out z-50",
          // 桌面端：根据折叠状态切换宽度
          collapsed ? "md:w-16" : "md:w-56",
          // 移动端：固定定位，滑入滑出
          "fixed inset-y-0 left-0 md:relative",
          mobileOpen ? "w-56 translate-x-0" : "w-56 -translate-x-full md:translate-x-0"
        )}
      >
        {/* 侧边栏头部 */}
        <div className={cn(
          "flex items-center border-b border-border/60 h-14 px-3 flex-shrink-0",
          collapsed ? "justify-center" : "justify-between"
        )}>
          {!collapsed && (
            <span className="text-sm font-bold text-primary truncate">后台管理</span>
          )}
          {/* 桌面端折叠按钮 */}
          <Button
            variant="ghost"
            size="icon"
            className="hidden md:flex h-8 w-8 rounded-lg"
            onClick={() => setCollapsed(!collapsed)}
          >
            {collapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
          </Button>
          {/* 移动端关闭按钮 */}
          <Button
            variant="ghost"
            size="icon"
            className="md:hidden h-8 w-8 rounded-lg"
            onClick={() => setMobileOpen(false)}
          >
            <X className="h-4 w-4" />
          </Button>
        </div>

        {/* 菜单列表 */}
        <nav className="flex-1 py-2 px-2 space-y-1 overflow-y-auto">
          {menuItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.key;
            const btn = (
              <button
                key={item.key}
                onClick={() => { setActiveTab(item.key); setMobileOpen(false); }}
                className={cn(
                  "flex items-center w-full rounded-lg text-sm font-medium transition-all duration-200",
                  collapsed ? "justify-center px-2 py-2.5" : "gap-3 px-3 py-2.5",
                  isActive
                    ? "bg-primary text-primary-foreground shadow-md shadow-primary/25"
                    : "text-muted-foreground hover:bg-accent hover:text-accent-foreground"
                )}
              >
                <Icon className="h-4.5 w-4.5 flex-shrink-0" />
                {!collapsed && <span className="truncate">{item.label}</span>}
              </button>
            );
            // 折叠态用 Tooltip 显示菜单名
            if (collapsed) {
              return (
                <Tooltip key={item.key}>
                  <TooltipTrigger asChild>{btn}</TooltipTrigger>
                  <TooltipContent side="right">{item.label}</TooltipContent>
                </Tooltip>
              );
            }
            return btn;
          })}
        </nav>
      </aside>

      {/* 右侧主区域 */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* 顶部栏 */}
        <header className="flex items-center justify-between h-14 px-4 border-b border-border/60 bg-background/60 backdrop-blur-sm flex-shrink-0">
          <div className="flex items-center gap-3">
            {/* 移动端汉堡菜单 */}
            <Button
              variant="ghost"
              size="icon"
              className="md:hidden h-8 w-8 rounded-lg"
              onClick={() => setMobileOpen(true)}
            >
              <Menu className="h-5 w-5" />
            </Button>
            {/* 面包屑 */}
            <nav className="flex items-center gap-1.5 text-sm">
              <span className="text-muted-foreground">后台管理</span>
              <span className="text-muted-foreground">/</span>
              <span className="font-medium text-foreground">{activeItem?.label}</span>
            </nav>
          </div>
          {/* 管理员信息 */}
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <UserCircle className="h-4 w-4" />
            <span className="hidden sm:inline">{username}</span>
          </div>
        </header>

        {/* 内容区 */}
        <main className="flex-1 overflow-auto p-4 sm:p-6">
          <AnimatePresence mode="wait">
            <motion.div
              key={activeTab}
              variants={pageVariants}
              initial="initial"
              animate="animate"
              exit="exit"
              transition={pageTransition}
            >
              {renderContent()}
            </motion.div>
          </AnimatePresence>
        </main>
      </div>
    </div>
  );
}
