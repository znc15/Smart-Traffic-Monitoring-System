import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Input } from "@/ui/input";
import { Button } from "@/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/ui/card";
import {
  Eye,
  EyeOff,
  User,
  Lock,
  Mail,
  Phone,
  Car,
  CheckCircle,
} from "lucide-react";
import { authConfig } from "@/config";
import { translateBackendDetailToZh } from "@/utils/translate";

function Register({ onRegisterSuccess }: { onRegisterSuccess?: () => void }) {
  const navigate = useNavigate();

  // Form states
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // UI states
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();

    if (password !== confirmPassword) {
      setError("两次输入的密码不一致！");
      return;
    }

    if (password.length < 8) {
      setError("密码长度至少 8 位！");
      return;
    }

    setLoading(true);
    setError("");
    setSuccess(false);

    try {
      const res = await fetch(authConfig.REGISTER_URL, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          username,
          email,
          phone_number: phone,
          password,
        }),
      });

      const data = await res.json();

      if (res.ok) {
        setSuccess(true);
        onRegisterSuccess?.();
        // Chuyển hướng đến trang đăng nhập sau 2 giây
        setTimeout(() => {
          navigate("/login");
        }, 2000);
      } else {
        const detail =
          typeof data?.detail === "string"
            ? translateBackendDetailToZh(data.detail)
            : undefined;
        setError(detail || "注册失败！");
      }
    } catch {
      setError("发生错误，请稍后重试。");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-background via-blue-50/40 to-indigo-100/30 dark:from-background dark:via-blue-950/20 dark:to-indigo-950/10 p-4">
      <Card className="w-full max-w-lg shadow-2xl shadow-primary/5 border-border/50 bg-card/95 backdrop-blur-xl">
        <CardHeader className="text-center pb-8">
          <div className="mx-auto w-16 h-16 bg-gradient-to-br from-indigo-600 via-blue-500 to-cyan-500 rounded-2xl flex items-center justify-center mb-4 shadow-lg shadow-blue-500/25">
            <Car className="w-8 h-8 text-white" />
          </div>
          <CardTitle className="text-3xl font-bold text-primary">
            注册账号
          </CardTitle>
          <p className="text-muted-foreground mt-2">
            创建新账号以使用系统
          </p>
        </CardHeader>
        <CardContent className="px-8 pb-8">
          <form onSubmit={handleRegister} className="space-y-6">
            <div className="space-y-4">
              {/* Username field */}
              <div className="relative">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground w-4.5 h-4.5" />
                <Input
                  placeholder="用户名"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                  className="pl-10 h-12 bg-secondary/30 border-border/50 focus:border-primary focus:ring-primary/20 transition-colors"
                />
              </div>

              {/* Email field */}
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

              {/* Phone field */}
              <div className="relative">
                <Phone className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground w-4.5 h-4.5" />
                <Input
                  type="tel"
                  placeholder="手机号"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  required
                  className="pl-10 h-12 bg-secondary/30 border-border/50 focus:border-primary focus:ring-primary/20 transition-colors"
                />
              </div>

              {/* Password field */}
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground w-4.5 h-4.5" />
                <Input
                  type={showPassword ? "text" : "password"}
                  placeholder="密码"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  className="pl-10 pr-10 h-12 bg-secondary/30 border-border/50 focus:border-primary focus:ring-primary/20 transition-colors"
                  minLength={8}
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

              {/* Confirm Password field */}
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground w-4.5 h-4.5" />
                <Input
                  type={showConfirmPassword ? "text" : "password"}
                  placeholder="再次输入密码"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                  className="pl-10 pr-10 h-12 bg-secondary/30 border-border/50 focus:border-primary focus:ring-primary/20 transition-colors"
                  minLength={8}
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                >
                  {showConfirmPassword ? (
                    <EyeOff className="w-5 h-5" />
                  ) : (
                    <Eye className="w-5 h-5" />
                  )}
                </button>
              </div>
            </div>

            {/* Status Messages */}
            {success && (
              <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 text-green-600 dark:text-green-400 px-4 py-3 rounded-lg text-sm flex items-center">
                <CheckCircle className="w-5 h-5 mr-2" />
                注册成功！正在跳转...
              </div>
            )}
            {error && (
              <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400 px-4 py-3 rounded-lg text-sm flex items-center">
                <div className="w-4 h-4 bg-red-500 rounded-full mr-2"></div>
                {error}
              </div>
            )}

            {/* Submit Button */}
            <Button
              type="submit"
              disabled={loading}
              className="w-full h-12 rounded-xl font-semibold shadow-lg shadow-primary/20 transition-all duration-200 hover:scale-[1.02] hover:shadow-xl hover:shadow-primary/25 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? (
                <div className="flex items-center justify-center">
                  <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin mr-2"></div>
                  正在注册...
                </div>
              ) : (
                "注册"
              )}
            </Button>

            {/* Login Link */}
            <p className="text-center text-muted-foreground">
              已有账号？{" "}
              <button
                type="button"
                onClick={() => navigate("/login")}
                className="text-primary hover:underline font-medium transition-colors"
              >
                立即登录
              </button>
            </p>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

export default Register;
