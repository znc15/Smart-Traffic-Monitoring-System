import { useState, useEffect } from "react";
import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
  useNavigate,
  NavLink,
} from "react-router-dom";
import { ThemeProvider, useTheme } from "next-themes";
import { Toaster } from "sonner";
import { Button } from "@/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/ui/dropdown-menu";
import {
  Car,
  LogOut,
  Settings,
  UserCircle,
  Sun,
  Moon,
  Home,
  BarChart3,
  Bot,
  ShieldCheck,
} from "lucide-react";
import { clearAllChatData } from "@/utils/chatStorage";
import { cn } from "@/lib/utils";
import LoginPage from "./pages/LoginPage";
import TrafficDashboard from "@/modules/features/traffic/components/TrafficDashboard";
import AnalyticsPage from "./pages/AnalyticsPage";
import ChatPage from "./pages/ChatPage";
import ProfilePage from "./pages/ProfilePage";
import ProtectedRoute from "@/modules/features/auth/guards/ProtectedRoute";
import AdminPage from "@/pages/AdminPage";
import { authConfig } from "@/config";
import "./App.css";
import { TrafficProvider } from "@/hooks/useTrafficStore";
export default function App() {
  return (
    <ThemeProvider attribute="class" defaultTheme="light" enableSystem>
      <BrowserRouter>
        <AppContent />
      </BrowserRouter>
    </ThemeProvider>
  );
}

