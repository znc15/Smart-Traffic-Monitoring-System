import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Input } from "@/ui/input";
import { Button } from "@/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/ui/card";
import { Eye, EyeOff, Mail, Lock, Car } from "lucide-react";
import { authConfig } from "@/config";
import { translateBackendDetailToZh } from "@/utils/translate";

function Login({ onLoginSuccess }: { onLoginSuccess?: () => void }) {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      // OAuth2 要求 application/x-www-form-urlencoded
      const formData = new URLSearchParams();
      formData.append("username", email); // OAuth2 使用 "username" 字段兼容邮箱/用户名
      formData.append("password", password);

      const res = await fetch(authConfig.LOGIN_URL, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: formData,
      });
      const data = await res.json();
      if (res.ok && data.access_token) {
        localStorage.setItem(authConfig.TOKEN_KEY, data.access_token);
        onLoginSuccess?.();
        navigate("/home");
      } else {
        const detail =
          typeof data?.detail === "string"
            ? translateBackendDetailToZh(data.detail)
            : undefined;
        setError(detail || "登录失败！");
      }
    } catch {
      setError("发生错误，请稍后重试。");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-background via-blue-50/40 to-indigo-100/30 dark:from-background dark:via-blue-950/20 dark:to-indigo-950/10 p-4">
      <Card className="w-full max-w-md shadow-2xl shadow-primary/5 border-border/50 bg-card/95 backdrop-blur-xl">
        <CardHeader className="text-center pb-8">
          <div className="mx-auto w-16 h-16 bg-gradient-to-br from-indigo-600 via-blue-500 to-cyan-500 rounded-2xl flex items-center justify-center mb-4 shadow-lg shadow-blue-500/25">
            <Car className="w-8 h-8 text-white" />
          </div>
          <CardTitle className="text-3xl font-bold text-primary">
            智能交通系统
          </CardTitle>
          <p className="text-muted-foreground mt-2">
            登录以继续
          </p>
        </CardHeader>
        <CardContent className="px-8 pb-8">
          <form onSubmit={handleLogin} className="space-y-6">
            <div className="space-y-4">
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground w-4.5 h-4.5" />
                <Input
                  type="email"
                  placeholder="邮箱"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  className="pl-10 h-12 bg-secondary/30 border-border/50 focus:border-primary focus:ring-primary/20 transition-colors"
                />
              </div>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground w-4.5 h-4.5" />
                <Input
                  type={showPassword ? "text" : "password"}
                  placeholder="密码"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  className="pl-10 pr-10 h-12 bg-secondary/30 border-border/50 focus:border-primary focus:ring-primary/20 transition-colors"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                >
                  {showPassword ? (
                    <EyeOff className="w-5 h-5" />
                  ) : (
                    <Eye className="w-5 h-5" />
                  )}
                </button>
              </div>
            </div>
            {error && (
              <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400 px-4 py-3 rounded-lg text-sm">
                {error}
              </div>
            )}
            <Button
              type="submit"
              disabled={loading}
              className="w-full h-12 rounded-xl font-semibold shadow-lg shadow-primary/20 transition-all duration-200 hover:scale-[1.02] hover:shadow-xl hover:shadow-primary/25 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? (
                <div className="flex items-center justify-center">
                  <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin mr-2"></div>
                  正在登录...
                </div>
              ) : (
                "登录"
              )}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

export default Login;