function AppContent() {
  const [showRegister, setShowRegister] = useState(false);
  const [authed, setAuthed] = useState(() => {
    const token = localStorage.getItem("access_token");
    if (token && token.length < 10) {
      localStorage.removeItem("access_token");
      return false;
    }
    return !!token;
  });
  const [isAdmin, setIsAdmin] = useState<boolean>(false);
  const { theme, setTheme } = useTheme();
  const navigate = useNavigate();

  const handleLoginSuccess = () => setAuthed(true);
  const handleRegisterSuccess = () => setShowRegister(false);
  const handleLogout = () => {
    // Clear authentication
    localStorage.removeItem("access_token");
    // Clear chat data when user logs out
    clearAllChatData();
    setAuthed(false);
    setIsAdmin(false);
    navigate("/login", { replace: true });
  };

  // Fetch current user to determine admin role
  useEffect(() => {
    const fetchMe = async () => {
      try {
        if (!authed) {
          setIsAdmin(false);
          return;
        }
        const token = localStorage.getItem("access_token");
        if (!token) {
          setIsAdmin(false);
          return;
        }
        const res = await fetch(`${authConfig.ME_URL}`, {
          headers: { Authorization: `Bearer ${token}` },
          credentials: "include",
        });
        if (!res.ok) {
          setIsAdmin(false);
          return;
        }
        const data = await res.json();
        setIsAdmin(data?.role_id === 0);
      } catch {
        setIsAdmin(false);
      }
    };
    fetchMe();
  }, [authed]);

  const navItemClass = ({ isActive }: { isActive: boolean }) =>
    cn(
      "flex items-center gap-1.5 sm:gap-2 px-3 sm:px-5 py-2 rounded-xl font-semibold text-sm transition-all duration-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring",
      isActive
        ? "bg-primary text-primary-foreground shadow-md shadow-primary/25"
        : "text-muted-foreground hover:bg-accent hover:text-accent-foreground hover:shadow-sm active:scale-95"
    );

  return (
    <div className="min-h-screen bg-gradient-to-br from-background via-blue-50/40 to-indigo-100/30 dark:from-background dark:via-blue-950/20 dark:to-indigo-950/10">
      {/* Banner */}
      <div className="w-full flex flex-wrap items-center justify-between px-3 sm:px-6 py-2.5 bg-background/80 dark:bg-background/70 shadow-sm border-b border-border/60 backdrop-blur-xl backdrop-saturate-150 sticky top-0 z-50 transition-colors">
        <div className="flex items-center min-w-0 gap-3">
          <a
            href="/home"
            className="flex items-center flex-shrink-0"
            title="首页"
          >
            <div className="relative w-12 h-12">
              {/* Animated background gradient */}
              <div className="absolute inset-0 rounded-xl bg-gradient-to-r from-indigo-600 via-blue-500 to-cyan-500 animate-gradient-x shadow-lg shadow-blue-500/20"></div>

              {/* Icon container with glassmorphism effect */}
              <div className="absolute inset-0.5 rounded-lg bg-white/10 backdrop-blur-sm flex items-center justify-center">
                <Car
                  className="w-7 h-7 text-white/90 drop-shadow-lg"
                  strokeWidth={2}
                />
                <div className="absolute -right-1 -top-1">
                  <div className="relative w-4 h-4 bg-green-500 rounded-full animate-pulse">
                    <div className="absolute inset-0 rounded-full bg-green-500 animate-ping opacity-75"></div>
                  </div>
                </div>
              </div>
            </div>
          </a>
          <div className="flex flex-col justify-center min-w-0">
            <h1 className="text-base sm:text-xl lg:text-2xl font-bold text-primary truncate leading-tight">
              智能交通监控系统
            </h1>
            <p className="text-muted-foreground text-xs sm:text-sm hidden sm:block leading-tight">
              实时监控与分析
            </p>
          </div>
        </div>
        <div className="flex-1 flex flex-col items-center justify-center max-w-md mx-auto">
          {/* Centered Navigation Tabs */}
          <nav className="flex items-center gap-1 sm:gap-3">
            <NavLink
              to="/home"
              className={navItemClass}
            >
              <Home className="h-5 w-5 sm:h-6 sm:w-6" />
              <span className="hidden sm:inline">首页</span>
            </NavLink>
            <NavLink
              to="/analys"
              className={navItemClass}
            >
              <BarChart3 className="h-5 w-5 sm:h-6 sm:w-6" />
              <span className="hidden sm:inline">分析</span>
            </NavLink>
            <NavLink
              to="/chat"
              className={navItemClass}
            >
              <Bot className="h-5 w-5 sm:h-6 sm:w-6" />
              <span className="hidden sm:inline">AI 助手</span>
            </NavLink>
          </nav>
        </div>
        <div className="flex items-center space-x-2 sm:space-x-3 relative flex-shrink-0">
          {authed && (
            <>
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="outline"
                    className="flex items-center gap-2 px-3 py-2 rounded-xl bg-secondary/50 hover:bg-secondary border-border/50 shadow-sm transition-all duration-200 hover:shadow-md active:scale-95"
                  >
                    <UserCircle className="h-5 w-5 sm:h-6 sm:w-6 text-primary" />
                    <span className="font-semibold text-foreground hidden sm:inline text-sm">
                      账号
                    </span>
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="w-44">
                  {isAdmin && (
                    <DropdownMenuItem onSelect={() => navigate("/admin")}
                      className="cursor-pointer"
                    >
                      <ShieldCheck className="h-4 w-4 mr-2 text-primary" />
                      管理员面板
                    </DropdownMenuItem>
                  )}
                  <DropdownMenuItem onSelect={() => navigate("/profile")}
                    className="cursor-pointer"
                  >
                    <Settings className="h-4 w-4 mr-2 text-primary" />
                    账户设置
                  </DropdownMenuItem>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem
                    variant="destructive"
                    onSelect={() => handleLogout()}
                    className="cursor-pointer"
                  >
                    <LogOut className="h-4 w-4 mr-2" />
                    退出登录
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </>
          )}
          {/* Theme toggle button */}
          <Button
            variant="outline"
            size="icon"
            onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
            className="h-9 w-9 rounded-xl bg-secondary/50 border-border/50 hover:bg-secondary shadow-sm transition-all duration-200 hover:shadow-md active:scale-95"
            title={theme === "dark" ? "切换到浅色模式" : "切换到深色模式"}
          >
            {theme === "dark" ? (
              <Sun className="h-4 w-4 text-amber-400" />
            ) : (
              <Moon className="h-4 w-4 text-primary" />
            )}
          </Button>
        </div>
      </div>
      {/* Main Content */}
      <TrafficProvider>
        <Routes>
          <Route
            path="/login"
            element={
              <LoginPage
                onLoginSuccess={handleLoginSuccess}
                onRegisterSuccess={handleRegisterSuccess}
                showRegister={showRegister}
                setShowRegister={setShowRegister}
              />
            }
          />
          <Route element={<ProtectedRoute />}>
            <Route path="/home" element={<TrafficDashboard />} />
            <Route path="/analys" element={<AnalyticsPage />} />
            <Route path="/chat" element={<ChatPage />} />
            <Route path="/profile" element={<ProfilePage />} />
            <Route path="/admin" element={<AdminPage />} />
          </Route>
          <Route
            path="*"
            element={<Navigate to={authed ? "/home" : "/login"} replace />}
          />
        </Routes>
      </TrafficProvider>
      <Toaster position="top-right" richColors />
    </div>
  );
}
